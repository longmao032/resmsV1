package com.guang.resms.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Schema(description = "经营走势数据点")
public class DashboardTrendVO {

    @Schema(description = "日期（MM-dd）")
    private String dateStr;

    @Schema(description = "当日成交金额（元）")
    private BigDecimal value;
}
