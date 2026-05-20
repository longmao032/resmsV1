package com.guang.resms.websocket;

import com.guang.resms.websocket.WSHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置 — 注册系统端和C端 WS 端点
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SystemWSHandler systemWSHandler;
    private final ClientWSHandler clientWSHandler;
    private final WSHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // B端管理端
        registry.addHandler(systemWSHandler, "/api/ws/system")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
        // C端客户端
        registry.addHandler(clientWSHandler, "/api/ws/client")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
