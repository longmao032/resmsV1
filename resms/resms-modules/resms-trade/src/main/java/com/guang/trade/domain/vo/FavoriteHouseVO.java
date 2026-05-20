package com.guang.trade.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源收藏统计视图对象
 */
@Data
@Schema(description = "房源收藏统计视图对象")
public class FavoriteHouseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "房源ID")
    private Integer id;

    @Schema(description = "房源标题")
    private String houseTitle;

    @Schema(description = "房源封面")
    private String cover;

    @Schema(description = "房源类型：1=新房，2=二手房，3=租房")
    private Byte houseType;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "户型")
    private String layout;

    @Schema(description = "面积")
    private BigDecimal area;

    @Schema(description = "收藏总数")
    private Long favCount;

    @Schema(description = "最后收藏时间")
    private LocalDateTime lastFav;

    @Schema(description = "是否热门")
    private Boolean isHot;
}
