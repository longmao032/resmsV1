package com.guang.house.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(name = "ProjectLogVO", description = "项目变更日志VO")
public class ProjectLogVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "项目ID")
    private Integer projectId;

    @Schema(description = "变更字段中文名")
    private String fieldLabel;

    @Schema(description = "变更前值")
    private String oldValue;

    @Schema(description = "变更后值")
    private String newValue;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作IP")
    private String ipAddress;

    @Schema(description = "变更时间")
    private LocalDateTime createTime;
}
