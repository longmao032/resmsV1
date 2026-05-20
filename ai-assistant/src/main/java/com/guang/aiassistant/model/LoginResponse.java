package com.guang.aiassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 登录响应 DTO — 兼容多种返回格式
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginResponse(
        String token,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("expiresAt") Long expiresAt,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String token,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("expiresAt") Long expiresAt
    ) {}

    /**
     * 从各层嵌套中提取 token
     */
    public String resolveToken() {
        if (token != null && !token.isBlank()) return token;
        if (data != null && data.token() != null && !data.token().isBlank()) return data.token();
        return null;
    }

    /**
     * 提取过期时间（秒）
     */
    public long resolveExpiresIn() {
        if (expiresIn != null) return expiresIn;
        if (data != null && data.expiresIn() != null) return data.expiresIn();
        return 86400; // 默认 24 小时
    }
}
