package com.guang.finance.listener;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.guang.common.event.TransactionCancelledEvent;
import com.guang.finance.entity.Payment;
import com.guang.finance.mapper.PaymentMapper;
import com.guang.trade.entity.PaymentPlan;
import com.guang.trade.mapper.PaymentPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 交易取消事件监听器
 * 负责联动处理财务账单和支付流水
 *
 * @author blackDuck
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCancelledListener {

    private final PaymentPlanMapper paymentPlanMapper;
    private final PaymentMapper paymentMapper;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleTransactionCancelled(TransactionCancelledEvent event) {
        Integer txId = event.getTransactionId();
        log.info("监听到交易取消事件，开始处理财务联动: transactionId={}", txId);

        // 1. 将所有关联的支付计划置为“已取消” (状态码暂定为 3)
        LambdaUpdateWrapper<PaymentPlan> planWrapper = new LambdaUpdateWrapper<>();
        planWrapper.eq(PaymentPlan::getTransactionId, txId)
                .set(PaymentPlan::getStatus, (byte) 3);
        int planCount = paymentPlanMapper.update(null, planWrapper);
        log.info("已更新支付计划状态为已取消: transactionId={}, count={}", txId, planCount);

        // 2. 将所有”待确认”的支付流水置为”已作废” (状态码 2)，排除退款流水
        LambdaUpdateWrapper<Payment> paymentWrapper = new LambdaUpdateWrapper<>();
        paymentWrapper.eq(Payment::getTransactionId, txId)
                .eq(Payment::getPaymentStatus, (byte) 0) // 待确认
                .ne(Payment::getFlowType, (byte) 2) // 排除退款流水
                .set(Payment::getPaymentStatus, (byte) 2); // 已作废
        int paymentCount = paymentMapper.update(null, paymentWrapper);
        log.info("已作废待确认的支付流水: transactionId={}, count={}", txId, paymentCount);

        // 3. 检查是否存在已生效的流水，若存在则记录警告，提示人工介入退款
        LambdaUpdateWrapper<Payment> effectiveCheck = new LambdaUpdateWrapper<>();
        effectiveCheck.eq(Payment::getTransactionId, txId)
                .eq(Payment::getPaymentStatus, (byte) 1); // 有效
        long effectiveCount = paymentMapper.selectCount(effectiveCheck);
        if (effectiveCount > 0) {
            log.warn("注意：交易取消，但存在 {} 笔已生效的支付流水，请财务人员人工核实并处理退款! transactionId={}", 
                    effectiveCount, txId);
        }
    }
}
