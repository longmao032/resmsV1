package com.guang.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.integration.entity.Notification;

/**
 * <p>
 * 工作通知表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.integration.domain.vo.NotificationVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 工作通知表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface NotificationMapper extends BaseMapper<Notification> {

    @Update("UPDATE sys_notification SET read_count = read_count + 1 WHERE id = #{id}")
    int updateReadCount(@Param("id") Integer id);

    Page<NotificationVO> selectNotificationPage(Page<NotificationVO> page,
                                                 @Param("title") String title,
                                                 @Param("noticeType") Byte noticeType,
                                                 @Param("userId") Integer userId,
                                                 @Param("deptId") Integer deptId,
                                                 @Param("dataScope") Byte dataScope);
}
