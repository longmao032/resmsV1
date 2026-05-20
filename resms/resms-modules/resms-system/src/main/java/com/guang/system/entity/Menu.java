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
 * 系统菜单表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("sys_menu")
@Schema(name = "Menu", description = "系统菜单表")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID
     */
    @Schema(description = "菜单ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 父菜单ID，0为顶级
     */
    @TableField("parent_id")
    @Schema(description = "父菜单ID，0为顶级")
    private Integer parentId;

    /**
     * 菜单名称
     */
    @TableField("menu_name")
    @Schema(description = "菜单名称")
    private String menuName;

    /**
     * 菜单标识
     */
    @TableField("menu_code")
    @Schema(description = "菜单标识")
    private String menuCode;

    /**
     * 菜单类型：1=目录，2=菜单，3=按钮
     */
    @TableField("menu_type")
    @Schema(description = "菜单类型：1=目录，2=菜单，3=按钮")
    private Byte menuType;

    /**
     * 路由路径
     */
    @TableField("path")
    @Schema(description = "路由路径")
    private String path;

    /**
     * 组件路径
     */
    @TableField("component")
    @Schema(description = "组件路径")
    private String component;

    /**
     * 图标
     */
    @TableField("icon")
    @Schema(description = "图标")
    private String icon;

    /**
     * 排序
     */
    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    /**
     * 是否可见：0=隐藏，1=可见
     */
    @TableField("visible")
    @Schema(description = "是否可见：0=隐藏，1=可见")
    private Byte visible;

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
}
