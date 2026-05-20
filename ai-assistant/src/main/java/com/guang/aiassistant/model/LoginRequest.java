package com.guang.aiassistant.model;

/**
 * 登录请求 DTO
 */
public record LoginRequest(
        String username,
        String password
) {}
