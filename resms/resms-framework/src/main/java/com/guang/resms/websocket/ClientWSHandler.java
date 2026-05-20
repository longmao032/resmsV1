package com.guang.resms.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guang.resms.websocket.dto.MessageType;
import com.guang.resms.websocket.dto.WSMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

/**
 * C端 WebSocket 处理器
 * 路径：/api/ws/client
 * 与 SystemWSHandler 结构相同，读取 appUserId + userType 委托给 WSConnectionManager
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientWSHandler extends TextWebSocketHandler {

    private final WSConnectionManager connectionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        String userType = getUserType(session);
        if (userId == null) {
            closeWithError(session, "未认证");
            return;
        }
        connectionManager.register(userType, userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = getUserId(session);
        String userType = getUserType(session);
        if (userId == null) return;

        try {
            WSMessage wsMsg = objectMapper.readValue(message.getPayload(), WSMessage.class);
            if (MessageType.PING.getValue().equals(wsMsg.getType())) {
                connectionManager.refreshHeartbeat(userType, userId);
                session.sendMessage(new TextMessage(
                        objectMapper.writeValueAsString(new WSMessage(MessageType.PONG.getValue(), null))));
            }
        } catch (Exception e) {
            log.debug("WS 消息解析失败: {}", message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        String userType = getUserType(session);
        if (userId != null) {
            connectionManager.unregister(userType, userId, session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = getUserId(session);
        String userType = getUserType(session);
        log.warn("C端 WS 传输异常: userId={}", userId, exception);
        if (userId != null) {
            connectionManager.unregister(userType, userId, session);
        }
    }

    private Long getUserId(WebSocketSession session) {
        Map<String, Object> attrs = session.getAttributes();
        Object userId = attrs.get("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    private String getUserType(WebSocketSession session) {
        Map<String, Object> attrs = session.getAttributes();
        Object userType = attrs.get("userType");
        return userType instanceof String ? (String) userType : "APP";
    }

    private void closeWithError(WebSocketSession session, String reason) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION.withReason(reason));
        } catch (IOException ignored) {}
    }
}
