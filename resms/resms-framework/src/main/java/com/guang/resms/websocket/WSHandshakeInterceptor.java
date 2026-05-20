package com.guang.resms.websocket;

import com.guang.common.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket 握手拦截器 — 从 query param token 中提取 userId
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WSHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        URI uri = request.getURI();
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            log.warn("WS 握手失败: 缺少 token 参数");
            return false;
        }

        String token = null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if ("token".equals(pair[0]) && pair.length == 2) {
                token = pair[1];
                break;
            }
        }

        if (token == null || token.isEmpty()) {
            log.warn("WS 握手失败: token 为空");
            return false;
        }

        try {
            Integer userId = jwtTokenUtil.getUserIdFromToken(token);
            if (userId == null || jwtTokenUtil.isTokenExpired(token)) {
                log.warn("WS 握手失败: token 无效或已过期");
                return false;
            }
            String userType = jwtTokenUtil.getUserTypeFromToken(token);
            attributes.put("userId", userId.longValue());
            attributes.put("userType", userType != null ? userType : "ADMIN");
            return true;
        } catch (Exception e) {
            log.warn("WS 握手失败: token 解析异常", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // nothing
    }
}
