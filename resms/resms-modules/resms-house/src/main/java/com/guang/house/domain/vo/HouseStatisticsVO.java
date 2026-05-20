package com.guang.house.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "房源统计概览")
public class HouseStatisticsVO {

    @Schema(description = "在线房源数（在售）")
    private long onlineCount;

    @Schema(description = "在线房源较上月增长比例（百分比，保留一位小数）")
    private double onlineTrend;

    @Schema(description = "本月新增房源数")
    private long monthlyNewCount;

    @Schema(description = "本月新增较上月增长比例")
    private double monthlyNewTrend;

    @Schema(description = "待审核房源数")
    private long pendingReviewCount;

    @Schema(description = "待审核较上月增长比例")
    private double pendingReviewTrend;

    @Schema(description = "成交均价（万元），已成交房源的平均总价")
    private BigDecimal avgDealPrice;

    @Schema(description = "成交均价较上月增长比例")
    private double avgDealPriceTrend;
}
