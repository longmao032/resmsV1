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
import java.time.LocalDateTime;

/**
 * <p>
 * 二手房扩展信息表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("tb_second_house_extend")
@Schema(name = "SecondHouseExtend", description = "二手房扩展信息表")
public class SecondHouseExtend implements Serializable {

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
     * 二手房总价（万元），冗余用于快速展示
     */
    @TableField("total_price")
    @Schema(description = "二手房总价（万元），冗余用于快速展示")
    private BigDecimal totalPrice;

    /**
     * 建筑年代
     */
    @TableField("build_year")
    @Schema(description = "建筑年代")
    private Integer buildYear;

    /**
     * 房屋用途
     */
    @TableField("house_usage")
    @Schema(description = "房屋用途")
    private String houseUsage;

    /**
     * 是否唯一住房
     */
    @TableField("is_only_house")
    @Schema(description = "是否唯一住房")
    private Byte isOnlyHouse;

    /**
     * 是否满二
     */
    @TableField("is_full_two")
    @Schema(description = "是否满二")
    private Byte isFullTwo;

    /**
     * 是否满五
     */
    @TableField("is_full_five")
    @Schema(description = "是否满五")
    private Byte isFullFive;

    /**
     * 抵押状态：0=无抵押，1=有抵押
     */
    @TableField("mortgage_status")
    @Schema(description = "抵押状态：0=无抵押，1=有抵押")
    private Byte mortgageStatus;

    /**
     * 房本信息
     */
    @TableField("property_deed")
    @Schema(description = "房本信息")
    private String propertyDeed;

    /**
     * 房本图片URL
     */
    @TableField("property_deed_url")
    @Schema(description = "房本图片URL")
    private String propertyDeedUrl;

    /**
     * 上次交易时间
     */
    @Schema(description = "上次交易时间")
    @TableField("last_transaction_time")
    private LocalDateTime lastTransactionTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
