package com.guang.portal.service;
 
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.portal.domain.dto.AppointmentSaveDTO;
import com.guang.trade.domain.vo.FollowUpVO;
 
/**
 * C端预约看房服务
 */
public interface ClientAppointmentService {
 
    /**
     * 创建预约
     */
    void createAppointment(AppointmentSaveDTO dto);
 
    /**
     * 分页查询我的预约
     */
    Page<FollowUpVO> pageMyAppointments(Integer pageNum, Integer pageSize);

    /**
     * 获取指定用户的预约列表（供 AI/后台使用）
     */
    Page<FollowUpVO> pageMyAppointments(Long userId, Integer pageNum, Integer pageSize);
 
    /**
     * 取消预约
     */
    void cancelAppointment(Integer id);
}
