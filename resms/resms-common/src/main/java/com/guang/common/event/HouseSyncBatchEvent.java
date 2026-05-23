package com.guang.common.event;

import com.guang.common.dto.HouseSyncDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.List;

@Getter
public class HouseSyncBatchEvent extends ApplicationEvent {
    private final List<HouseSyncDTO> dtos;

    public HouseSyncBatchEvent(Object source, List<HouseSyncDTO> dtos) {
        super(source);
        this.dtos = dtos;
    }
}
