package com.guang.resms.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guang.common.event.NotificationPublishedEvent;
import com.guang.resms.websocket.dto.MessageType;
import com.guang.resms.websocket.dto.WSMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知发布事件监听器 — WebSocket 实时推送给在线接收人
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketListener {

    private final WSConnectionManager connectionManager;
    private final ObjectMapper objectMapper;

    @Async
    @EventListener
    public void handleNotificationPublished(NotificationPublishedEvent event) {
        log.info("[通知推送] 通知(id={}) 发布，目标 {} 位用户", event.getNotificationId(),
                event.getTargetUserIds().size());

        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", event.getNotificationId());
        payload.put("title", event.getTitle());
        payload.put("content", event.getContent() != null && event.getContent().length() > 100
                ? event.getContent().substring(0, 100) + "..." : event.getContent());
        payload.put("noticeType", event.getNoticeType());
        payload.put("timestamp", System.currentTimeMillis());

        try {
            String messageJson = objectMapper.writeValueAsString(
                    new WSMessage(MessageType.NOTIFICATION.getValue(), payload));

            int pushed = 0;
            for (Integer userId : event.getTargetUserIds()) {
                if (connectionManager.pushToUser("ADMIN", userId.longValue(), messageJson)) {
                    pushed++;
                }
            }
            log.info("[通知推送] 通知(id={}) WebSocket 推送完成: {}/{} 在线", event.getNotificationId(),
                    pushed, event.getTargetUserIds().size());
        } catch (JsonProcessingException e) {
            log.error("[通知推送] 序列化消息失败", e);
        }
    }
}
