package com.guang.portal.service.impl;
 
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.event.AppointmentCreatedEvent;
import com.guang.common.exception.ApiException;
import com.guang.common.util.CodeGeneratorUtil;
import com.guang.common.util.SecurityUtils;
import com.guang.house.entity.House;
import com.guang.house.service.HouseService;
import com.guang.portal.domain.dto.AppointmentSaveDTO;
import com.guang.portal.service.ClientAppointmentService;
import com.guang.trade.domain.vo.FollowUpVO;
import com.guang.trade.entity.AppUser;
import com.guang.trade.entity.Customer;
import com.guang.trade.entity.CustomerFollowUpRecord;
import com.guang.trade.service.AppUserService;
import com.guang.trade.service.CustomerFollowUpRecordService;
import com.guang.trade.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.LocalDateTime;
import java.util.List;
 
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAppointmentServiceImpl implements ClientAppointmentService {
 
    private final CustomerFollowUpRecordService followUpRecordService;
    private final CustomerService customerService;
    private final AppUserService appUserService;
    private final HouseService houseService;
    private final ApplicationEventPublisher eventPublisher;
 
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAppointment(AppointmentSaveDTO dto) {
        Long appUserId = SecurityUtils.getAppUserId();
        if (appUserId == null) throw new ApiException("未登录");
 
        // 1. 获取房源及顾问信息
        House house = houseService.getById(dto.getHouseId());
        if (house == null) throw new ApiException("房源不存在");
        if (house.getSalesId() == null) throw new ApiException("该房源暂未指派置业顾问，请联系客服");
 
        // 2. 获取或创建客户档案
        Integer customerId = customerService.getIdByAppUserId(appUserId);
        Customer customer;
        if (customerId == null) {
            AppUser appUser = appUserService.getById(appUserId);
            // 自动创建客户档案
            customer = new Customer();
            customer.setCustomerNo(CodeGeneratorUtil.generateCustomerNo());
            customer.setAppUserId(appUserId);
            customer.setRealName(appUser.getNickname());
            customer.setPhone(appUser.getPhone());
            customer.setSource("C端在线预约");
            customer.setSalesId(house.getSalesId()); // 默认指派给该房源的顾问
            customerService.save(customer);
            customerId = customer.getId();
        } else {
            customer = customerService.getById(customerId);
        }
 
        // 3. 冲突检查
        if (followUpRecordService.hasAppointmentConflict(customerId, dto.getViewTime())) {
            throw new ApiException("您在该时间段附近已有其他看房预约，请调整时间");
        }
 
        // 4. 创建预约
        boolean success = followUpRecordService.createAppointment(
                customerId,
                house.getSalesId(),
                dto.getViewTime(),
                dto.getHouseId(),
                dto.getRemark(),
                null
        );
 
        if (success) {
            // 获取最新创建的记录 ID (MyBatis Plus 会回填)
            // 这里简单处理，发布事件
            eventPublisher.publishEvent(new AppointmentCreatedEvent(
                    this,
                    null, // ID 暂时传空或查一下
                    customerId,
                    customer.getRealName(),
                    customer.getPhone(),
                    house.getId(),
                    house.getProjectName(),
                    dto.getViewTime(),
                    house.getSalesId()
            ));
            log.info("[C端预约] 用户 {} 预约房源 {} 成功", appUserId, house.getProjectName());
        }
    }
 
    @Override
    public Page<FollowUpVO> pageMyAppointments(Integer pageNum, Integer pageSize) {
        Long appUserId = SecurityUtils.getAppUserId();
        return pageMyAppointments(appUserId, pageNum, pageSize);
    }

    @Override
    public Page<FollowUpVO> pageMyAppointments(Long userId, Integer pageNum, Integer pageSize) {
        if (userId == null) {
            return new Page<>(pageNum, pageSize);
        }
        Integer customerId = customerService.getIdByAppUserId(userId);
        if (customerId == null) {
            return new Page<>(pageNum, pageSize);
        }
        return followUpRecordService.pageFollowUp(pageNum, pageSize, customerId);
    }
 
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAppointment(Integer id) {
        Long appUserId = SecurityUtils.getAppUserId();
        Integer customerId = customerService.getIdByAppUserId(appUserId);
        
        CustomerFollowUpRecord record = followUpRecordService.getById(id);
        if (record == null || !record.getCustomerId().equals(customerId)) {
            throw new ApiException("记录不存在或无权操作");
        }
        
        followUpRecordService.cancelAppointment(id, "用户自主取消");
    }
}
