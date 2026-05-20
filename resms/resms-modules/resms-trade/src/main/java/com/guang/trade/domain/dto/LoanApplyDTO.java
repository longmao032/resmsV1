package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "贷款申请参数")
public class LoanApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "交易ID不能为空")
    @Schema(description = "交易ID")
    private Integer transactionId;

    @NotNull(message = "贷款金额不能为空")
    @Schema(description = "贷款金额（元）")
    private BigDecimal loanAmount;

    @NotNull(message = "贷款银行不能为空")
    @Schema(description = "贷款银行")
    private String bankName;

    @Schema(description = "贷款期限（月）")
    private Integer loanTerm;

    @Schema(description = "利率")
    private BigDecimal interestRate;

    @Schema(description = "备注")
    private String remark;
}
