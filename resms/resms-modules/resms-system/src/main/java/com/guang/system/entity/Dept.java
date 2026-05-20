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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 部门表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("sys_dept")
@Schema(name = "Dept", description = "部门表")
public class Dept implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    @Schema(description = "部门ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 父部门ID，0为顶级
     */
    @TableField("parent_id")
    @Schema(description = "父部门ID，0为顶级")
    private Integer parentId;

    /**
     * 部门名称
     */
    @TableField("dept_name")
    @Schema(description = "部门名称")
    private String deptName;

    /**
     * 部门编码
     */
    @TableField("dept_code")
    @Schema(description = "部门编码")
    private String deptCode;

    /**
     * 部门负责人ID
     */
    @TableField("leader_id")
    @Schema(description = "部门负责人ID")
    private Integer leaderId;

    /**
     * 排序
     */
    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

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

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 层级路径
     */
    @TableField("ancestors")
    @Schema(description = "层级路径")
    private String ancestors;

    /**
     * 部门类型：1=公司，2=部门，3=门店
     */
    @TableField("dept_type")
    @Schema(description = "部门类型：1=公司，2=部门，3=门店")
    private Byte deptType;

    /**
     * 部门邮箱
     */
    @TableField("email")
    @Schema(description = "部门邮箱")
    private String email;
    
    /**
     * 部门电话
     */
    @TableField("phone")
    @Schema(description = "部门电话")
    private String phone;
}
