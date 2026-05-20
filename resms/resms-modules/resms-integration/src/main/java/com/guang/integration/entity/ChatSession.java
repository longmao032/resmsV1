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
 * 聊天会话主表
 *
 * @author blackDuck
 * @since 2026-05-11
 */
@Getter
@Setter
@ToString
@TableName("tb_chat_session")
@Schema(name = "ChatSession", description = "聊天会话主表")
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "会话ID")
    private Long id;

    /**
     * 会话类型：1=员工私聊，2=员工群聊，3=销售与C端客户
     */
    @TableField("session_type")
    @Schema(description = "会话类型：1=员工私聊，2=员工群聊，3=销售与C端客户")
    private Byte sessionType;

    @TableField("session_name")
    @Schema(description = "会话名称（群聊时有效）")
    private String sessionName;

    @TableField("last_message_content")
    @Schema(description = "最后一条消息内容快照")
    private String lastMessageContent;

    @TableField("last_message_type")
    @Schema(description = "最后一条消息类型")
    private Byte lastMessageType;

    @TableField("last_message_time")
    @Schema(description = "最后一条消息发送时间")
    private LocalDateTime lastMessageTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
