package com.guang.trade.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "跟进记录视图对象")
public class FollowUpVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    @Schema(description = "客户ID")
    private Integer customerId;

    @Schema(description = "跟进类型：visit=实地带看, call=电话咨询, wechat=微信沟通, other=其他")
    private String type;

    @Schema(description = "关联房源ID")
    private Integer houseId;

    @Schema(description = "房源标题（冗余）")
    private String houseTitle;

    @Schema(description = "房源地址")
    private String houseAddress;

    @Schema(description = "销售ID")
    private Integer salesId;

    @Schema(description = "销售姓名")
    private String salesName;

    @Schema(description = "客户反馈")
    private String customerFeedback;

    @Schema(description = "跟进内容/销售备注")
    private String content;

    @Schema(description = "调整后意向等级（1=高，2=中，3=低）")
    private Byte newIntentionLevel;

    @Schema(description = "跟进时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime followDate;

    @Schema(description = "状态：2=已完成，3=已取消")
    private Byte status;

    @Schema(description = "跟进建议/下一步计划")
    private String followAdvice;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
