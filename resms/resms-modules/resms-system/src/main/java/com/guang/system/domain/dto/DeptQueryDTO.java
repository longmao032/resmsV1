package com.guang.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门查询对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "部门查询对象")
public class DeptQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "状态：0=禁用，1=启用")
    private Byte status;
}
