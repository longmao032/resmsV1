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
 * 用户通知关联表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("sys_user_notification")
@Schema(name = "UserNotification", description = "用户通知关联表")
public class UserNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 通知ID
     */
    @Schema(description = "通知ID")
    @TableField("notification_id")
    private Integer notificationId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @Schema(description = "用户ID")
    private Integer userId;

    /**
     * 是否已读：0=未读，1=已读
     */
    @TableField("is_read")
    @Schema(description = "是否已读：0=未读，1=已读")
    private Byte isRead;

    /**
     * 阅读时间
     */
    @TableField("read_time")
    @Schema(description = "阅读时间")
    private LocalDateTime readTime;

    /**
     * 是否删除：0=未删除，1=已删除
     */
    @TableField("is_deleted")
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;
}
