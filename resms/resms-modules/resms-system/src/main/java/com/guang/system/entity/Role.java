package com.guang.system.entity;

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
 * 角色表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("sys_role")
@Schema(name = "Role", description = "角色表")
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @Schema(description = "角色ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 角色名称
     */
    @TableField("role_name")
    @Schema(description = "角色名称")
    private String roleName;

    /**
     * 角色代码
     */
    @TableField("role_code")
    @Schema(description = "角色代码")
    private String roleCode;

    /**
     * 描述
     */
    @TableField("description")
    @Schema(description = "描述")
    private String description;

    /**
     * 数据权限：1=全部，2=本部门，3=本部门及子部门，4=仅本人
     */
    @TableField("data_scope")
    @Schema(description = "数据权限：1=全部，2=本部门，3=本部门及子部门，4=仅本人")
    private Byte dataScope;

    /**
     * 状态：0=禁用，1=启用
     */
    @TableField("status")
    @Schema(description = "状态：0=禁用，1=启用")
    private Byte status;

    /**
     * 是否删除：0=未删除，1=已删除
     */
    @TableField("is_deleted")
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    /**
     * 创建人ID
     */
    @TableField("create_user_id")
    @Schema(description = "创建人ID")
    private Integer createUserId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
