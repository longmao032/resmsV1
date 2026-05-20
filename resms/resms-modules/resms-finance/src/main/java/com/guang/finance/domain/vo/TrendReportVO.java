package com.guang.finance.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 收支趋势报表 VO
 *
 * @author blackDuck
 */
@Data
@Schema(description = "收支趋势报表")
public class TrendReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "时间节点（如：2026-05-07）")
    private String dateStr;

    @Schema(description = "资金流入（收款）")
    private BigDecimal income = BigDecimal.ZERO;

    @Schema(description = "资金流出（佣金发放）")
    private BigDecimal expense = BigDecimal.ZERO;
}
