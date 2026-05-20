package com.guang.house.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 新房扩展信息表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("tb_new_house_extend")
@Schema(name = "NewHouseExtend", description = "新房扩展信息表")
public class NewHouseExtend implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 房源ID
     */
    @TableField("house_id")
    @Schema(description = "房源ID")
    private Integer houseId;

    /**
     * 预售许可证号
     */
    @Schema(description = "预售许可证号")
    @TableField("pre_sale_license_no")
    private String preSaleLicenseNo;

    /**
     * 备案价（元/㎡），新房的官方备案价格
     */
    @TableField("record_price")
    @Schema(description = "备案价（元/㎡），新房的官方备案价格")
    private BigDecimal recordPrice;

    /**
     * 楼盘均价（元/㎡），冗余用于快速展示
     */
    @TableField("avg_price")
    @Schema(description = "楼盘均价（元/㎡），冗余用于快速展示")
    private BigDecimal avgPrice;

    /**
     * 产权年限
     */
    @Schema(description = "产权年限")
    @TableField("property_right_years")
    private Integer propertyRightYears;

    /**
     * 预计交房日期
     */
    @Schema(description = "预计交房日期")
    @TableField("estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    /**
     * 交付标准
     */
    @Schema(description = "交付标准")
    @TableField("delivery_standard")
    private String deliveryStandard;

    /**
     * 梯户比
     */
    @Schema(description = "梯户比")
    @TableField("elevator_ratio")
    private String elevatorRatio;

    /**
     * 得房率(%)
     */
    @Schema(description = "得房率(%)")
    @TableField("actual_area_rate")
    private BigDecimal actualAreaRate;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
