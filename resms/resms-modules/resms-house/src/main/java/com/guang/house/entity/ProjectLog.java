package com.guang.house.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 项目变更日志表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-13
 */
@Getter
@Setter
@ToString
@TableName("tb_project_log")
@Schema(name = "ProjectLog", description = "项目变更日志表")
public class ProjectLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "项目ID")
    @TableField("project_id")
    private Integer projectId;

    @Schema(description = "变更字段中文名")
    @TableField("field_label")
    private String fieldLabel;

    @Schema(description = "变更前值（创建时为NULL）")
    @TableField("old_value")
    private String oldValue;

    @Schema(description = "变更后值")
    @TableField("new_value")
    private String newValue;

    @Schema(description = "操作人ID")
    @TableField("operator_id")
    private Integer operatorId;

    @Schema(description = "操作人姓名")
    @TableField("operator_name")
    private String operatorName;

    @Schema(description = "操作IP")
    @TableField("ip_address")
    private String ipAddress;

    @Schema(description = "变更时间")
    @TableField("create_time")
    private LocalDateTime createTime;
}
