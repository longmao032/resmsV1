package com.guang.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 部门保存对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "部门保存对象")
public class DeptSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID（更新时必填）")
    private Integer id;

    @Schema(description = "父部门ID，0为顶级")
    private Integer parentId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门编码")
    private String deptCode;

    @Schema(description = "部门负责人ID")
    private Integer leaderId;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态：0=禁用，1=启用")
    private Byte status;

    @Schema(description = "部门类型：1=公司，2=部门，3=门店")
    private Byte deptType;

    @Schema(description = "部门邮箱")
    private String email;

    @Schema(description = "部门电话")
    private String phone;
}
