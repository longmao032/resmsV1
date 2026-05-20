package com.guang.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.integration.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
