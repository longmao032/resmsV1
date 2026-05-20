package com.guang.trade.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分期付款计划参数
 */
@Data
@Schema(description = "分期付款计划参数")
public class PaymentPlanDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "期数名称 (如：第一期分期款)")
    private String payName;

    @Schema(description = "应收金额（元）")
    private BigDecimal receivableAmount;

    @Schema(description = "应收截止时间/应还日期")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueDate;
}
