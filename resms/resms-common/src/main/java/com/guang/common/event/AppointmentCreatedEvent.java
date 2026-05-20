package com.guang.common.event;
 
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
 
import java.time.LocalDateTime;
 
/**
 * 预约创建事件
 */
@Getter
public class AppointmentCreatedEvent extends ApplicationEvent {
 
    private final Integer appointmentId;
    private final Integer customerId;
    private final String customerName;
    private final String customerPhone;
    private final Integer houseId;
    private final String projectName;
    private final LocalDateTime viewTime;
    private final Integer salesId;
 
    public AppointmentCreatedEvent(Object source, Integer appointmentId, Integer customerId, String customerName, 
                                   String customerPhone, Integer houseId, String projectName, 
                                   LocalDateTime viewTime, Integer salesId) {
        super(source);
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.houseId = houseId;
        this.projectName = projectName;
        this.viewTime = viewTime;
        this.salesId = salesId;
    }
}
