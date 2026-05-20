package com.guang.house.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "项目统计概览")
public class ProjectStatisticsVO {

    @Schema(description = "项目总数")
    private long totalCount;

    @Schema(description = "项目总数较上月增长比例")
    private double totalTrend;

    @Schema(description = "在售楼盘数")
    private long onSaleCount;

    @Schema(description = "在售楼盘较上月增长比例")
    private double onSaleTrend;

    @Schema(description = "待开项目数")
    private long pendingCount;

    @Schema(description = "待开项目较上月增长比例")
    private double pendingTrend;

    @Schema(description = "平均佣金比例（%）")
    private BigDecimal avgCommissionRate;

    @Schema(description = "平均佣金较上月增长比例")
    private double avgCommissionTrend;
}
