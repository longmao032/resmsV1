package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易保存参数
 *
 * @author blackDuck
 */
@Data
@Schema(description = "交易保存参数")
public class TransactionSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "房源ID不能为空")
    @Schema(description = "房源ID")
    private Integer houseId;

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID")
    private Integer customerId;

    @NotNull(message = "销售ID不能为空")
    @Schema(description = "销售ID")
    private Integer salesId;

    @NotNull(message = "成交价格不能为空")
    @Schema(description = "成交价格（元）")
    private BigDecimal dealPrice;

    @NotNull(message = "定金金额不能为空")
    @Schema(description = "定金金额（元）")
    private BigDecimal deposit;

    @NotNull(message = "付款方式不能为空")
    @Schema(description = "付款方式：1=一次性付款，2=分期付款，3=按揭贷款，4=租房")
    private Byte paymentType;

    @Schema(description = "分期付款计划（当 paymentType=2 时必填）")
    private java.util.List<PaymentPlanDTO> paymentPlanList;

    @Schema(description = "首付款金额（元）")
    private BigDecimal downPayment;

    @Schema(description = "贷款金额（元）")
    private BigDecimal loanAmount;

    @Schema(description = "预计下次收款时间")
    private LocalDateTime nextPaymentTime;
}
