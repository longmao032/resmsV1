package com.guang.integration.event;

import com.guang.integration.entity.ChatMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 聊天消息发送事件
 * ChatServiceImpl 发消息后发布，由 WSMessageEventListener 推送给在线接收方
 */
@Getter
public class ChatMessageSentEvent extends ApplicationEvent {

    private final ChatMessage message;

    /** 需要推送的接收方列表（排除发送者自身） */
    private final List<Recipient> recipients;

    public ChatMessageSentEvent(Object source, ChatMessage message, List<Recipient> recipients) {
        super(source);
        this.message = message;
        this.recipients = recipients;
    }

    @Getter
    public static class Recipient {
        private final Byte userType;
        private final Long userId;

        public Recipient(Byte userType, Long userId) {
            this.userType = userType;
            this.userId = userId;
        }
    }
}
