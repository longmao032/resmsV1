package com.guang.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门树对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "部门树对象")
public class DeptTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID")
    private Integer id;

    @Schema(description = "父部门ID")
    private Integer parentId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门编码")
    private String deptCode;

    @Schema(description = "部门类型：1=公司，2=部门，3=门店")
    private Byte deptType;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "负责人ID")
    private Integer leaderId;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态：0=禁用，1=启用")
    private Byte status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子部门")
    private List<DeptTreeVO> children = new ArrayList<>();
}
