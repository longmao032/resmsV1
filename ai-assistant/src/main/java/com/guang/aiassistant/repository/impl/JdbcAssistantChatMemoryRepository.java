package com.guang.aiassistant.repository.impl;

import com.guang.aiassistant.repository.AssistantChatMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class JdbcAssistantChatMemoryRepository implements AssistantChatMemoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAssistantChatMemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        try {
            jdbcTemplate.update("DELETE FROM chat_memory WHERE conversation_id = ?", conversationId);
        } catch (Exception e) {
            log.error("清空 chat_memory 表中指定 conversationId={} 的聊天历史失败", conversationId, e);
            throw e;
        }
    }
}
