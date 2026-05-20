package com.guang.trade.strategy;

import com.guang.trade.entity.Transaction;

import java.math.BigDecimal;

/**
 * 交易支付策略接口 — 每种付款方式对应一种策略，内聚"支付审核通过后的状态推进"逻辑。
 *
 * @author blackDuck
 */
public interface OrderTriggerStrategy {

    /**
     * 返回支持的付款方式
     *
     * @see com.guang.trade.entity.Transaction #paymentType 1=一次性 2=分期 3=按揭 4=租房
     */
    Integer getPaymentMode();

    /**
     * 处理支付审核通过后的交易状态推进。
     *
     * @param transaction 交易对象（调用方已加行锁）
     * @param payType     款项类型 1=定金 2=首付款 3=尾款 5=贷款
     * @param actualAmount 实际到账金额
     * @param allSettled  是否当前交易下所有账单均已结清
     */
    void handlePaymentSuccess(Transaction transaction, Byte payType, BigDecimal actualAmount, boolean allSettled);
}
