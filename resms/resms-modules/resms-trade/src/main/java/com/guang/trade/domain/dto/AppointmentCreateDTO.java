package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "预约带看创建参数")
public class AppointmentCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "预约时间不能为空")
    @Future(message = "预约时间必须是将来的时间")
    @Schema(description = "预约带看时间")
    private LocalDateTime viewTime;

    @Schema(description = "关联房源ID")
    private Integer houseId;

    @Schema(description = "客户关注点/备注")
    private String customerFeedback;

    @Schema(description = "调整后意向等级（1=高，2=中，3=低）")
    private Byte newIntentionLevel;
}
