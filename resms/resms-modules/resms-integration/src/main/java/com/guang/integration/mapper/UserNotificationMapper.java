package com.guang.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.integration.entity.UserNotification;

/**
 * <p>
 * 用户通知关联表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.integration.domain.vo.UserNotificationVO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 用户通知关联表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface UserNotificationMapper extends BaseMapper<UserNotification> {

    /**
     * 联表分页查询我的通知列表
     */
    Page<UserNotificationVO> selectMyNotificationPage(Page<UserNotificationVO> page, 
                                                      @Param("userId") Integer userId, 
                                                      @Param("isRead") Byte isRead);
}
