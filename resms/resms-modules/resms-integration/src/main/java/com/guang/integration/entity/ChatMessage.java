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
 * 聊天消息表（多态发送者）
 * sender_type: 1=员工(sys_user), 2=C端客户(tb_app_user)
 *
 * @author blackDuck
 * @since 2026-05-11
 */
@Getter
@Setter
@ToString
@TableName("tb_chat_message")
@Schema(name = "ChatMessage", description = "聊天消息表（多态发送者）")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "消息ID")
    private Long id;

    @TableField("session_id")
    @Schema(description = "所属会话ID")
    private Long sessionId;

    @TableField("parent_id")
    @Schema(description = "回复的消息ID（引用回复）")
    private Long parentId;

    /**
     * 发送者类型：1=员工(sys_user), 2=C端客户(tb_app_user)
     * 应用层根据此字段决定去哪张表查询用户信息
     */
    @TableField("sender_type")
    @Schema(description = "发送者类型：1=员工，2=C端客户")
    private Byte senderType;

    /**
     * 发送者ID（根据 senderType 对应不同用户表）
     * 使用 bigint 兼容 tb_app_user(bigint) 和 sys_user(int)
     */
    @TableField("sender_id")
    @Schema(description = "发送者ID（根据 sender_type 对应不同用户表）")
    private Long senderId;

    /**
     * 发送者名称快照（冗余字段，避免跨表 JOIN，渲染时直接使用）
     */
    @TableField("sender_name")
    @Schema(description = "发送者名称快照")
    private String senderName;

    @TableField("sender_avatar")
    @Schema(description = "发送者头像快照")
    private String senderAvatar;

    @TableField("content")
    @Schema(description = "消息内容（文本类型时有值）")
    private String content;

    /**
     * 消息类型：1=文本，2=图片，3=文件，4=系统提示
     */
    @TableField("msg_type")
    @Schema(description = "消息类型：1=文本，2=图片，3=文件，4=系统提示")
    private Byte msgType;

    @TableField("file_url")
    @Schema(description = "文件/图片地址")
    private String fileUrl;

    @TableField("file_size")
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @TableField("file_name")
    @Schema(description = "原始文件名")
    private String fileName;

    @TableField("is_recalled")
    @Schema(description = "是否已撤回：0=正常，1=已撤回")
    private Byte isRecalled;

    @TableField("create_time")
    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}
