package com.guang.integration.service;

import com.guang.integration.entity.UserNotification;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户通知关联表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.integration.domain.vo.UserNotificationVO;

/**
 * <p>
 * 用户通知关联表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface UserNotificationService extends IService<UserNotification> {

    /**
     * 分页查询我的通知
     */
    Page<UserNotificationVO> pageMyNotifications(Integer pageNum, Integer pageSize, Byte isRead);

    /**
     * 标记为已读
     */
    Boolean markAsRead(Long id);

    /**
     * 全部标记为已读
     */
    Boolean markAllRead();

    /**
     * 获取未读通知数量
     */
    Long getUnreadCount();
}
