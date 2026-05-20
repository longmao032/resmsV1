package com.guang.integration.entity;

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

/**
 * <p>
 * 工作通知表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("sys_notification")
@Schema(name = "Notification", description = "工作通知表")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    @Schema(description = "通知ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 通知标题
     */
    @TableField("title")
    @Schema(description = "通知标题")
    private String title;

    /**
     * 通知内容
     */
    @TableField("content")
    @Schema(description = "通知内容")
    private String content;

    /**
     * 内容格式：1=纯文本，2=HTML，3=Markdown
     */
    @TableField("content_type")
    @Schema(description = "内容格式：1=纯文本，2=HTML，3=Markdown")
    private Byte contentType;

    /**
     * 通知类型：1=系统通知，2=任务提醒，3=交易提醒，4=审批通知
     */
    @TableField("notice_type")
    @Schema(description = "通知类型：1=系统通知，2=任务提醒，3=交易提醒，4=审批通知")
    private Byte noticeType;

    /**
     * 关联业务类型
     */
    @TableField("business_type")
    @Schema(description = "关联业务类型")
    private String businessType;

    /**
     * 关联业务ID
     */
    @TableField("business_id")
    @Schema(description = "关联业务ID")
    private Integer businessId;

    /**
     * 跳转路径
     */
    @TableField("router_path")
    @Schema(description = "跳转路径")
    private String routerPath;

    /**
     * 优先级：1=紧急，2=重要，3=普通
     */
    @TableField("priority")
    @Schema(description = "优先级：1=紧急，2=重要，3=普通")
    private Byte priority;

    /**
     * 发送人ID
     */
    @TableField("sender_id")
    @Schema(description = "发送人ID")
    private Integer senderId;

    /**
     * 发送人姓名
     */
    @TableField("sender_name")
    @Schema(description = "发送人姓名")
    private String senderName;

    /**
     * 接收类型：1=指定用户，2=部门，3=角色，4=全体
     */
    @TableField("receiver_type")
    @Schema(description = "接收类型：1=指定用户，2=部门，3=角色，4=全体")
    private Byte receiverType;

    /**
     * 接收者ID列表
     */
    @TableField("receiver_ids")
    @Schema(description = "接收者ID列表")
    private String receiverIds;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    /**
     * 发送时间
     */
    @TableField("send_time")
    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    /**
     * 撤回状态：0=正常，1=已撤回
     */
    @TableField("withdraw_status")
    @Schema(description = "撤回状态：0=正常，1=已撤回")
    private Byte withdrawStatus;

    /**
     * 撤回时间
     */
    @TableField("withdraw_time")
    @Schema(description = "撤回时间")
    private LocalDateTime withdrawTime;

    /**
     * 应接收人数
     */
    @Schema(description = "应接收人数")
    @TableField("total_receiver_count")
    private Integer totalReceiverCount;

    /**
     * 已读人数
     */
    @TableField("read_count")
    @Schema(description = "已读人数")
    private Integer readCount;

    /**
     * 状态：0=草稿，1=已发送
     */
    @TableField("status")
    @Schema(description = "状态：0=草稿，1=已发送")
    private Byte status;

    /**
     * 是否删除：0=未删除，1=已删除
     */
    @TableField("is_deleted")
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    /**
     * 扩展数据
     */
    @TableField("extra_data")
    @Schema(description = "扩展数据")
    private String extraData;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
