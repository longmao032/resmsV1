package com.guang.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 房源状态变更事件
 * 用于模块间状态联动（如交易模块触发房源状态变更）
 *
 * @author blackDuck
 */
@Getter
public class HouseStatusChangeEvent extends ApplicationEvent {

    /**
     * 房源ID
     */
    private final Integer houseId;

    /**
     * 目标状态
     */
    private final Byte status;

    /**
     * 期望的当前状态（用于并发控制/乐观锁）
     * 如果为 null，则不校验前置状态
     */
    private final Byte expectedStatus;

    /**
     * 变更原因
     */
    private final String reason;

    /**
     * 房源名称
     */
    private final String houseName;

    /**
     * 操作人姓名
     */
    private final String operatorName;

    public HouseStatusChangeEvent(Object source, Integer houseId, String houseName, Byte status, Byte expectedStatus, String reason, String operatorName) {
        super(source);
        this.houseId = houseId;
        this.houseName = houseName;
        this.status = status;
        this.expectedStatus = expectedStatus;
        this.reason = reason;
        this.operatorName = operatorName;
    }
}
