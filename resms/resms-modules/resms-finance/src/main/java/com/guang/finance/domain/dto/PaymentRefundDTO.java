package com.guang.finance.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "退款申请参数")
public class PaymentRefundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "原收款记录ID不能为空")
    @Schema(description = "原收款记录ID")
    private Integer originalPaymentId;

    @NotNull(message = "退款金额不能为空")
    @Schema(description = "退款金额（元）")
    private BigDecimal amount;

    @Schema(description = "退款方式")
    private String refundMethod;

    @Schema(description = "付款人信息")
    private String payerInfo;

    @Schema(description = "退款凭证")
    private String proofUrl;

    @Schema(description = "退款原因")
    private String remark;
}
