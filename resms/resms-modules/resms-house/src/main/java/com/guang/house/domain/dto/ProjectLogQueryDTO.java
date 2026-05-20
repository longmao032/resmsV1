package com.guang.house.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "ProjectLogQueryDTO", description = "项目变更日志查询DTO")
public class ProjectLogQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "项目ID")
    private Integer projectId;
}
