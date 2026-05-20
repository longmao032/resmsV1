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
 * 交易信息表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("tb_transaction")
@Schema(name = "Transaction", description = "交易信息表")
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 交易ID
     */
    @Schema(description = "交易ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 交易编号
     */
    @Schema(description = "交易编号")
    @TableField("transaction_no")
    private String transactionNo;

    /**
     * 房源ID
     */
    @TableField("house_id")
    @Schema(description = "房源ID")
    private Integer houseId;

    /**
     * 房源地址（冗余）
     */
    @TableField("house_address")
    @Schema(description = "房源地址（冗余）")
    private String houseAddress;

    /**
     * 客户ID
     */
    @TableField("customer_id")
    @Schema(description = "客户ID")
    private Integer customerId;

    /**
     * 客户姓名（冗余）
     */
    @TableField("customer_name")
    @Schema(description = "客户姓名（冗余）")
    private String customerName;

    /**
     * 客户电话（冗余）
     */
    @TableField("customer_phone")
    @Schema(description = "客户电话（冗余）")
    private String customerPhone;

    /**
     * 销售ID
     */
    @TableField("sales_id")
    @Schema(description = "销售ID")
    private Integer salesId;

    /**
     * 成交价格（元）
     */
    @TableField("deal_price")
    @Schema(description = "成交价格（元）")
    private BigDecimal dealPrice;

    /**
     * 定金金额（元）
     */
    @TableField("deposit")
    @Schema(description = "定金金额（元）")
    private BigDecimal deposit;

    /**
     * 付款方式：1=一次性付款，2=分期付款，3=按揭贷款，4=租房
     */
    @TableField("payment_type")
    @Schema(description = "付款方式：1=一次性付款，2=分期付款，3=按揭贷款，4=租房")
    private Byte paymentType;

    /**
     * 定金支付时间
     */
    @TableField("deposit_time")
    @Schema(description = "定金支付时间")
    private LocalDateTime depositTime;

    /**
     * 首付款金额（元）
     */
    @TableField("down_payment")
    @Schema(description = "首付款金额（元）")
    private BigDecimal downPayment;

    /**
     * 首付款支付时间
     */
    @Schema(description = "首付款支付时间")
    @TableField("down_payment_time")
    private LocalDateTime downPaymentTime;

    /**
     * 贷款金额（元）
     */
    @TableField("loan_amount")
    @Schema(description = "贷款金额（元）")
    private BigDecimal loanAmount;

    /**
     * 贷款状态：0=未申请，1=审核中，2=已放款，3=未通过
     */
    @TableField("loan_status")
    @Schema(description = "贷款状态：0=未申请，1=审核中，2=已放款，3=未通过")
    private Byte loanStatus;

    /**
     * 过户时间
     */
    @TableField("transfer_time")
    @Schema(description = "过户时间")
    private LocalDateTime transferTime;

    /**
     * 交易状态：
     * 买卖模式：0=待付定金，1=已付定金，2=已付首付，3=已过户，4=已完成，5=已取消
     * 租房模式：0=待付款，1=已付押金，4=已完成，5=已取消（跳过首付和过户）
     */
    @TableField("status")
    @Schema(description = "交易状态：0=待付定金(租房=待付款)，1=已付定金(租房=已付押金)，2=已付首付，3=已过户，4=已完成，5=已取消")
    private Byte status;

    /**
     * 是否删除：0=未删除，1=已删除
     */
    @TableField("is_deleted")
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    /**
     * 经理审核：0=待审核，1=已通过，2=已驳回
     */
    @TableField("manager_audit")
    @Schema(description = "经理审核：0=待审核，1=已通过，2=已驳回")
    private Byte managerAudit;

    /**
     * 完成审核：0=未申请，1=待审核，2=已通过，3=已驳回
     */
    @TableField("finish_audit")
    @Schema(description = "完成审核：0=未申请，1=待审核，2=已通过，3=已驳回")
    private Byte finishAudit;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 已收总金额
     */
    @Schema(description = "已收总金额")
    @TableField("actual_paid_amount")
    private BigDecimal actualPaidAmount;

    /**
     * 预计下次收款时间
     */
    @TableField("next_payment_time")
    @Schema(description = "预计下次收款时间")
    private LocalDateTime nextPaymentTime;
}
