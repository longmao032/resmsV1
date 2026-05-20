package com.guang.trade.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.common.exception.ApiException;
import com.guang.trade.domain.dto.FollowUpSaveDTO;
import com.guang.trade.domain.vo.FollowUpVO;
import com.guang.trade.entity.Customer;
import com.guang.trade.entity.CustomerFollowUpRecord;
import com.guang.trade.mapper.CustomerFollowUpRecordMapper;
import com.guang.trade.mapper.CustomerMapper;
import com.guang.trade.service.CustomerFollowUpRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerFollowUpRecordServiceImpl extends ServiceImpl<CustomerFollowUpRecordMapper, CustomerFollowUpRecord> implements CustomerFollowUpRecordService {

    private final CustomerFollowUpRecordMapper followUpMapper;
    private final CustomerMapper customerMapper;

    @Override
    public Page<FollowUpVO> pageFollowUp(Integer pageNum, Integer pageSize, Integer customerId) {
        Page<FollowUpVO> page = new Page<>(pageNum, pageSize);
        return (Page<FollowUpVO>) followUpMapper.selectFollowUpPage(page, customerId);
    }

    @Override
    public List<FollowUpVO> listFollowUpByCustomerId(Integer customerId) {
        return followUpMapper.selectFollowUpByCustomerId(customerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveFollowUp(FollowUpSaveDTO saveDTO) {
        CustomerFollowUpRecord record = new CustomerFollowUpRecord();
        record.setCustomerId(saveDTO.getCustomerId());
        record.setType(saveDTO.getType());
        record.setHouseId(saveDTO.getHouseId());
        record.setSalesId(saveDTO.getSalesId());
        // customerFeedback 优先作为客户反馈，content(销售备注)作为备用
        record.setContent(saveDTO.getCustomerFeedback() != null ? saveDTO.getCustomerFeedback() : saveDTO.getContent());
        record.setNewIntentionLevel(saveDTO.getNewIntentionLevel());
        record.setFollowDate(saveDTO.getFollowDate() != null ? saveDTO.getFollowDate() : LocalDateTime.now());
        record.setAppointType((byte) 1); // 销售录入

        if ("visit".equals(saveDTO.getType()) && saveDTO.getStatus() != null) {
            record.setStatus(saveDTO.getStatus());
            if (saveDTO.getStatus() == 3) {
                record.setCancelReason(saveDTO.getCancelReason());
            }
        } else {
            record.setStatus((byte) 2);
        }

        record.setFollowAdvice(saveDTO.getFollowAdvice());
        boolean saved = this.save(record);

        if (saved && saveDTO.getNewIntentionLevel() != null) {
            updateCustomerIntention(saveDTO.getCustomerId(), saveDTO.getNewIntentionLevel());
        }

        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateFollowUp(Integer id, FollowUpSaveDTO saveDTO) {
        CustomerFollowUpRecord record = this.getById(id);
        if (record == null) {
            throw new ApiException("跟进记录不存在");
        }

        if (saveDTO.getCustomerFeedback() != null) {
            record.setContent(saveDTO.getCustomerFeedback());
        } else if (saveDTO.getContent() != null) {
            record.setContent(saveDTO.getContent());
        }
        if (saveDTO.getFollowAdvice() != null) {
            record.setFollowAdvice(saveDTO.getFollowAdvice());
        }
        if (saveDTO.getNewIntentionLevel() != null) {
            record.setNewIntentionLevel(saveDTO.getNewIntentionLevel());
        }
        if (saveDTO.getCancelReason() != null) {
            record.setCancelReason(saveDTO.getCancelReason());
        }

        return this.updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean createAppointment(Integer customerId, Integer salesId, LocalDateTime viewTime,
                                     Integer houseId, String customerFeedback, Byte newIntentionLevel) {
        CustomerFollowUpRecord record = new CustomerFollowUpRecord();
        record.setCustomerId(customerId);
        record.setType("visit");
        record.setHouseId(houseId);
        record.setSalesId(salesId);
        record.setContent(customerFeedback);
        record.setNewIntentionLevel(newIntentionLevel);
        record.setFollowDate(viewTime);
        record.setStatus((byte) 1);
        record.setAppointType((byte) 1);
        boolean saved = this.save(record);

        if (saved && newIntentionLevel != null) {
            updateCustomerIntention(customerId, newIntentionLevel);
        }
        return saved;
    }

    @Override
    public boolean hasAppointmentConflict(Integer customerId, LocalDateTime viewTime) {
        // 约定：前后 2 小时内不能有其他预约
        LocalDateTime start = viewTime.minusHours(2);
        LocalDateTime end = viewTime.plusHours(2);

        Long count = this.lambdaQuery()
                .eq(CustomerFollowUpRecord::getCustomerId, customerId)
                .eq(CustomerFollowUpRecord::getType, "visit")
                .in(CustomerFollowUpRecord::getStatus, 1, 2) // 待确认或已确认（未取消）
                .between(CustomerFollowUpRecord::getFollowDate, start, end)
                .count();

        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean completeAppointment(Integer id, String customerFeedback, String followAdvice, Byte newIntentionLevel) {
        CustomerFollowUpRecord record = this.getById(id);
        if (record == null) {
            throw new ApiException("预约记录不存在");
        }
        if (record.getStatus() != 1) {
            throw new ApiException("当前预约状态不允许确认完成");
        }
        record.setStatus((byte) 2);
        if (customerFeedback != null) {
            record.setContent(customerFeedback);
        }
        if (followAdvice != null) {
            record.setFollowAdvice(followAdvice);
        }
        record.setNewIntentionLevel(newIntentionLevel);
        boolean updated = this.updateById(record);

        if (updated && newIntentionLevel != null) {
            updateCustomerIntention(record.getCustomerId(), newIntentionLevel);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelAppointment(Integer id, String cancelReason) {
        CustomerFollowUpRecord record = this.getById(id);
        if (record == null) {
            throw new ApiException("预约记录不存在");
        }
        if (record.getStatus() != 1) {
            throw new ApiException("当前预约状态不允许取消");
        }
        record.setStatus((byte) 3);
        record.setCancelReason(cancelReason);
        return this.updateById(record);
    }

    @Override
    public List<FollowUpVO> listAppointments(Integer customerId) {
        return followUpMapper.selectAppointmentsByCustomerId(customerId);
    }

    private void updateCustomerIntention(Integer customerId, Byte newIntentionLevel) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer != null) {
            customer.setIntentionLevel(newIntentionLevel);
            customer.setUpdateTime(LocalDateTime.now());
            customerMapper.updateById(customer);
        }
    }
}
