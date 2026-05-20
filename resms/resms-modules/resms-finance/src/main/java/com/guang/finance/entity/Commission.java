package com.guang.finance.entity;

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
 * 销售佣金表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("tb_commission")
@Schema(name = "Commission", description = "销售佣金表")
public class Commission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 佣金ID
     */
    @Schema(description = "佣金ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 交易ID
     */
    @Schema(description = "交易ID")
    @TableField("transaction_id")
    private Integer transactionId;

    /**
     * 销售ID
     */
    @TableField("sales_id")
    @Schema(description = "销售ID")
    private Integer salesId;

    /**
     * 提成比例(%)
     */
    @TableField("commission_rate")
    @Schema(description = "提成比例(%)")
    private BigDecimal commissionRate;

    /**
     * 佣金金额（元）
     */
    @TableField("amount")
    @Schema(description = "佣金金额（元）")
    private BigDecimal amount;

    /**
     * 状态：0=待核算，1=已核算，2=已发放
     */
    @TableField("status")
    @Schema(description = "状态：0=待核算，1=已核算，2=已发放")
    private Byte status;

    /**
     * 是否删除：0=未删除，1=已删除
     */
    @TableField("is_deleted")
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    /**
     * 核算时间
     */
    @Schema(description = "核算时间")
    @TableField("calculate_time")
    private LocalDateTime calculateTime;

    /**
     * 发放时间
     */
    @TableField("issue_time")
    @Schema(description = "发放时间")
    private LocalDateTime issueTime;

    /**
     * 核算财务ID
     */
    @TableField("finance_id")
    @Schema(description = "核算财务ID")
    private Integer financeId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 发放银行卡号
     */
    @TableField("bank_card_no")
    @Schema(description = "发放银行卡号")
    private String bankCardNo;

    /**
     * 财务核算备注
     */
    @TableField("remark")
    @Schema(description = "财务核算备注")
    private String remark;

    // ====== 以下为展示用关联字段（非持久化） ======

    @TableField(exist = false)
    @Schema(description = "销售姓名")
    private String salesName;

    @TableField(exist = false)
    @Schema(description = "财务核算人姓名")
    private String financeName;

    @TableField(exist = false)
    @Schema(description = "交易编号")
    private String transactionNo;
}
