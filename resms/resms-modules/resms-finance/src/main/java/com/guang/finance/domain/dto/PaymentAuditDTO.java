package com.guang.finance.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付流水审核参数
 *
 * @author blackDuck
 */
@Data
@Schema(description = "支付流水审核参数")
public class PaymentAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "记录ID不能为空")
    @Schema(description = "流水ID")
    private Integer id;

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "状态：1=通过，2=作废")
    private Byte paymentStatus;

    @Schema(description = "实际到账金额")
    private BigDecimal actualAmount;

    @Schema(description = "审核备注")
    private String remark;
}
