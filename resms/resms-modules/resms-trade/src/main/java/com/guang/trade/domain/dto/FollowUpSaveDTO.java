package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "跟进记录保存参数")
public class FollowUpSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID")
    private Integer customerId;

    @NotBlank(message = "跟进类型不能为空")
    @Schema(description = "跟进类型：visit=实地带看, call=电话咨询, wechat=微信沟通, other=其他")
    private String type;

    @Schema(description = "关联房源ID（带看时必填）")
    private Integer houseId;

    @Schema(description = "销售ID")
    private Integer salesId;

    @Schema(description = "客户反馈（客户原话/关注点）")
    private String customerFeedback;

    @Schema(description = "销售备注（内部工作记录）")
    private String content;

    @Schema(description = "调整后意向等级（1=高，2=中，3=低），不调整则留空")
    private Byte newIntentionLevel;

    @Schema(description = "跟进时间，默认当前时间")
    private LocalDateTime followDate;

    @Schema(description = "状态：2=已完成，3=已取消（仅 visit 类型可取消）")
    private Byte status;

    @Schema(description = "跟进建议/下一步计划")
    private String followAdvice;

    @Schema(description = "取消原因（status=3 时必填）")
    private String cancelReason;
}
