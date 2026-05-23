package com.guang.common.event;

import com.guang.common.dto.HouseSyncDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class HouseSyncEvent extends ApplicationEvent {
    private final HouseSyncDTO dto;

    public HouseSyncEvent(Object source, HouseSyncDTO dto) {
        super(source);
        this.dto = dto;
    }
}
