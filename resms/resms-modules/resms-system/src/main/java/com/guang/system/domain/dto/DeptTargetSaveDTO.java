package com.guang.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(name = "DeptTargetSaveDTO", description = "部门月度目标保存DTO")
public class DeptTargetSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID（更新时必填）")
    private Integer id;

    @NotNull(message = "部门ID不能为空")
    @Schema(description = "部门ID")
    private Integer deptId;

    @NotBlank(message = "目标月份不能为空")
    @Schema(description = "目标月份, YYYY-MM")
    private String targetMonth;

    @NotNull(message = "目标金额不能为空")
    @Schema(description = "目标业绩金额（万元）")
    private BigDecimal targetAmount;
}
