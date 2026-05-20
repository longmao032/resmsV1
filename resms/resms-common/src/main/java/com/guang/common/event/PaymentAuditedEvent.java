package com.guang.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 支付核销审核事件
 *
 * @author blackDuck
 */
@Getter
public class PaymentAuditedEvent extends ApplicationEvent {

    private final Integer transactionId;
    private final String payName; // 款项名称
    private final BigDecimal amount;
    private final Byte status; // 1=通过, 2=驳回
    private final String remark; // 审核备注（驳回原因）
    private final Integer salesId; // 接收人

    public PaymentAuditedEvent(Object source, Integer transactionId, String payName, BigDecimal amount, Byte status, String remark, Integer salesId) {
        super(source);
        this.transactionId = transactionId;
        this.payName = payName;
        this.amount = amount;
        this.status = status;
        this.remark = remark;
        this.salesId = salesId;
    }
}
