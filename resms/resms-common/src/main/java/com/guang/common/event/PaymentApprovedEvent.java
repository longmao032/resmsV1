package com.guang.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 款项审核通过事件
 * 用于财务模块与交易模块的金额与状态同步
 *
 * @author blackDuck
 */
@Getter
public class PaymentApprovedEvent extends ApplicationEvent {

    /**
     * 交易ID
     */
    private final Integer transactionId;

    /**
     * 款项类型：1=定金，2=首付款，3=尾款，4=中介费，5=贷款
     */
    private final Byte paymentType;

    /**
     * 本次审核通过的实际金额
     */
    private final BigDecimal amount;

    public PaymentApprovedEvent(Object source, Integer transactionId, Byte paymentType, BigDecimal amount) {
        super(source);
        this.transactionId = transactionId;
        this.paymentType = paymentType;
        this.amount = amount;
    }
}
