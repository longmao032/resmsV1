package com.guang.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树返回对象
 */
@Data
@Schema(description = "菜单树返回对象")
public class MenuVO {

    @Schema(description = "菜单ID")
    private Integer id;

    @Schema(description = "父菜单ID")
    private Integer parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单标识")
    private String menuCode;

    @Schema(description = "菜单类型：1=目录，2=菜单，3=按钮")
    private Byte menuType;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "是否可见：0=隐藏，1=可见")
    private Byte visible;

    @Schema(description = "状态：0=禁用，1=启用")
    private Byte status;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "子菜单列表")
    private List<MenuVO> children = new ArrayList<>();
}
