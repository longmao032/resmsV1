package com.guang.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单逾期事件
 *
 * @author blackDuck
 */
@Getter
public class PaymentOverdueEvent extends ApplicationEvent {

    private final Integer transactionId;
    private final String transactionNo;
    private final String payName;
    private final BigDecimal receivableAmount;
    private final LocalDateTime dueDate;
    private final Integer salesId;

    public PaymentOverdueEvent(Object source, Integer transactionId, String transactionNo, String payName, BigDecimal receivableAmount, LocalDateTime dueDate, Integer salesId) {
        super(source);
        this.transactionId = transactionId;
        this.transactionNo = transactionNo;
        this.payName = payName;
        this.receivableAmount = receivableAmount;
        this.dueDate = dueDate;
        this.salesId = salesId;
    }
}
