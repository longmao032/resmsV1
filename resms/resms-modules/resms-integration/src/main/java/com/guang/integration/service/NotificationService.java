package com.guang.integration.service;

import com.guang.integration.entity.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 工作通知表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.integration.domain.dto.NotificationSaveDTO;
import com.guang.integration.domain.vo.NotificationVO;

import java.util.List;

/**
 * <p>
 * 工作通知表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 发布通知/公告
     */
    Boolean publishNotification(NotificationSaveDTO saveDTO);

    /**
     * 撤回通知
     */
    Boolean withdrawNotification(Integer id);

    /**
     * 分页查询通知 (管理端)
     */
    Page<NotificationVO> pageNotifications(Integer pageNum, Integer pageSize, String title, Byte noticeType);

    /**
     * 批量删除通知
     */
    Boolean deleteNotifications(List<Integer> ids);

    /**
     * 编辑已发布的通知（仅更新可变字段）
     */
    Boolean updateNotification(Integer id, NotificationSaveDTO saveDTO);
}
