package com.guang.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(name = "DeptTargetVO", description = "部门月度目标VO")
public class DeptTargetVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Integer id;

    @Schema(description = "部门ID")
    private Integer deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "目标月份, YYYY-MM")
    private String targetMonth;

    @Schema(description = "目标业绩金额（万元）")
    private BigDecimal targetAmount;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
