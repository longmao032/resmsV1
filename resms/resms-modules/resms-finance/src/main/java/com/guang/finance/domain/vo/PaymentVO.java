package com.guang.finance.domain.vo;

import com.guang.finance.entity.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付流水展示对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "支付流水展示对象")
public class PaymentVO extends Payment {

    @Schema(description = "交易编号")
    private String transactionNo;

    @Schema(description = "经办财务姓名")
    private String financeName;

    @Schema(description = "审核人姓名")
    private String auditName;
}
