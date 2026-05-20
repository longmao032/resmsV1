package com.guang.resms.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guang.integration.event.ChatMessageSentEvent;
import com.guang.resms.websocket.dto.MessageType;
import com.guang.resms.websocket.dto.WSMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 聊天消息事件监听器
 * 收到 ChatMessageSentEvent 后，通过 WebSocket 推送给在线接收方
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageEventListener {

    private final WSConnectionManager connectionManager;
    private final ObjectMapper objectMapper;

    @EventListener
    public void onMessageSent(ChatMessageSentEvent event) {
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(
                    new WSMessage(MessageType.MESSAGE.getValue(), event.getMessage()));
        } catch (JsonProcessingException e) {
            log.error("序列化 WS 消息失败", e);
            return;
        }

        for (ChatMessageSentEvent.Recipient recipient : event.getRecipients()) {
            String userType = recipient.getUserType() == 1 ? "ADMIN" : "APP";
            boolean pushed = connectionManager.pushToUser(userType, recipient.getUserId(), messageJson);
            log.debug("WS 推送消息: sessionId={}, 接收方={}, userType={}, 在线={}",
                    event.getMessage().getSessionId(), recipient.getUserId(), userType, pushed);
        }
    }
}
