package com.guang.house.listener;

import com.guang.common.dto.HouseSyncDTO;
import com.guang.common.dto.ProjectSyncDTO;
import com.guang.common.event.HouseSyncBatchEvent;
import com.guang.common.event.HouseSyncEvent;
import com.guang.common.event.ProjectSyncEvent;
import com.guang.house.service.SyncFailedLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HouseDataSyncListener {

    private final SyncFailedLogService syncFailedLogService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sync.to-ai.enabled:true}")
    private boolean enabled;

    @Value("${sync.to-ai.url:http://localhost:9000}")
    private String aiServerUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleHouseSync(HouseSyncEvent event) {
        if (!enabled) return;
        HouseSyncDTO dto = event.getDto();
        log.info("监听到房源实时同步事件: houseId={}, action={}", dto.getHouseId(), dto.getAction());
        
        executeWithRetry(() -> {
            restTemplate.postForObject(aiServerUrl + "/api/sync/house", dto, String.class);
        }, "HOUSE", dto.getHouseId(), dto);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleHouseSyncBatch(HouseSyncBatchEvent event) {
        if (!enabled) return;
        List<HouseSyncDTO> dtos = event.getDtos();
        if (dtos == null || dtos.isEmpty()) return;
        log.info("监听到批量房源实时同步事件: size={}", dtos.size());
        
        Integer businessId = dtos.get(0).getHouseId();
        executeWithRetry(() -> {
            restTemplate.postForObject(aiServerUrl + "/api/sync/house/batch", dtos, String.class);
        }, "HOUSE_BATCH", businessId, dtos);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProjectSync(ProjectSyncEvent event) {
        if (!enabled) return;
        ProjectSyncDTO dto = event.getDto();
        log.info("监听到楼盘实时同步事件: projectId={}, action={}", dto.getProjectId(), dto.getAction());
        
        executeWithRetry(() -> {
            restTemplate.postForObject(aiServerUrl + "/api/sync/project", dto, String.class);
        }, "PROJECT", dto.getProjectId(), dto);
    }

    private void executeWithRetry(Runnable runnable, String businessType, Integer businessId, Object payload) {
        int maxAttempts = 3;
        int delay = 1000;
        Exception lastException = null;
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                runnable.run();
                log.info("数据同步到AI系统成功: businessId={}, type={}", businessId, businessType);
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("数据同步失败，第 {} 次重试... 错误: {}", i, e.getMessage());
                if (i < maxAttempts) {
                    try {
                        Thread.sleep(delay * i);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
        // 3次全部失败，执行补偿落库
        log.error("同步数据到AI系统最终失败，转入本地日志表进行补偿。业务ID: {}", businessId);
        syncFailedLogService.saveLog(businessType, businessId, payload, lastException != null ? lastException.getMessage() : "Unknown error");
    }
}
