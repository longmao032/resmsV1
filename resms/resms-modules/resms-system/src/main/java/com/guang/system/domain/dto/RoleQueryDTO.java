package com.guang.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色查询对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "角色查询对象")
public class RoleQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色代码")
    private String roleCode;

    @Schema(description = "状态：0=禁用，1=启用")
    private Byte status;
}
