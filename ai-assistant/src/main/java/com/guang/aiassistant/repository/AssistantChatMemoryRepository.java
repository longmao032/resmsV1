package com.guang.aiassistant.repository;

public interface AssistantChatMemoryRepository {

    /**
     * 根据会话 ID 删除聊天记忆物理记录
     *
     * @param conversationId 会话ID
     */
    void deleteByConversationId(String conversationId);
}
