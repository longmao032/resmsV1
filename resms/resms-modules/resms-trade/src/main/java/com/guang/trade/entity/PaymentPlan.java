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
 * 交易付款应收账单计划表
 * </p>
 *
 * @author antigravity
 * @since 2026-05-10
 */
@Getter
@Setter
@ToString
@TableName("tb_payment_plan")
@Schema(name = "PaymentPlan", description = "交易付款应收账单计划表")
public class PaymentPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账单ID
     */
    @Schema(description = "账单ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 关联交易ID
     */
    @Schema(description = "关联交易ID")
    @TableField("transaction_id")
    private Integer transactionId;

    /**
     * 期数/阶段 (0:定金, 1:首期/第一期, 2:第二期, 3:尾款/按揭款...)
     */
    @Schema(description = "期数/阶段 (0:定金, 1:首期/第一期, 2:第二期, 3:尾款/按揭款...)")
    @TableField("stage")
    private Integer stage;

    /**
     * 款项名称 (如：定金、首付款、第二期分期款、银行按揭贷款)
     */
    @Schema(description = "款项名称")
    @TableField("pay_name")
    private String payName;

    /**
     * 应收金额（元）
     */
    @Schema(description = "应收金额（元）")
    @TableField("receivable_amount")
    private BigDecimal receivableAmount;

    /**
     * 已收金额（元）
     */
    @Schema(description = "已收金额（元）")
    @TableField("paid_amount")
    private BigDecimal paidAmount;

    /**
     * 应收截止时间/应还日期
     */
    @Schema(description = "应收截止时间/应还日期")
    @TableField("due_date")
    private LocalDateTime dueDate;

    /**
     * 账单状态：0=待付款，1=部分付款，2=已结清，3=已取消
     */
    @Schema(description = "账单状态：0=待付款，1=部分付款，2=已结清，3=已取消")
    @TableField("status")
    private Byte status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
