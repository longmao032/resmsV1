package com.guang.aiassistant.model;

import lombok.Data;

/**
 * 聊天请求参数
 */
@Data
public class ChatRequest {
    /** 用户 ID（必填），用于标识用户、获取画像 */
    private String userId;
    /** 用户消息（必填） */
    private String message;
    /** 会话 ID（可选），为空则自动生成新会话 */
    private String sessionId;
    /** 城市（可选），用于限定房源搜索范围，如"南宁" */
    private String city;
    /** 是否开启个性化服务（可选），默认为 true。若用户不同意授权，则关闭画像功能。 */
    private Boolean isPersonaEnabled;

}
