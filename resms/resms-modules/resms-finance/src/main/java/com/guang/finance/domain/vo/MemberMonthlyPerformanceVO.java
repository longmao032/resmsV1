package com.guang.finance.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "成员月度业绩VO")
public class MemberMonthlyPerformanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Integer userId;

    @Schema(description = "成交总额（万元）")
    private BigDecimal totalDealAmount;

    @Schema(description = "成交单数")
    private Integer totalOrders;
}
