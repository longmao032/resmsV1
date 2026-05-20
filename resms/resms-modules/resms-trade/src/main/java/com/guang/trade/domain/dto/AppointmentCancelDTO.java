package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "取消预约参数")
public class AppointmentCancelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "取消原因不能为空")
    @Schema(description = "取消原因")
    private String cancelReason;
}
