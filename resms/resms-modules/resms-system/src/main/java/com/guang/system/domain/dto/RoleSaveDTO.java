package com.guang.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色保存对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "角色保存对象")
public class RoleSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID（更新时必填）")
    private Integer id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色代码")
    private String roleCode;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "数据权限：1=全部，2=本部门，3=本部门及子部门，4=仅本人")
    private Byte dataScope;

    @Schema(description = "状态：0=禁用，1=启用")
    private Byte status;

    @Schema(description = "关联菜单ID列表")
    private List<Integer> menuIds;

}
