package com.guang.finance.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 项目经营报表 VO
 *
 * @author blackDuck
 */
@Data
@Schema(description = "项目经营报表")
public class ProjectReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Integer projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "累计收款总额")
    private BigDecimal totalIncome;

    @Schema(description = "累计支出佣金")
    private BigDecimal totalCommission;

    @Schema(description = "成交套数")
    private Integer dealCount;
}
