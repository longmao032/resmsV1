package com.guang.finance.listener;

import com.guang.common.event.TransactionCompletedEvent;
import com.guang.finance.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 交易完成事件监听器
 * 负责触发佣金自动计算
 *
 * @author blackDuck
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCompletedListener {

    private final CommissionService commissionService;

    @Async
    @EventListener
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("监听到交易完成事件，开始异步计算佣金: transactionId={}, salesId={}", 
                event.getTransactionId(), event.getSalesId());
        try {
            commissionService.calculateAndSave(event);
        } catch (Exception e) {
            log.error("佣金计算失败: transactionId={}", event.getTransactionId(), e);
        }
    }
}
