package com.guang.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guang.trade.domain.dto.FollowUpSaveDTO;
import com.guang.trade.domain.vo.FollowUpVO;
import com.guang.trade.entity.CustomerFollowUpRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerFollowUpRecordService extends IService<CustomerFollowUpRecord> {

    /**
     * 分页查询客户跟进记录
     */
    Page<FollowUpVO> pageFollowUp(Integer pageNum, Integer pageSize, Integer customerId);

    /**
     * 查询客户的所有跟进记录
     */
    List<FollowUpVO> listFollowUpByCustomerId(Integer customerId);

    /**
     * 保存跟进记录
     */
    Boolean saveFollowUp(FollowUpSaveDTO saveDTO);

    /**
     * 更新跟进记录
     */
    Boolean updateFollowUp(Integer id, FollowUpSaveDTO saveDTO);

    /**
     * 检查预约冲突（同一用户在前后2小时内不能有其他预约）
     */
    boolean hasAppointmentConflict(Integer customerId, LocalDateTime viewTime);

    /**
     * 创建预约带看（封装为type=visit, status=1）
     */
    Boolean createAppointment(Integer customerId, Integer salesId, LocalDateTime viewTime,
                             Integer houseId, String customerFeedback, Byte newIntentionLevel);

    /**
     * 确认完成预约
     */
    Boolean completeAppointment(Integer id, String customerFeedback, String followAdvice, Byte newIntentionLevel);

    /**
     * 取消预约
     */
    Boolean cancelAppointment(Integer id, String cancelReason);

    /**
     * 查询客户的预约列表（status=1 且未过期）
     */
    List<FollowUpVO> listAppointments(Integer customerId);
}
