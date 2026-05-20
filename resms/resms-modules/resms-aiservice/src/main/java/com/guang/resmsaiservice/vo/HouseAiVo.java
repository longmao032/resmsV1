package com.guang.resmsaiservice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * AI 专属房源精简对象
 * 屏蔽冗余字段，增强语义化描述
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 专属房源精简对象")
public class HouseAiVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "房源ID")
    private Integer id;

    @Schema(description = "房源标题/名称")
    private String houseTitle;

    @Schema(description = "户型（如：3室2厅2卫）")
    private String roomType;

    @Schema(description = "面积描述（如：89.5㎡）")
    private String areaText;

    @Schema(description = "价格描述（如：250万元 或 3500元/月）")
    private String priceText;

    @Schema(description = "单价描述（如：28000元/㎡）")
    private String unitPriceText;

    @Schema(description = "房源标签")
    private List<String> tags;

    @Schema(description = "楼层信息（如：中楼层/32层）")
    private String floorInfo;

    @Schema(description = "朝向（如：南、南北）")
    private String orientation;

    @Schema(description = "房源类型：1=新房，2=二手房，3=租房")
    private Byte houseType;

    @Schema(description = "核心卖点/描述")
    private String description;
}
