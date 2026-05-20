package com.guang.finance.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 销售业绩报表 VO
 *
 * @author blackDuck
 */
@Data
@Schema(description = "销售业绩报表")
public class SalesReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "销售人员ID")
    private Integer userId;

    @Schema(description = "销售姓名")
    private String realName;

    @Schema(description = "总成交金额")
    private BigDecimal totalDealAmount;

    @Schema(description = "实际回款总额")
    private BigDecimal totalActualPaid;

    @Schema(description = "应发佣金总额")
    private BigDecimal totalCommission;

    @Schema(description = "成交总单数")
    private Integer totalOrders;
}
