package com.guang.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 交易创建事件
 *
 * @author blackDuck
 */
@Getter
public class TransactionCreatedEvent extends ApplicationEvent {

    private final Integer transactionId;
    private final Integer ownerId;
    private final String houseName;
    private final BigDecimal dealPrice;

    public TransactionCreatedEvent(Object source, Integer transactionId, Integer ownerId, String houseName, BigDecimal dealPrice) {
        super(source);
        this.transactionId = transactionId;
        this.ownerId = ownerId;
        this.houseName = houseName;
        this.dealPrice = dealPrice;
    }
}
