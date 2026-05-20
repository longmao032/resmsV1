package com.guang.resms.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guang.common.event.AppointmentCreatedEvent;
import com.guang.resms.websocket.dto.MessageType;
import com.guang.resms.websocket.dto.WSMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 预约创建事件监听器 - 发送 WebSocket 通知给销售
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final WSConnectionManager connectionManager;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Async
    @EventListener
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("[预约通知] 监听到新预约事件: customer={}, salesId={}", event.getCustomerName(), event.getSalesId());

        // 构建通知内容
        String content = String.format("新看房预约：客户 %s 预约在 %s 查看 [%s]。请及时跟进确认。",
                event.getCustomerName(),
                event.getViewTime().format(DTF),
                event.getProjectName());

        // 构造负载数据
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "新预约提醒");
        payload.put("content", content);
        payload.put("subId", "APPOINTMENT");
        payload.put("appointmentId", event.getAppointmentId());
        payload.put("timestamp", System.currentTimeMillis());

        try {
            // 构造并序列化 WS 消息
            String messageJson = objectMapper.writeValueAsString(
                    new WSMessage(MessageType.NOTIFICATION.getValue(), payload));

            // 推送给指定的销售人员 (销售属于后台系统用户，userType="ADMIN")
            boolean pushed = connectionManager.pushToUser("ADMIN", event.getSalesId().longValue(), messageJson);
            
            if (pushed) {
                log.info("[预约通知] 已通过 WebSocket 推送至销售(id={})", event.getSalesId());
            } else {
                log.warn("[预约通知] 销售(id={}) 当前不在线，WebSocket 推送取消", event.getSalesId());
            }
        } catch (JsonProcessingException e) {
            log.error("[预约通知] 序列化消息失败", e);
        }
    }
}
