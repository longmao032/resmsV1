package com.guang.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 线索/客户指派事件
 *
 * @author blackDuck
 */
@Getter
public class LeadAssignedEvent extends ApplicationEvent {

    private final Integer customerId;
    private final String customerName;
    private final String phone;
    private final String source;
    private final Integer salesId;

    public LeadAssignedEvent(Object source, Integer customerId, String customerName, String phone, String sourceName, Integer salesId) {
        super(source);
        this.customerId = customerId;
        this.customerName = customerName;
        this.phone = phone;
        this.source = sourceName;
        this.salesId = salesId;
    }
}
