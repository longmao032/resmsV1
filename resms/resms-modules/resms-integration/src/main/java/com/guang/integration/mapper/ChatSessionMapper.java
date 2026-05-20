package com.guang.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.integration.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天会话 Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
