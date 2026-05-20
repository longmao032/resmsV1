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

@Getter
@Setter
@ToString
@TableName("tb_transaction_fee")
@Schema(name = "TransactionFee", description = "交易费用表")
public class TransactionFee implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "关联交易ID")
    @TableField("transaction_id")
    private Integer transactionId;

    @Schema(description = "费用类型")
    @TableField("fee_type")
    private String feeType;

    @Schema(description = "费用名称")
    @TableField("fee_name")
    private String feeName;

    @Schema(description = "金额（元）")
    @TableField("amount")
    private BigDecimal amount;

    @Schema(description = "支付方")
    @TableField("payer")
    private String payer;

    @Schema(description = "缴纳时间")
    @TableField("pay_time")
    private LocalDateTime payTime;

    @Schema(description = "缴费凭证")
    @TableField("proof_url")
    private String proofUrl;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;
}
