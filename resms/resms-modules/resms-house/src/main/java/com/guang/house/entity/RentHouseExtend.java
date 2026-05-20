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
 * 租房扩展信息表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("tb_rent_house_extend")
@Schema(name = "RentHouseExtend", description = "租房扩展信息表")
public class RentHouseExtend implements Serializable {

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
     * 月租金（元/月），冗余用于快速展示
     */
    @TableField("monthly_rent")
    @Schema(description = "月租金（元/月），冗余用于快速展示")
    private BigDecimal monthlyRent;

    /**
     * 出租方式：1=整租，2=合租
     */
    @TableField("rent_type")
    @Schema(description = "出租方式：1=整租，2=合租")
    private Byte rentType;

    /**
     * 押金方式，如押一付三、押二付一
     */
    @TableField("deposit_method")
    @Schema(description = "押金方式，如押一付三、押二付一")
    private String depositMethod;

    /**
     * 押金金额（元）
     */
    @TableField("deposit_amount")
    @Schema(description = "押金金额（元）")
    private BigDecimal depositAmount;

    /**
     * 可入住日期
     */
    @TableField("check_in_date")
    @Schema(description = "可入住日期")
    private LocalDate checkInDate;

    /**
     * 最短租期（月）
     */
    @TableField("min_lease_period")
    @Schema(description = "最短租期（月）")
    private Integer minLeasePeriod;

    /**
     * 是否支持短租
     */
    @Schema(description = "是否支持短租")
    @TableField("support_short_rent")
    private Byte supportShortRent;

    /**
     * 配套设施，逗号分隔
     */
    @TableField("appliances")
    @Schema(description = "配套设施，逗号分隔")
    private String appliances;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
