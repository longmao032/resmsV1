package com.guang.integration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话成员关联表（多态用户）
 * user_type: 1=员工(sys_user), 2=C端客户(tb_app_user)
 *
 * @author blackDuck
 * @since 2026-05-11
 */
@Getter
@Setter
@ToString
@TableName("tb_chat_session_member")
@Schema(name = "ChatSessionMember", description = "会话成员关联表（多态用户）")
public class ChatSessionMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    @Schema(description = "会话ID")
    private Long sessionId;

    /**
     * 用户类型：1=员工(sys_user), 2=C端客户(tb_app_user)
     * 与 userId 组合唯一标识一个成员，避免不同表的 ID 碰撞
     */
    @TableField("user_type")
    @Schema(description = "用户类型：1=员工，2=C端客户")
    private Byte userType;

    /**
     * 用户ID（bigint 以兼容 tb_app_user 的 bigint 主键）
     */
    @TableField("user_id")
    @Schema(description = "用户ID（根据 user_type 对应不同用户表）")
    private Long userId;

    @TableField("user_name")
    @Schema(description = "用户名称快照")
    private String userName;

    @TableField("user_avatar")
    @Schema(description = "用户头像快照")
    private String userAvatar;

    @TableField("unread_count")
    @Schema(description = "未读消息数")
    private Integer unreadCount;

    @TableField("is_top")
    @Schema(description = "是否置顶：0=否，1=是")
    private Byte isTop;

    @TableField("is_disturb")
    @Schema(description = "消息免打扰：0=否，1=是")
    private Byte isDisturb;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    @TableField("join_time")
    @Schema(description = "加入时间")
    private LocalDateTime joinTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
