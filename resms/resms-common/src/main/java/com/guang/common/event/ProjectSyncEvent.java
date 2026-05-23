package com.guang.common.event;

import com.guang.common.dto.ProjectSyncDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ProjectSyncEvent extends ApplicationEvent {
    private final ProjectSyncDTO dto;

    public ProjectSyncEvent(Object source, ProjectSyncDTO dto) {
        super(source);
        this.dto = dto;
    }
}
