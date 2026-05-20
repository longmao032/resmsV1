package com.guang.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.common.exception.ApiException;
import com.guang.integration.domain.dto.CreateSessionDTO;
import com.guang.integration.domain.dto.SendMessageDTO;
import com.guang.integration.domain.vo.SessionVO;
import com.guang.integration.entity.ChatMessage;
import com.guang.integration.entity.ChatSession;
import com.guang.integration.entity.ChatSessionMember;
import com.guang.integration.event.ChatMessageSentEvent;
import com.guang.integration.mapper.ChatMessageMapper;
import com.guang.integration.mapper.ChatSessionMapper;
import com.guang.integration.mapper.ChatSessionMemberMapper;
import com.guang.integration.service.ChatService;
import com.guang.common.security.RedisService;
import com.guang.system.entity.User;
import com.guang.system.mapper.UserMapper;
import com.guang.trade.entity.AppUser;
import com.guang.trade.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天业务 Service 实现
 * 多态设计：通过 senderType/userType (1=员工, 2=C端客户) 区分用户来源，
 * 移除强外键依赖，应用层保证数据完整性。
 *
 * @author blackDuck
 * @since 2026-05-11
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final AppUserMapper appUserMapper;
    private final RedisService redisService;
    private final ApplicationEventPublisher eventPublisher;

    // 消息撤回时间窗口（秒）
    private static final int RECALL_WINDOW_SECONDS = 120;

    // 在线状态 Redis key 前缀及 TTL（毫秒）
    private static final String ONLINE_KEY_PREFIX = "ws:online:";
    private static final long ONLINE_TTL_MS = 70_000; // 70 秒，与 WSConnectionManager 保持一致

    // ==========================================================
    // 1. 创建或获取会话
    // ==========================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SessionVO createOrGetSession(CreateSessionDTO dto, Byte callerType, Long callerId) {
        touchOnline(callerType, callerId);
        // 私聊时（员工私聊或销售与C端客户）：如果双方已存在会话则直接复用
        if ((dto.getSessionType() == 1 || dto.getSessionType() == 3) && dto.getMembers().size() == 2) {
            ChatSession existing = findExistingPrivateSession(dto.getMembers());
            if (existing != null) {
                // 如果调用者之前删除了该会话，恢复其成员状态
                restoreIfDeleted(existing.getId(), callerType, callerId);
                // 刷新 C 端成员快照（处理老会话中 userAvatar/userName 为 null 的情况）
                refreshMemberSnapshots(existing.getId(), dto.getMembers());
                return buildSessionVO(existing, callerType, callerId);
            }
        }

        // 创建新会话
        ChatSession session = new ChatSession();
        session.setSessionType(dto.getSessionType());
        session.setSessionName(dto.getSessionName());
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);

        // 批量插入成员（多态：不再区分是否 sys_user）
        List<ChatSessionMember> members = dto.getMembers().stream().map(m -> {
            ChatSessionMember member = new ChatSessionMember();
            member.setSessionId(session.getId());
            member.setUserType(m.getUserType());
            member.setUserId(m.getUserId());
            
            // 填充快照信息 (1=员工)
            if (m.getUserType() == 1) {
                User user = userMapper.selectById(m.getUserId());
                if (user != null) {
                    String showName = user.getNickName();
                    if (showName == null || showName.isBlank()) showName = user.getRealName();
                    if (showName == null || showName.isBlank()) showName = "用户" + user.getId();

                    member.setUserName(showName);
                    member.setUserAvatar(user.getAvatar());
                }
            } else if (m.getUserType() == 2) {
                AppUser appUser = appUserMapper.selectById(m.getUserId());
                if (appUser != null) {
                    String showName = appUser.getNickname();
                    if (showName == null || showName.isBlank()) showName = "用户" + appUser.getId();
                    member.setUserName(showName);
                    member.setUserAvatar(appUser.getAvatarUrl());
                }
            }
            
            member.setUnreadCount(0);
            member.setIsTop((byte) 0);
            member.setIsDisturb((byte) 0);
            member.setJoinTime(LocalDateTime.now());
            return member;
        }).collect(Collectors.toList());

        // 批量插入
        for (ChatSessionMember member : members) {
            memberMapper.insert(member);
        }

        return buildSessionVO(session, callerType, callerId);
    }

    // ==========================================================
    // 2. 获取当前用户的会话列表
    // ==========================================================

    @Override
    public List<SessionVO> listMySessions(Byte userType, Long userId) {
        touchOnline(userType, userId);
        // 查询该用户参与的所有会话成员记录
        List<ChatSessionMember> myMemberships = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getUserType, userType)
                        .eq(ChatSessionMember::getUserId, userId)
                        .eq(ChatSessionMember::getIsDeleted, 0)
                        .orderByDesc(ChatSessionMember::getUpdateTime)
        );

        if (myMemberships.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> sessionIds = myMemberships.stream()
                .map(ChatSessionMember::getSessionId)
                .collect(Collectors.toList());

        // 批量查询会话主表
        List<ChatSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .in(ChatSession::getId, sessionIds)
                        .orderByDesc(ChatSession::getLastMessageTime)
        );

        // 批量查询所有成员（用于构建头像/昵称快照）
        List<ChatSessionMember> allMembers = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .in(ChatSessionMember::getSessionId, sessionIds)
        );
        Map<Long, List<ChatSessionMember>> memberMap = allMembers.stream()
                .collect(Collectors.groupingBy(ChatSessionMember::getSessionId));

        // 构建我的成员记录索引（用于取未读数、置顶等个人配置）
        Map<Long, ChatSessionMember> myMemberMap = myMemberships.stream()
                .collect(Collectors.toMap(ChatSessionMember::getSessionId, m -> m));

        return sessions.stream().map(session -> {
            SessionVO vo = new SessionVO();
            vo.setId(session.getId());
            vo.setSessionType(session.getSessionType());
            vo.setSessionName(session.getSessionName());
            vo.setLastMessageContent(session.getLastMessageContent());
            vo.setLastMessageTime(session.getLastMessageTime());

            ChatSessionMember myMember = myMemberMap.get(session.getId());
            if (myMember != null) {
                vo.setUnreadCount(myMember.getUnreadCount());
                vo.setIsTop(myMember.getIsTop() == 1);
            }

            // 填充成员快照（排除自己）
            List<SessionVO.MemberVO> memberVOs = memberMap.getOrDefault(session.getId(), new ArrayList<>())
                    .stream()
                    .filter(m -> !(m.getUserType().equals(userType) && m.getUserId().equals(userId)))
                    .map(m -> {
                        SessionVO.MemberVO mv = new SessionVO.MemberVO();
                        mv.setUserType(m.getUserType());
                        mv.setUserId(m.getUserId());
                        mv.setUserName(m.getUserName());
                        mv.setUserAvatar(m.getUserAvatar());
                        return mv;
                    }).collect(Collectors.toList());
            vo.setMembers(memberVOs);

            return vo;
        }).collect(Collectors.toList());
    }

    // ==========================================================
    // 3. 分页拉取历史消息
    // ==========================================================

    @Override
    public Page<ChatMessage> pageMessages(Long sessionId, int pageNum, int pageSize) {
        Page<ChatMessage> page = new Page<>(pageNum, pageSize);
        return messageMapper.selectPage(page,
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getIsRecalled, 0)
                        .orderByDesc(ChatMessage::getCreateTime)
        );
    }

    // ==========================================================
    // 4. 发送消息
    // ==========================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage sendMessage(SendMessageDTO dto, Byte senderType, Long senderId,
                                   String senderName, String senderAvatar) {
        // 刷新发送者在线状态
        touchOnline(senderType, senderId);

        // 校验发送者是否是会话成员
        long memberCount = memberMapper.selectCount(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, dto.getSessionId())
                        .eq(ChatSessionMember::getUserType, senderType)
                        .eq(ChatSessionMember::getUserId, senderId)
        );
        if (memberCount == 0) {
            throw new ApiException("您不是该会话的成员");
        }

        // 文本消息必须有内容
        if (dto.getMsgType() == 1 && (dto.getContent() == null || dto.getContent().isBlank())) {
            throw new ApiException("消息内容不能为空");
        }

        // 快照兜底：如果调用方未传 senderName/senderAvatar（如 LoginUser 缓存过期），从 DB 直查
        if (senderType == 2 && (senderName == null || senderAvatar == null)) {
            AppUser appUser = appUserMapper.selectById(senderId);
            if (appUser != null) {
                if (senderName == null) {
                    senderName = appUser.getNickname();
                    if (senderName == null || senderName.isBlank()) senderName = "用户" + appUser.getId();
                }
                if (senderAvatar == null) {
                    senderAvatar = appUser.getAvatarUrl();
                }
            }
        }

        // 保存消息（senderName/senderAvatar 为快照字段，直接写入，避免后续查询跨库）
        ChatMessage message = new ChatMessage();
        message.setSessionId(dto.getSessionId());
        message.setParentId(dto.getParentId());
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setSenderAvatar(senderAvatar);
        message.setContent(dto.getContent());
        message.setMsgType(dto.getMsgType());
        message.setFileUrl(dto.getFileUrl());
        message.setFileSize(dto.getFileSize());
        message.setFileName(dto.getFileName());
        message.setIsRecalled((byte) 0);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);

        // 更新会话快照并对其他成员的未读数 +1
        updateSessionSnapshot(dto.getSessionId(), message, senderType, senderId);

        // 发布事件，由 WebSocket 监听器推送给在线接收方
        publishMessageEvent(message, senderType, senderId);

        return message;
    }

    /**
     * 发布消息发送事件，触发 WebSocket 推送
     */
    private void publishMessageEvent(ChatMessage message, Byte senderType, Long senderId) {
        // 查询该会话的所有成员（排除发送者）
        List<ChatSessionMember> allMembers = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, message.getSessionId())
        );
        List<ChatMessageSentEvent.Recipient> recipients = allMembers.stream()
                .filter(m -> !(m.getUserType().equals(senderType) && m.getUserId().equals(senderId)))
                .map(m -> new ChatMessageSentEvent.Recipient(m.getUserType(), m.getUserId()))
                .collect(Collectors.toList());

        if (!recipients.isEmpty()) {
            eventPublisher.publishEvent(new ChatMessageSentEvent(this, message, recipients));
        }
    }

    // ==========================================================
    // 5. 撤回消息
    // ==========================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recallMessage(Long messageId, Byte senderType, Long senderId) {
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new ApiException("消息不存在");
        }
        // 校验发送者身份
        if (!message.getSenderType().equals(senderType) || !message.getSenderId().equals(senderId)) {
            throw new ApiException("只能撤回自己发送的消息");
        }
        // 校验时间窗口
        if (message.getCreateTime().isBefore(LocalDateTime.now().minusSeconds(RECALL_WINDOW_SECONDS))) {
            throw new ApiException("超过 2 分钟的消息无法撤回");
        }

        messageMapper.update(null,
                new LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getId, messageId)
                        .set(ChatMessage::getIsRecalled, 1)
        );
    }

    // ==========================================================
    // 6. 清零未读数
    // ==========================================================

    @Override
    public void readSession(Long sessionId, Byte userType, Long userId) {
        touchOnline(userType, userId);
        memberMapper.update(null,
                new LambdaUpdateWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, sessionId)
                        .eq(ChatSessionMember::getUserType, userType)
                        .eq(ChatSessionMember::getUserId, userId)
                        .set(ChatSessionMember::getUnreadCount, 0)
        );
    }

    // ==========================================================
    // 7. 删除会话（软删除当前用户的成员记录）
    // ==========================================================

    @Override
    public void deleteSession(Long sessionId, Byte userType, Long userId) {
        memberMapper.update(null,
                new LambdaUpdateWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, sessionId)
                        .eq(ChatSessionMember::getUserType, userType)
                        .eq(ChatSessionMember::getUserId, userId)
                        .set(ChatSessionMember::getIsDeleted, 1)
                        .set(ChatSessionMember::getUnreadCount, 0)
        );
    }

    // ==========================================================
    // 私有辅助方法
    // ==========================================================

    /**
     * 私聊场景：查找两人之间是否已存在会话
     * 策略：找出双方都参与的私聊会话（sessionType = 1 或 3）
     */
    private ChatSession findExistingPrivateSession(List<CreateSessionDTO.MemberDTO> members) {
        if (members.size() != 2) return null;

        CreateSessionDTO.MemberDTO m1 = members.get(0);
        CreateSessionDTO.MemberDTO m2 = members.get(1);

        // 查 m1 的所有会话
        List<ChatSessionMember> m1Sessions = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getUserType, m1.getUserType())
                        .eq(ChatSessionMember::getUserId, m1.getUserId())
        );
        List<Long> m1SessionIds = m1Sessions.stream()
                .map(ChatSessionMember::getSessionId)
                .collect(Collectors.toList());

        if (m1SessionIds.isEmpty()) return null;

        // 在 m1 的会话中，找 m2 也参与的
        List<ChatSessionMember> shared = memberMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .in(ChatSessionMember::getSessionId, m1SessionIds)
                        .eq(ChatSessionMember::getUserType, m2.getUserType())
                        .eq(ChatSessionMember::getUserId, m2.getUserId())
        );

        if (shared.isEmpty()) return null;

        // 过滤出私聊类型会话（sessionType = 1 或 3）
        List<Long> sharedSessionIds = shared.stream()
                .map(ChatSessionMember::getSessionId)
                .collect(Collectors.toList());

        return sessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .in(ChatSession::getId, sharedSessionIds)
                        .in(ChatSession::getSessionType, 1, 3)
                        .last("LIMIT 1")
        );
    }

    /**
     * 更新会话的最后消息快照，并对除发送者外的所有成员未读数 +1
     */
    private void updateSessionSnapshot(Long sessionId, ChatMessage message,
                                       Byte senderType, Long senderId) {
        // 更新会话快照
        sessionMapper.update(null,
                new LambdaUpdateWrapper<ChatSession>()
                        .eq(ChatSession::getId, sessionId)
                        .set(ChatSession::getLastMessageContent, message.getContent())
                        .set(ChatSession::getLastMessageType, message.getMsgType())
                        .set(ChatSession::getLastMessageTime, message.getCreateTime())
                        .set(ChatSession::getUpdateTime, LocalDateTime.now())
        );

        // 对其他成员未读数 +1（使用 SQL 自增，避免并发读写冲突）
        memberMapper.update(null,
                new LambdaUpdateWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, sessionId)
                        // 排除发送者自身（多态条件：类型和 ID 都不匹配时才 +1）
                        .and(w -> w.ne(ChatSessionMember::getUserType, senderType)
                                .or()
                                .ne(ChatSessionMember::getUserId, senderId))
                        .setSql("unread_count = unread_count + 1")
        );
    }

    private SessionVO buildSessionVO(ChatSession session, Byte userType, Long userId) {
        SessionVO vo = new SessionVO();
        vo.setId(session.getId());
        vo.setSessionType(session.getSessionType());
        vo.setSessionName(session.getSessionName());
        vo.setLastMessageContent(session.getLastMessageContent());
        vo.setLastMessageTime(session.getLastMessageTime());

        ChatSessionMember myMember = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, session.getId())
                        .eq(ChatSessionMember::getUserType, userType)
                        .eq(ChatSessionMember::getUserId, userId)
        );
        if (myMember != null) {
            vo.setUnreadCount(myMember.getUnreadCount());
            vo.setIsTop(myMember.getIsTop() == 1);
        }
        return vo;
    }

    /**
     * 如果调用者曾删除过该会话（is_deleted = 1），恢复其成员状态
     */
    private void restoreIfDeleted(Long sessionId, Byte userType, Long userId) {
        memberMapper.update(null,
                new LambdaUpdateWrapper<ChatSessionMember>()
                        .eq(ChatSessionMember::getSessionId, sessionId)
                        .eq(ChatSessionMember::getUserType, userType)
                        .eq(ChatSessionMember::getUserId, userId)
                        .eq(ChatSessionMember::getIsDeleted, 1)
                        .set(ChatSessionMember::getIsDeleted, 0)
                        .set(ChatSessionMember::getUnreadCount, 0)
        );
    }

    /**
     * 刷新 C 端成员快照（仅当现有快照为 null 时补充）
     * 解决老会话/复用会话中 userAvatar/userName 缺失的问题
     */
    private void refreshMemberSnapshots(Long sessionId, List<CreateSessionDTO.MemberDTO> members) {
        for (CreateSessionDTO.MemberDTO m : members) {
            if (m.getUserType() != 2) continue;

            ChatSessionMember existing = memberMapper.selectOne(
                    new LambdaQueryWrapper<ChatSessionMember>()
                            .eq(ChatSessionMember::getSessionId, sessionId)
                            .eq(ChatSessionMember::getUserType, m.getUserType())
                            .eq(ChatSessionMember::getUserId, m.getUserId())
            );
            if (existing == null || existing.getUserAvatar() != null) continue;

            AppUser appUser = appUserMapper.selectById(m.getUserId());
            if (appUser != null) {
                String showName = appUser.getNickname();
                if (showName == null || showName.isBlank()) showName = "用户" + appUser.getId();
                memberMapper.update(null,
                        new LambdaUpdateWrapper<ChatSessionMember>()
                                .eq(ChatSessionMember::getId, existing.getId())
                                .set(ChatSessionMember::getUserName, showName)
                                .set(ChatSessionMember::getUserAvatar, appUser.getAvatarUrl())
                );
            }
        }
    }

    // ==========================================================
    // 8. 在线状态（Redis Presence）
    // ==========================================================

    @Override
    public void touchOnline(Byte userType, Long userId) {
        String key = ONLINE_KEY_PREFIX + userType + ":" + userId;
        redisService.set(key, "1", ONLINE_TTL_MS);
    }

    @Override
    public boolean isOnline(Byte userType, Long userId) {
        String key = ONLINE_KEY_PREFIX + userType + ":" + userId;
        return redisService.hasKey(key);
    }
}
