package com.guang.aiassistant.service;

import com.guang.aiassistant.model.ChatResult;

/**
 * 对话服务高层接口契约 — 声明对外规范，隔离实现细节。
 */
public interface ChatService {

    /**
     * 处理用户对话消息。
     *
     * @param message          用户消息文本
     * @param sessionId        会话 ID
     * @param city             请求携带的城市（可选兜底）
     * @param isPersonaEnabled 是否启用个性化画像
     * @return 对话结果（含回复文本与结构化推荐数据）
     */
    ChatResult chat(String message, String sessionId, String city, Boolean isPersonaEnabled);
}
