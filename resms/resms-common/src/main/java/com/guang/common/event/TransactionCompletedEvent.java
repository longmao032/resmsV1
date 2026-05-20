package com.guang.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 交易完成事件
 * 用于触发佣金计算等后续业务
 *
 * @author blackDuck
 */
@Getter
public class TransactionCompletedEvent extends ApplicationEvent {

    private final Integer transactionId;
    private final Integer houseId;
    private final Integer salesId;
    private final String houseName;
    private final BigDecimal dealPrice;

    public TransactionCompletedEvent(Object source, Integer transactionId, Integer houseId, Integer salesId, String houseName, BigDecimal dealPrice) {
        super(source);
        this.transactionId = transactionId;
        this.houseId = houseId;
        this.salesId = salesId;
        this.houseName = houseName;
        this.dealPrice = dealPrice;
    }
}
