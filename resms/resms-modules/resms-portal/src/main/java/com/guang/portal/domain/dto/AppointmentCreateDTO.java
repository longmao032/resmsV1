package com.guang.portal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "C端预约带看参数")
public class AppointmentCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "房源ID不能为空")
    @Schema(description = "房源ID")
    private Integer houseId;

    @NotNull(message = "预约时间不能为空")
    @Schema(description = "预约看房时间")
    private LocalDateTime appointmentTime;

    @Schema(description = "客户备注")
    private String remark;
}
