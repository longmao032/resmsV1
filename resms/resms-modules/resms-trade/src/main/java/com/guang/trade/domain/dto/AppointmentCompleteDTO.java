package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "预约完成参数")
public class AppointmentCompleteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "客户反馈")
    private String customerFeedback;

    @Schema(description = "跟进建议/下一步计划")
    private String followAdvice;

    @Schema(description = "调整后意向等级")
    private Byte newIntentionLevel;
}
