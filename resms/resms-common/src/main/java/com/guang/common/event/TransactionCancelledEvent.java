package com.guang.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 交易取消事件
 *
 * @author blackDuck
 */
@Getter
public class TransactionCancelledEvent extends ApplicationEvent {

    private final Integer transactionId;
    private final Integer salesId;
    private final String houseName;
    private final String reason;

    public TransactionCancelledEvent(Object source, Integer transactionId, Integer salesId, String houseName, String reason) {
        super(source);
        this.transactionId = transactionId;
        this.salesId = salesId;
        this.houseName = houseName;
        this.reason = reason;
    }
}
