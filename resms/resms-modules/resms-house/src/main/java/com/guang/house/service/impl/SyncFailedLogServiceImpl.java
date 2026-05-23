package com.guang.house.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guang.house.entity.SyncFailedLog;
import com.guang.house.mapper.SyncFailedLogMapper;
import com.guang.house.service.SyncFailedLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncFailedLogServiceImpl extends ServiceImpl<SyncFailedLogMapper, SyncFailedLog> implements SyncFailedLogService {

    private final ObjectMapper objectMapper;

    @Override
    public void saveLog(String eventType, Integer businessId, Object payload, String errorMsg) {
        try {
            SyncFailedLog syncFailedLog = new SyncFailedLog();
            syncFailedLog.setEventType(eventType);
            syncFailedLog.setBusinessId(businessId);
            syncFailedLog.setPayload(objectMapper.writeValueAsString(payload));
            syncFailedLog.setRetryCount(0);
            syncFailedLog.setNextRetryTime(LocalDateTime.now().plusMinutes(5)); // 5分钟后进行第一次补偿重试
            syncFailedLog.setStatus("PENDING");
            syncFailedLog.setErrorMsg(errorMsg);
            syncFailedLog.setCreateTime(LocalDateTime.now());
            syncFailedLog.setUpdateTime(LocalDateTime.now());
            this.save(syncFailedLog);
            log.info("同步失败日志已持久化保存: businessId={}, eventType={}", businessId, eventType);
        } catch (Exception e) {
            log.error("保存同步失败日志发生异常", e);
        }
    }
}
