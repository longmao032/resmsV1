package com.guang.house.domain.vo;

import com.guang.house.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 房源详情聚合视图对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "房源详情聚合视图对象")
public class HouseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "房源基本信息")
    private House house;

    @Schema(description = "房源图片列表")
    private List<HouseImage> images;

    @Schema(description = "新房扩展信息")
    private NewHouseExtend newHouseExtend;

    @Schema(description = "二手房扩展信息")
    private SecondHouseExtend secondHouseExtend;

    @Schema(description = "租房扩展信息")
    private RentHouseExtend rentHouseExtend;

    // --- 展示用价格字段 ---
    @Schema(description = "展示用价格（万元或元/月）")
    private BigDecimal price;

    @Schema(description = "展示用价格单位")
    private Byte priceUnit;

    @Schema(description = "展示用单价（元/㎡）")
    private BigDecimal unitPrice;

    @Schema(description = "负责销售姓名")
    private String salesName;

    @Schema(description = "负责销售头像URL")
    private String salesAvatar;
}
