package com.guang.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "C端收藏房源展示")
public class FavoriteItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收藏记录ID")
    private Integer id;

    @Schema(description = "房源ID")
    private Integer houseId;

    @Schema(description = "房源编号")
    private String houseNo;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "房源类型：1=新房，2=二手房，3=租房")
    private Integer houseType;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区域")
    private String district;

    @Schema(description = "建筑面积（㎡）")
    private BigDecimal area;

    @Schema(description = "户型")
    private String layout;

    @Schema(description = "展示用价格（万元或元/月）")
    private BigDecimal totalPrice;

    @Schema(description = "价格单位")
    private String priceUnit;

    @Schema(description = "单价（元/㎡）")
    private BigDecimal unitPrice;

    @Schema(description = "收藏时间")
    private String favoriteTime;
}
