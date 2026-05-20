package com.guang.resms.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "仪表盘核心统计数据")
public class DashboardStatsVO {

    @Schema(description = "新增房源数")
    private Long newHouses;

    @Schema(description = "本月订单数")
    private Long monthlyOrders;

    @Schema(description = "佣金总额（元）")
    private java.math.BigDecimal totalCommission;

    @Schema(description = "活跃客户数")
    private Long activeClients;
}
