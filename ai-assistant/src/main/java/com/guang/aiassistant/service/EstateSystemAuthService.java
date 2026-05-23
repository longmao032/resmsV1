package com.guang.aiassistant.service;

/**
 * 房产系统认证服务接口契约 — 声明认证管理与 Token 生命周期规范。
 */
public interface EstateSystemAuthService {

    /** 获取当前有效 token，过期自动刷新 */
    String getToken();

    /** 手动刷新 token */
    void refreshToken();
}
