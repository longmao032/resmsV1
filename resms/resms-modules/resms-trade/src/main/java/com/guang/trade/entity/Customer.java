package com.guang.trade.entity;

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
 * 客户信息表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("tb_customer")
@Schema(name = "Customer", description = "客户信息表")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户ID
     */
    @Schema(description = "客户ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    /**
     * 绑定的C端账号ID
     */
    @TableField("app_user_id")
    @Schema(description = "绑定的C端账号ID")
    private Long appUserId;

    /**
     * 客户编号
     */
    @TableField("customer_no")
    @Schema(description = "客户编号")
    private String customerNo;

    /**
     * 客户姓名
     */
    @TableField("real_name")
    @Schema(description = "客户姓名")
    private String realName;

    /**
     * 客户电话
     */
    @TableField("phone")
    @Schema(description = "客户电话")
    private String phone;

    /**
     * 身份证号（脱敏存储）
     */
    @TableField("id_card")
    @Schema(description = "身份证号（脱敏存储）")
    private String idCard;

    /**
     * 意向面积
     */
    @TableField("demand_area")
    @Schema(description = "意向面积")
    private BigDecimal demandArea;

    /**
     * 意向价格
     */
    @TableField("demand_price")
    @Schema(description = "意向价格")
    private BigDecimal demandPrice;

    /**
     * 意向户型
     */
    @TableField("demand_layout")
    @Schema(description = "意向户型")
    private String demandLayout;

    /**
     * 意向区域
     */
    @Schema(description = "意向区域")
    @TableField("demand_area_region")
    private String demandAreaRegion;

    /**
     * 意向等级：1=高，2=中，3=低
     */
    @TableField("intention_level")
    @Schema(description = "意向等级：1=高，2=中，3=低")
    private Byte intentionLevel;

    /**
     * 对接销售ID
     */
    @TableField("sales_id")
    @Schema(description = "对接销售ID")
    private Integer salesId;

    /**
     * 客户来源
     */
    @TableField("source")
    @Schema(description = "客户来源")
    private String source;

    /**
     * 是否删除：0=未删除，1=已删除
     */
    @TableField("is_deleted")
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
