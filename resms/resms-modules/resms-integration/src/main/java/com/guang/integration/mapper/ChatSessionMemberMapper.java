package com.guang.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.integration.entity.ChatSessionMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话成员 Mapper
 */
@Mapper
public interface ChatSessionMemberMapper extends BaseMapper<ChatSessionMember> {
}
