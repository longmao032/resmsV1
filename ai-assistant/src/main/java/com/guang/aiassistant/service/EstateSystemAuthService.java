package com.guang.aiassistant.service;

import com.guang.aiassistant.model.LoginRequest;
import com.guang.aiassistant.model.LoginResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 房产系统认证服务 — 启动时自动登录获取 token，缓存并提供给其他 API 调用方
 */
@Service
public class EstateSystemAuthService {

    private static final Logger log = LoggerFactory.getLogger(EstateSystemAuthService.class);

    private final RestClient restClient;
    private final String loginUrl;
    private final String username;
    private final String password;

    private final AtomicReference<String> token = new AtomicReference<>();
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public EstateSystemAuthService(
            @Value("${remote.estate-system.target-url}") String baseUrl,
            @Value("${remote.estate-system.login-url}") String loginUrl,
            @Value("${remote.estate-system.target-user-name}") String username,
            @Value("${remote.estate-system.target-password}") String password
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.loginUrl = loginUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * 启动时自动登录
     */
    @PostConstruct
    public void init() {
        log.info("正在登录房产系统...");
        refreshToken();
        log.info("房产系统登录成功，token 已缓存");
    }

    /**
     * 获取当前 token，如果已过期则自动刷新
     */
    public String getToken() {
        if (Instant.now().isAfter(tokenExpiresAt)) {
            log.info("Token 已过期，重新登录...");
            refreshToken();
        }
        return token.get();
    }

    /**
     * 手动刷新 token
     */
    public synchronized void refreshToken() {
        LoginRequest loginRequest = new LoginRequest(username, password);

        log.debug("发送登录请求: POST {}  username={}", loginUrl, username);

        LoginResponse response = restClient.post()
                .uri(loginUrl)
                .body(loginRequest)
                .retrieve()
                .body(LoginResponse.class);

        if (response == null) {
            throw new RuntimeException("登录失败：响应为空");
        }

        String newToken = response.resolveToken();
        if (newToken == null) {
            throw new RuntimeException("登录失败：响应中未找到 token，响应: " + response);
        }

        token.set(newToken);
        long expiresIn = response.resolveExpiresIn();
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn - 60); // 提前 60 秒过期

        log.info("Token 已更新，有效期 {} 秒", expiresIn);
    }
}
