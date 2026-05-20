package com.guang.resms.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 统一消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WSMessage {
    private String type;
    private Object payload;
}
