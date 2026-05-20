package com.guang.integration.service.impl;

import com.guang.integration.entity.UserNotification;
import com.guang.integration.mapper.UserNotificationMapper;
import com.guang.integration.service.UserNotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户通知关联表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.util.SecurityUtils;
import com.guang.integration.domain.vo.UserNotificationVO;
import com.guang.integration.entity.Notification;
import com.guang.integration.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户通知关联表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl extends ServiceImpl<UserNotificationMapper, UserNotification> implements UserNotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public Page<UserNotificationVO> pageMyNotifications(Integer pageNum, Integer pageSize, Byte isRead) {
        Page<UserNotificationVO> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectMyNotificationPage(page, SecurityUtils.getUserId(), isRead);
    }

    @Override
    public Boolean markAsRead(Long id) {
        UserNotification un = this.getById(id);
        if (un == null || !un.getUserId().equals(SecurityUtils.getUserId())) {
            return false;
        }
        if (un.getIsRead() == 1) return true;

        un.setIsRead((byte) 1);
        un.setReadTime(LocalDateTime.now());
        boolean success = this.updateById(un);
        
        if (success) {
            // 同步更新 Notification 的 read_count (此步可优化为异步)
            notificationMapper.updateReadCount(un.getNotificationId());
        }
        return success;
    }

    @Override
    public Boolean markAllRead() {
        Integer userId = SecurityUtils.getUserId();
        LambdaUpdateWrapper<UserNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1)
                .set(UserNotification::getReadTime, LocalDateTime.now());
        return this.update(wrapper);
    }

    @Override
    public Long getUnreadCount() {
        Integer userId = SecurityUtils.getUserId();
        return this.count(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0)
                .eq(UserNotification::getIsDeleted, 0));
    }
}
