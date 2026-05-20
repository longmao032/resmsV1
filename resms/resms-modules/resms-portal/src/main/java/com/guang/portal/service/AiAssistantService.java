package com.guang.portal.service;

import com.guang.portal.domain.vo.AiChatVO;

public interface AiAssistantService {
    AiChatVO chat(String userId, String message, String sessionId, String city);
}
