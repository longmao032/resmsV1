package com.guang.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@TableName("tb_view_record")
@Schema(name = "CustomerFollowUpRecord", description = "客户跟进/带看记录表")
public class CustomerFollowUpRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "跟进ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "客户ID")
    @TableField("customer_id")
    private Integer customerId;

    @Schema(description = "跟进类型：visit=实地带看, call=电话咨询, wechat=微信沟通, other=其他")
    @TableField("type")
    private String type;

    @Schema(description = "关联房源ID（带看时必填）")
    @TableField("house_id")
    private Integer houseId;

    @Schema(description = "销售ID")
    @TableField("sales_id")
    private Integer salesId;

    @Schema(description = "跟进内容/客户反馈")
    @TableField("customer_feedback")
    private String content;

    @Schema(description = "调整后意向等级（1=高，2=中，3=低），不调整则留空")
    @TableField("new_intention_level")
    private Byte newIntentionLevel;

    @Schema(description = "跟进时间")
    @TableField("view_time")
    private LocalDateTime followDate;

    @Schema(description = "状态：0=待确认，1=已预约，2=已完成，3=已取消")
    @TableField("status")
    private Byte status;

    @Schema(description = "预约方式：1=销售录入，2=客户线上申请")
    @TableField("appoint_type")
    private Byte appointType;

    @Schema(description = "跟进建议")
    @TableField("follow_advice")
    private String followAdvice;

    @Schema(description = "取消原因")
    @TableField("cancel_reason")
    private String cancelReason;

    @Schema(description = "是否删除：0=未删除，1=已删除")
    @TableField("is_deleted")
    private Byte isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
