package com.guang.finance.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水提交参数
 *
 * @author blackDuck
 */
@Data
@Schema(description = "支付流水提交参数")
public class PaymentSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "交易ID不能为空")
    @Schema(description = "交易ID")
    private Integer transactionId;

    @NotNull(message = "关联账单计划ID不能为空")
    @Schema(description = "关联账单计划ID")
    private Integer paymentPlanId;

    @NotNull(message = "款项类型不能为空")
    @Schema(description = "款项类型：1=定金，2=首付款，3=尾款，4=中介费，5=贷款")
    private Byte paymentType;

    @NotNull(message = "资金流向不能为空")
    @Schema(description = "资金流向：1=收款，2=退款")
    private Byte flowType;

    @NotNull(message = "金额不能为空")
    @Schema(description = "金额（元）")
    private BigDecimal amount;

    @Schema(description = "支付方式")
    private String paymentMethod;

    @Schema(description = "凭证图片路径")
    private String proofUrl;

    @Schema(description = "支付时间")
    private LocalDateTime paymentTime;

    @Schema(description = "付款人信息")
    private String payerInfo;

    @Schema(description = "备注")
    private String remark;
}
