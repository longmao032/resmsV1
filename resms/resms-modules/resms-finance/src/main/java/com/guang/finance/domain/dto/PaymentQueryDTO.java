package com.guang.finance.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 支付流水查询参数
 *
 * @author blackDuck
 */
@Data
@Schema(description = "支付流水查询参数")
public class PaymentQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "交易ID")
    private Integer transactionId;

    @Schema(description = "交易编号")
    private String transactionNo;

    @Schema(description = "款项类型")
    private Byte paymentType;

    @Schema(description = "支付状态：0=待确认，1=有效，2=已作废")
    private Byte paymentStatus;
}
