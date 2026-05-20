package com.guang.trade.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户足迹视图对象
 */
@Data
@Schema(description = "客户足迹视图对象")
public class FootprintVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "客户头像")
    private String avatar;

    @Schema(description = "房源标题")
    private String houseTitle;

    @Schema(description = "房源封面")
    private String houseCover;

    @Schema(description = "房源类型")
    private String houseType;

    @Schema(description = "房源编号")
    private String houseNo;

    @Schema(description = "户型")
    private String layout;

    @Schema(description = "建筑面积（㎡）")
    private BigDecimal area;

    @Schema(description = "楼层信息")
    private String floorLabel;

    @Schema(description = "朝向")
    private String orientation;

    @Schema(description = "装修情况")
    private String decoration;

    @Schema(description = "单价（元/㎡）")
    private BigDecimal unitPrice;

    @Schema(description = "总价（万元）")
    private BigDecimal totalPrice;

    @Schema(description = "月租（元/月）")
    private BigDecimal rentPrice;

    @Schema(description = "行为类型")
    private String actionType;

    @Schema(description = "交互时长(秒)")
    private Integer duration;

    @Schema(description = "意向评估(1-5星)")
    private Byte interestLevel;

    @Schema(description = "交互备注")
    private String content;

    @Schema(description = "交互时间")
    private LocalDateTime viewTime;
}
