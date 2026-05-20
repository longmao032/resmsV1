package com.guang.integration.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.common.event.NotificationPublishedEvent;
import com.guang.integration.entity.Notification;
import com.guang.integration.mapper.NotificationMapper;
import com.guang.integration.service.NotificationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 工作通知表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.exception.ApiException;
import com.guang.common.security.LoginUser;
import com.guang.common.util.SecurityUtils;
import com.guang.integration.domain.dto.NotificationSaveDTO;
import com.guang.integration.domain.vo.NotificationVO;
import com.guang.integration.entity.UserNotification;
import com.guang.integration.service.UserNotificationService;
import com.guang.system.entity.User;
import com.guang.system.entity.UserRole;
import com.guang.system.mapper.UserMapper;
import com.guang.system.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 工作通知表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final UserNotificationService userNotificationService;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean publishNotification(NotificationSaveDTO saveDTO) {
        Notification notification = BeanUtil.copyProperties(saveDTO, Notification.class);
        Integer userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        notification.setSenderId(userId != null ? userId : 8);
        notification.setSenderName(username != null ? username : "系统");
        notification.setSendTime(LocalDateTime.now());
        notification.setStatus((byte) 1); // 已发送
        notification.setWithdrawStatus((byte) 0);
        notification.setIsDeleted((byte) 0);
        notification.setReadCount(0);

        // 非管理员限制 receiver 范围
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null && !loginUser.isAppUser()) {
            Byte dataScope = loginUser.getDataScope();
            Integer userDeptId = loginUser.getDeptId();
            if (dataScope != null && dataScope != 1) {
                if (saveDTO.getReceiverType() == 4) {
                    throw new ApiException("无权限发送全体公告");
                }
                if (saveDTO.getReceiverType() == 2) {
                    if (CollUtil.isEmpty(saveDTO.getReceiverIds())
                            || !saveDTO.getReceiverIds().contains(userDeptId)) {
                        throw new ApiException("只能向所在部门发送公告");
                    }
                    saveDTO.setReceiverIds(List.of(userDeptId));
                }
                if (saveDTO.getReceiverType() == 1) {
                    List<User> users = userMapper.selectBatchIds(saveDTO.getReceiverIds());
                    for (User u : users) {
                        if (!userDeptId.equals(u.getDeptId())) {
                            throw new ApiException("只能向本部门用户发送公告");
                        }
                    }
                }
            }
        }

        // 1. 获取接收者 ID 列表
        Set<Integer> targetUserIds = getTargetUserIds(saveDTO);
        notification.setTotalReceiverCount(targetUserIds.size());
        if (CollUtil.isNotEmpty(saveDTO.getReceiverIds())) {
            notification.setReceiverIds(CollUtil.join(saveDTO.getReceiverIds(), ","));
        }

        // 2. 保存通知主体
        boolean success = this.save(notification);
        if (!success) return false;

        // 3. 分发通知给每个用户 (sys_user_notification)
        if (CollUtil.isNotEmpty(targetUserIds)) {
            List<UserNotification> userNotifications = targetUserIds.stream().map(uId -> {
                UserNotification un = new UserNotification();
                un.setNotificationId(notification.getId());
                un.setUserId(uId);
                un.setIsRead((byte) 0);
                un.setIsDeleted((byte) 0);
                un.setCreateTime(LocalDateTime.now());
                return un;
            }).collect(Collectors.toList());
            userNotificationService.saveBatch(userNotifications);
        }

        // 4. 发布事件 → WebSocket 实时推送给在线用户
        eventPublisher.publishEvent(new NotificationPublishedEvent(
                this, notification.getId(), notification.getTitle(),
                notification.getContent(), notification.getNoticeType(), targetUserIds));

        return true;
    }

    private Set<Integer> getTargetUserIds(NotificationSaveDTO saveDTO) {
        Byte receiverType = saveDTO.getReceiverType();
        if (receiverType == 1) { // 指定用户
            return CollUtil.newHashSet(saveDTO.getReceiverIds());
        } else if (receiverType == 2) { // 指定部门
            // 需要调用部门下的用户逻辑，此处简化为查询该部门下所有用户
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .in(User::getDeptId, saveDTO.getReceiverIds()));
            return users.stream().map(User::getId).collect(Collectors.toSet());
        } else if (receiverType == 3) { // 指定角色
            List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                    .in(UserRole::getRoleId, saveDTO.getReceiverIds()));
            return userRoles.stream().map(UserRole::getUserId).collect(Collectors.toSet());
        } else if (receiverType == 4) { // 全体用户
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getIsDeleted, 0));
            return users.stream().map(User::getId).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean withdrawNotification(Integer id) {
        Notification notification = this.getById(id);
        if (notification == null) return false;
        
        notification.setWithdrawStatus((byte) 1);
        notification.setWithdrawTime(LocalDateTime.now());
        boolean success = this.updateById(notification);
        
        // 撤回后，用户端的记录也可以选择删除或标记，此处标记关联记录为已删除
        if (success) {
            userNotificationService.update(new LambdaUpdateWrapper<UserNotification>()
                    .eq(UserNotification::getNotificationId, id)
                    .set(UserNotification::getIsDeleted, 1));
        }
        return success;
    }

    @Override
    public Page<NotificationVO> pageNotifications(Integer pageNum, Integer pageSize, String title, Byte noticeType) {
        Page<NotificationVO> page = new Page<>(pageNum, pageSize);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Integer userId = loginUser != null ? loginUser.getUserId() : null;
        Integer deptId = loginUser != null ? loginUser.getDeptId() : null;
        Byte dataScope = loginUser != null ? loginUser.getDataScope() : null;
        return baseMapper.selectNotificationPage(page, title, noticeType, userId, deptId, dataScope);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteNotifications(List<Integer> ids) {
        if (CollUtil.isEmpty(ids)) return true;
        // 物理删除或逻辑删除
        this.removeByIds(ids);
        // 同步删除关联记录
        userNotificationService.remove(new LambdaQueryWrapper<UserNotification>()
                .in(UserNotification::getNotificationId, ids));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateNotification(Integer id, NotificationSaveDTO saveDTO) {
        Notification notification = this.getById(id);
        if (notification == null) return false;

        // 仅更新可变字段，不可变字段（sender/receiver/status/withdraw/readCount）保持不变
        notification.setTitle(saveDTO.getTitle());
        notification.setContent(saveDTO.getContent());
        if (saveDTO.getContentType() != null) notification.setContentType(saveDTO.getContentType());
        if (saveDTO.getPriority() != null) notification.setPriority(saveDTO.getPriority());
        notification.setExpireTime(saveDTO.getExpireTime());
        notification.setRouterPath(saveDTO.getRouterPath());
        notification.setBusinessType(saveDTO.getBusinessType());
        notification.setBusinessId(saveDTO.getBusinessId());

        return this.updateById(notification);
    }
}
