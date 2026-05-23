package com.guang.house.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guang.house.entity.House;
import com.guang.house.entity.SyncFailedLog;
import com.guang.house.service.HouseService;
import com.guang.house.service.SyncFailedLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncConsistencyJob {

    private final SyncFailedLogService syncFailedLogService;
    private final HouseService houseService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sync.to-ai.enabled:true}")
    private boolean enabled;

    @Value("${sync.to-ai.url:http://localhost:9090}") // AI 助手默认的端口在 application.yml 里是 9090
    private String aiServerUrl;

    /**
     * 定时任务 1：重试补偿表中的失败记录
     * 每 10 分钟运行一次
     */
    @Scheduled(fixedDelay = 600000)
    public void retryFailedLogs() {
        if (!enabled) return;
        log.info("开始执行同步失败日志补偿重试任务...");
        
        List<SyncFailedLog> pendingLogs = syncFailedLogService.list(
                new LambdaQueryWrapper<SyncFailedLog>()
                        .eq(SyncFailedLog::getStatus, "PENDING")
                        .lt(SyncFailedLog::getRetryCount, 3)
                        .le(SyncFailedLog::getNextRetryTime, LocalDateTime.now())
        );

        if (pendingLogs.isEmpty()) {
            log.info("暂无需要补偿重试的同步日志");
            return;
        }

        log.info("发现 {} 条待重试的同步失败记录", pendingLogs.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (SyncFailedLog syncLog : pendingLogs) {
            String url = aiServerUrl + switch (syncLog.getEventType()) {
                case "HOUSE" -> "/api/sync/house";
                case "HOUSE_BATCH" -> "/api/sync/house/batch";
                case "PROJECT" -> "/api/sync/project";
                default -> "";
            };

            if (url.isEmpty()) {
                log.warn("未知的事件类型，跳过: {}", syncLog.getEventType());
                continue;
            }

            try {
                HttpEntity<String> entity = new HttpEntity<>(syncLog.getPayload(), headers);
                restTemplate.postForObject(url, entity, String.class);

                // 推送成功，更新状态
                syncLog.setStatus("SUCCESS");
                syncLog.setUpdateTime(LocalDateTime.now());
                syncFailedLogService.updateById(syncLog);
                log.info("补偿成功: logId={}, type={}, businessId={}", syncLog.getId(), syncLog.getEventType(), syncLog.getBusinessId());
            } catch (Exception e) {
                int nextRetryCount = syncLog.getRetryCount() + 1;
                syncLog.setRetryCount(nextRetryCount);
                syncLog.setErrorMsg(e.getMessage());
                syncLog.setUpdateTime(LocalDateTime.now());
                
                if (nextRetryCount >= 3) {
                    syncLog.setStatus("FAILED");
                    log.error("补偿重试已达最大次数，标记为失败: logId={}, businessId={}", syncLog.getId(), syncLog.getBusinessId());
                } else {
                    int backoffMinutes = (int) Math.pow(3, nextRetryCount) * 5; 
                    syncLog.setNextRetryTime(LocalDateTime.now().plusMinutes(backoffMinutes));
                    log.warn("补偿重试失败，已安排下一次重试: logId={}, retryCount={}, nextTime={}", 
                            syncLog.getId(), nextRetryCount, syncLog.getNextRetryTime());
                }
                syncFailedLogService.updateById(syncLog);
            }
        }
    }

    /**
     * 定时任务 2：全量在售房源差集校验与自动补差
     * 每天凌晨 3 点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void consistencyCheck() {
        if (!enabled) return;
        log.info("开始执行在售房源一致性校验与自动校准...");
        
        try {
            List<House> localHouses = houseService.list(
                    new LambdaQueryWrapper<House>()
                            .select(House::getId)
                            .in(House::getStatus, Arrays.asList((byte) 0, (byte) 1))
                            .eq(House::getIsDeleted, (byte) 0)
            );
            Set<Integer> localIds = localHouses.stream().map(House::getId).collect(Collectors.toSet());

            String url = aiServerUrl + "/api/sync/house/ids";
            Integer[] response = restTemplate.getForObject(url, Integer[].class);
            Set<Integer> aiIds = response != null ? Arrays.stream(response).collect(Collectors.toSet()) : Collections.emptySet();

            log.info("房源一致性对比结果：本地在售/待审核数量={}, AI系统现有数量={}", localIds.size(), aiIds.size());

            Set<Integer> toSave = new HashSet<>(localIds);
            toSave.removeAll(aiIds);

            Set<Integer> toDelete = new HashSet<>(aiIds);
            toDelete.removeAll(localIds);

            if (toSave.isEmpty() && toDelete.isEmpty()) {
                log.info("一致性校验通过：两端在售房源数据完全一致，无需校准！");
                return;
            }

            log.warn("一致性对比不一致！需要补推的房源数: {}, 需要补删的房源数: {}", toSave.size(), toDelete.size());

            if (!toDelete.isEmpty()) {
                log.info("开始执行物理删除校准...");
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                for (Integer id : toDelete) {
                    try {
                        String deleteUrl = aiServerUrl + "/api/sync/house";
                        Map<String, Object> body = new HashMap<>();
                        body.put("eventId", UUID.randomUUID().toString());
                        body.put("houseId", id);
                        body.put("action", "DELETE");
                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                        restTemplate.postForObject(deleteUrl, entity, String.class);
                        log.info("下架校准成功: houseId={}", id);
                    } catch (Exception e) {
                        log.error("下架校准失败: houseId={}", id, e);
                    }
                }
            }

            if (!toSave.isEmpty()) {
                log.info("开始执行更新/同步校准...");
                for (Integer id : toSave) {
                    try {
                        houseService.publishHouseSyncEvent(id, "SAVE");
                        log.info("同步校准触发成功: houseId={}", id);
                    } catch (Exception e) {
                        log.error("同步校准触发失败: houseId={}", id, e);
                    }
                }
            }
            
            log.info("房源数据校准完毕！");
        } catch (Exception e) {
            log.error("执行在售房源一致性校验时发生异常", e);
        }
    }
}
