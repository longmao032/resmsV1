package com.guang.resms.websocket.dto;

import lombok.Getter;

/**
 * WebSocket 消息类型枚举
 */
@Getter
public enum MessageType {

    /** 心跳 Ping */
    PING("PING"),
    /** 心跳 Pong */
    PONG("PONG"),
    /** 新消息推送 */
    MESSAGE("MESSAGE"),
    /** 通知推送 */
    NOTIFICATION("NOTIFICATION"),
    /** 在线状态变更 */
    ONLINE_STATUS("ONLINE_STATUS");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    public static MessageType fromValue(String value) {
        for (MessageType t : values()) {
            if (t.value.equals(value)) return t;
        }
        return null;
    }
}
