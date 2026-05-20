package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "交易费用保存参数")
public class FeeSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "交易ID不能为空")
    @Schema(description = "交易ID")
    private Integer transactionId;

    @NotNull(message = "费用类型不能为空")
    @Schema(description = "费用类型：deed_tax/income_tax/vat/service_fee/other")
    private String feeType;

    @Schema(description = "费用名称")
    private String feeName;

    @NotNull(message = "金额不能为空")
    @Schema(description = "金额（元）")
    private BigDecimal amount;

    @Schema(description = "支付方：buyer/seller/agent")
    private String payer;

    @Schema(description = "缴纳时间")
    private LocalDateTime payTime;

    @Schema(description = "缴费凭证")
    private String proofUrl;

    @Schema(description = "备注")
    private String remark;
}
