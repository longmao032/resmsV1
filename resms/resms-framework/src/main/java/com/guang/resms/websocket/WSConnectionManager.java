package com.guang.resms.websocket;

import com.guang.common.security.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 连接管理器
 * compositeKey → Set&lt;WebSocketSession&gt;（支持多 tab）
 * Redis 记录在线状态 online:{userType}:{userId}，TTL=70s（由心跳刷新）
 *
 * 复合键设计：B端(sys_user) 和 C端(app_user) 的 userId 可能重复，
 * 通过 userType 前缀区分，避免连接冲突。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WSConnectionManager {

    private static final String ONLINE_KEY_PREFIX = "ws:online:";
    private static final long ONLINE_TTL_MS = 70_000;
    private static final long APP_USER_OFFSET = 1_000_000_000L;

    private final RedisService redisService;

    /** compositeKey → 该用户的所有 WS 会话（多 tab） */
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> connections = new ConcurrentHashMap<>();

    /**
     * 将 userType + userId 编码为唯一的 Long 键
     * C端 (userType="APP"): key = userId + 1_000_000_000
     * B端 (其他):           key = userId
     */
    public static long encodeKey(String userType, Long userId) {
        if ("APP".equals(userType)) {
            return userId + APP_USER_OFFSET;
        }
        return userId;
    }

    /**
     * 注册一个新的 WS 连接
     */
    public void register(String userType, Long userId, WebSocketSession session) {
        long key = encodeKey(userType, userId);
        connections.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>()).add(session);
        markOnline(userType, userId);
        log.info("WS 连接建立: userType={}, userId={}, key={}, 当前连接数={}", userType, userId, key, countConnections(key));
    }

    /**
     * 移除一个 WS 连接
     */
    public void unregister(String userType, Long userId, WebSocketSession session) {
        long key = encodeKey(userType, userId);
        Set<WebSocketSession> sessions = connections.get(key);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                connections.remove(key);
                markOffline(userType, userId);
            }
        }
        log.info("WS 连接关闭: userType={}, userId={}", userType, userId);
    }

    /**
     * 推送消息给指定用户的全部 WS 会话（多 tab）
     */
    public boolean pushToUser(String userType, Long userId, String messageJson) {
        long key = encodeKey(userType, userId);
        Set<WebSocketSession> sessions = connections.get(key);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        TextMessage msg = new TextMessage(messageJson);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(msg);
                } catch (IOException e) {
                    log.warn("WS 推送失败: userType={}, userId={}", userType, userId, e);
                }
            }
        }
        return true;
    }

    /**
     * 判断用户是否在线（有活跃 WS 连接）
     */
    public boolean isOnline(String userType, Long userId) {
        long key = encodeKey(userType, userId);
        Set<WebSocketSession> sessions = connections.get(key);
        return sessions != null && sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    /**
     * 刷新心跳：重置 Redis TTL
     */
    public void refreshHeartbeat(String userType, Long userId) {
        markOnline(userType, userId);
    }

    private void markOnline(String userType, Long userId) {
        redisService.set(ONLINE_KEY_PREFIX + userType + ":" + userId, "1", ONLINE_TTL_MS);
    }

    private void markOffline(String userType, Long userId) {
        redisService.del(ONLINE_KEY_PREFIX + userType + ":" + userId);
    }

    private int countConnections(long key) {
        Set<WebSocketSession> sessions = connections.get(key);
        return sessions == null ? 0 : sessions.size();
    }
}
