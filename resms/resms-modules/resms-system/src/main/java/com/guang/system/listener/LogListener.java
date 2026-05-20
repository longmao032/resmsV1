package com.guang.system.listener;

import com.guang.common.security.LogDTO;
import com.guang.system.entity.OperationLog;
import com.guang.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 操作日志事件监听器
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LogListener {

    private final OperationLogService operationLogService;

    @Async // 异步保存日志，不阻塞主业务
    @EventListener
    public void saveLog(LogDTO logDTO) {
        try {
            OperationLog operationLog = new OperationLog();
            BeanUtils.copyProperties(logDTO, operationLog);
            operationLogService.save(operationLog);
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage());
        }
    }
}
