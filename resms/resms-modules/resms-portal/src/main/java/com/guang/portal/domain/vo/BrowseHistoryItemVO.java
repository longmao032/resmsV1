package com.guang.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "C端浏览记录展示")
public class BrowseHistoryItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "浏览记录ID")
    private Long id;

    @Schema(description = "资源类型：1=房源，2=项目")
    private Integer resourceType;

    @Schema(description = "资源ID")
    private Integer resourceId;

    @Schema(description = "资源标题")
    private String resourceTitle;

    @Schema(description = "资源封面")
    private String resourceCover;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区域")
    private String district;

    @Schema(description = "户型")
    private String layout;

    @Schema(description = "面积（㎡）")
    private BigDecimal area;

    @Schema(description = "价格描述，如 580万、4500元/月")
    private String priceDesc;

    @Schema(description = "行为类型：view=浏览, call=电话, visit=带看, chat=咨询")
    private String actionType;

    @Schema(description = "交互时长(秒)")
    private Integer duration;

    @Schema(description = "交互时间")
    private String viewTime;
}
