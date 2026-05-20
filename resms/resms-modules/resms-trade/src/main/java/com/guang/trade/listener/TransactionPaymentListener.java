package com.guang.trade.listener;

import com.guang.common.event.PaymentApprovedEvent;
import com.guang.trade.entity.Transaction;
import com.guang.trade.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 支付流水审核通过监听器
 * <p>
 * 注意：交易状态推进已在 {@code PaymentServiceImpl.doReconciliation} 中
 * 通过 {@link com.guang.trade.strategy.OrderTriggerStrategy} 完成。
 * 本监听器仅负责已收金额的累加（strategy 不处理该字段）。
 * </p>
 *
 * @author blackDuck
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionPaymentListener {

    private final TransactionService transactionService;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentApproved(PaymentApprovedEvent event) {
        log.info("监听到收款审核通过事件: transactionId={}, amount={}, type={}",
                event.getTransactionId(), event.getAmount(), event.getPaymentType());

        // 已收金额累加（状态推进由 doReconciliation → strategy 处理）
        Transaction transaction = transactionService.getById(event.getTransactionId());
        if (transaction == null) {
            log.warn("交易记录不存在，忽略处理: transactionId={}", event.getTransactionId());
            return;
        }

        BigDecimal currentPaid = transaction.getActualPaidAmount() != null ? transaction.getActualPaidAmount() : BigDecimal.ZERO;
        transaction.setActualPaidAmount(currentPaid.add(event.getAmount()));
        transactionService.updateById(transaction);

        log.debug("已收金额已更新: transactionId={}, newPaidAmount={}",
                event.getTransactionId(), transaction.getActualPaidAmount());
    }
}
