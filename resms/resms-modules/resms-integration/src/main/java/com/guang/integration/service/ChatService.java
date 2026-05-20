package com.guang.integration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.integration.domain.dto.CreateSessionDTO;
import com.guang.integration.domain.dto.SendMessageDTO;
import com.guang.integration.domain.vo.SessionVO;
import com.guang.integration.entity.ChatMessage;

import java.util.List;

/**
 * 聊天业务 Service 接口
 */
public interface ChatService {

    /**
     * 创建或获取已有会话（私聊时若双方已存在会话则复用）
     *
     * @param dto       请求参数（含成员列表，支持多态 userType）
     * @param callerType 调用者类型：1=员工，2=C端客户
     * @param callerId   调用者ID
     */
    SessionVO createOrGetSession(CreateSessionDTO dto, Byte callerType, Long callerId);

    /**
     * 获取当前用户的会话列表
     *
     * @param userType 1=员工，2=C端客户
     * @param userId   用户ID
     */
    List<SessionVO> listMySessions(Byte userType, Long userId);

    /**
     * 分页拉取历史消息（按时间正序）
     *
     * @param sessionId 会话ID
     * @param pageNum   页码
     * @param pageSize  每页条数
     */
    Page<ChatMessage> pageMessages(Long sessionId, int pageNum, int pageSize);

    /**
     * 发送消息
     *
     * @param dto        消息内容
     * @param senderType 1=员工，2=C端客户
     * @param senderId   发送者ID
     * @param senderName 发送者名称（快照，直接写入 DB，避免跨表查询）
     * @param senderAvatar 发送者头像快照
     */
    ChatMessage sendMessage(SendMessageDTO dto, Byte senderType, Long senderId,
                            String senderName, String senderAvatar);

    /**
     * 撤回消息（仅限发送者本人，且在 2 分钟内）
     *
     * @param messageId  消息ID
     * @param senderType 发送者类型
     * @param senderId   发送者ID
     */
    void recallMessage(Long messageId, Byte senderType, Long senderId);

    /**
     * 将指定会话的未读数清零
     *
     * @param sessionId 会话ID
     * @param userType  用户类型
     * @param userId    用户ID
     */
    void readSession(Long sessionId, Byte userType, Long userId);

    /**
     * 删除会话（软删除当前用户的成员记录，其他成员不受影响）
     *
     * @param sessionId 会话ID
     * @param userType  用户类型
     * @param userId    用户ID
     */
    void deleteSession(Long sessionId, Byte userType, Long userId);

    /**
     * 刷新用户在线状态（写入 Redis，TTL 60 秒）
     * 每次用户执行聊天相关操作时调用
     */
    void touchOnline(Byte userType, Long userId);

    /**
     * 查询用户是否在线（Redis key 是否存在）
     */
    boolean isOnline(Byte userType, Long userId);
}
