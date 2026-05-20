package com.guang.house.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 楼盘分页列表视图对象（含房源聚合数据）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "楼盘分页列表视图对象")
public class ProjectPageVO extends ProjectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "楼盘均价（元/㎡）")
    private BigDecimal avgPrice;

    @Schema(description = "最小面积（㎡）")
    private BigDecimal minArea;

    @Schema(description = "最大面积（㎡）")
    private BigDecimal maxArea;

    @Schema(description = "户型汇总，如 2-4室")
    private String layoutSummary;

    @Schema(description = "在售房源数")
    private Integer houseCount;
}
