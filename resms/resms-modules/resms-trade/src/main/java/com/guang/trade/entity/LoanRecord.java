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
@TableName("tb_loan_record")
@Schema(name = "LoanRecord", description = "贷款记录表")
public class LoanRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "关联交易ID")
    @TableField("transaction_id")
    private Integer transactionId;

    @Schema(description = "贷款金额（元）")
    @TableField("loan_amount")
    private BigDecimal loanAmount;

    @Schema(description = "贷款银行")
    @TableField("bank_name")
    private String bankName;

    @Schema(description = "贷款期限（月）")
    @TableField("loan_term")
    private Integer loanTerm;

    @Schema(description = "利率")
    @TableField("interest_rate")
    private BigDecimal interestRate;

    @Schema(description = "申请时间")
    @TableField("application_time")
    private LocalDateTime applicationTime;

    @Schema(description = "审批时间")
    @TableField("approval_time")
    private LocalDateTime approvalTime;

    @Schema(description = "放款时间")
    @TableField("disbursement_time")
    private LocalDateTime disbursementTime;

    @Schema(description = "状态：0=待申请，1=审核中，2=已放款，3=未通过")
    @TableField("status")
    private Byte status;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
