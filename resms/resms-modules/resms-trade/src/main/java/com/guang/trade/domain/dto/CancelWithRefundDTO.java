package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "取消交易并退款参数")
public class CancelWithRefundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "交易ID不能为空")
    @Schema(description = "交易ID")
    private Integer id;

    @Schema(description = "取消原因")
    private String reason;
}
