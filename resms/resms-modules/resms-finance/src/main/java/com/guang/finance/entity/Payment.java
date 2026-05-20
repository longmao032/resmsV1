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
 * 收退款记录表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("tb_payment")
@Schema(name = "Payment", description = "收退款记录表")
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 收款ID
     */
    @Schema(description = "收款ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 交易ID
     */
    @Schema(description = "交易ID")
    @TableField("transaction_id")
    private Integer transactionId;

    /**
     * 关联的应收账单ID（用于对账核销）
     */
    @Schema(description = "关联的应收账单ID")
    @TableField("payment_plan_id")
    private Integer paymentPlanId;

    /**
     * 款项类型：1=定金，2=首付款，3=尾款，4=中介费，5=贷款
     */
    @TableField("payment_type")
    @Schema(description = "款项类型：1=定金，2=首付款，3=尾款，4=中介费，5=贷款")
    private Byte paymentType;

    /**
     * 资金流向：1=收款，2=退款
     */
    @TableField("flow_type")
    @Schema(description = "资金流向：1=收款，2=退款")
    private Byte flowType;

    /**
     * 金额（元）
     */
    @TableField("amount")
    @Schema(description = "金额（元）")
    private BigDecimal amount;

    /**
     * 状态：0=待确认，1=有效，2=已作废
     */
    @TableField("payment_status")
    @Schema(description = "状态：0=待确认，1=有效，2=已作废")
    private Byte paymentStatus;

    /**
     * 是否删除：0=未删除，1=已删除
     */
    @TableField("is_deleted")
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    /**
     * 变动时间
     */
    @TableField("payment_time")
    @Schema(description = "变动时间")
    private LocalDateTime paymentTime;

    /**
     * 支付方式
     */
    @Schema(description = "支付方式")
    @TableField("payment_method")
    private String paymentMethod;

    /**
     * 收据/发票编号
     */
    @TableField("receipt_no")
    @Schema(description = "收据/发票编号")
    private String receiptNo;

    /**
     * 凭证图片路径
     */
    @TableField("proof_url")
    @Schema(description = "凭证图片路径")
    private String proofUrl;

    /**
     * 付款人备注
     */
    @TableField("payer_info")
    @Schema(description = "付款人备注")
    private String payerInfo;

    /**
     * 经办财务ID
     */
    @TableField("finance_id")
    @Schema(description = "经办财务ID")
    private Integer financeId;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 实际到账金额
     */
    @TableField("actual_amount")
    @Schema(description = "实际到账金额")
    private BigDecimal actualAmount;

    /**
     * 财务审核时间
     */
    @TableField("audit_time")
    @Schema(description = "财务审核时间")
    private LocalDateTime auditTime;

    /**
     * 审核人ID
     */
    @TableField("audit_user_id")
    @Schema(description = "审核人ID")
    private Integer auditUserId;
}
