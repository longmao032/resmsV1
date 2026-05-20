package com.guang.finance.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 团队业绩看板 VO
 */
@Data
@Schema(description = "团队业绩看板")
public class TeamPerformanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "顶部概览统计")
    private SummaryStats summary;

    @Schema(description = "月度趋势")
    private List<MonthlyTrendItem> monthlyTrend;

    @Schema(description = "销售排行")
    private List<SalesReportVO> salesRanking;

    @Schema(description = "房源类型分布")
    private List<HouseTypeDistItem> houseTypeDistribution;

    @Schema(description = "客源转化漏斗")
    private FunnelData funnel;

    @Data
    public static class SummaryStats implements Serializable {
        private static final long serialVersionUID = 1L;
        private BigDecimal totalDealAmount;
        private Integer newCustomerCount;
        private Integer totalViewings;
        private BigDecimal targetRate;
    }

    @Data
    public static class MonthlyTrendItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String month;
        private BigDecimal value;
    }

    @Data
    public static class HouseTypeDistItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private Integer value;
        private String color;
    }

    @Data
    public static class FunnelData implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer customerCount;
        private Integer viewingCount;
        private Integer dealCount;
    }
}
