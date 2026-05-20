package com.guang.trade.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.exception.ApiException;
import com.guang.common.util.CodeGeneratorUtil;
import com.guang.common.util.SecurityUtils;
import com.guang.common.annotation.DataScope;
import com.guang.common.annotation.BolasGuard;
import com.guang.trade.domain.dto.CustomerQueryDTO;
import com.guang.trade.domain.dto.CustomerSaveDTO;
import com.guang.trade.domain.vo.CustomerVO;
import com.guang.trade.entity.Customer;
import com.guang.trade.mapper.CustomerMapper;
import com.guang.trade.service.CustomerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 客户信息表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    private final CustomerMapper customerMapper;

    @Override
    @DataScope(userField = "sales_id", joinUserDept = true, includeNullUser = true)
    public Page<CustomerVO> pageCustomers(CustomerQueryDTO queryDTO) {
        // 处理公海池筛选
        if ("mine".equals(queryDTO.getPoolFilter())) {
            queryDTO.setSalesId(SecurityUtils.getUserId());
        }
        // pool 筛选由 XML 中处理 (sales_id IS NULL)

        Page<CustomerVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<CustomerVO> result = (Page<CustomerVO>) customerMapper.selectCustomerPage(page, queryDTO);

        // 手机号脱敏
        result.getRecords().forEach(customer -> {
            if (StrUtil.isNotBlank(customer.getPhone())) {
                customer.setPhone(DesensitizedUtil.mobilePhone(customer.getPhone()));
            }
        });

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @BolasGuard(entityClass = Customer.class, userField = "salesId", includeNullUser = true)
    public Boolean saveCustomer(CustomerSaveDTO saveDTO) {
        Customer customer = BeanUtil.copyProperties(saveDTO, Customer.class);

        if (customer.getId() == null) {
            customer.setCustomerNo(CodeGeneratorUtil.generateCustomerNo());
        }

        return this.saveOrUpdate(customer);
    }

    @Override
    public String getRealPhone(Integer id) {
        Customer customer = this.getById(id);
        if (customer == null) {
            throw new ApiException("客户不存在");
        }
        return customer.getPhone();
    }

    @Override
    public Integer getIdByAppUserId(Long appUserId) {
        Customer customer = this.lambdaQuery()
                .eq(Customer::getAppUserId, appUserId)
                .last("LIMIT 1")
                .one();
        return customer != null ? customer.getId() : null;
    }

    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @BolasGuard(entityClass = Customer.class, userField = "salesId", includeNullUser = true, idParamName = "customerId")
    public Boolean assignCustomer(Integer customerId, Integer salesId) {
        Customer customer = this.getById(customerId);
        if (customer == null) {
            throw new ApiException("客户不存在");
        }

        customer.setSalesId(salesId);
        customer.setUpdateTime(LocalDateTime.now());
        boolean success = this.updateById(customer);

        if (success) {
            // 发布指派事件，触发通知
            eventPublisher.publishEvent(new com.guang.common.event.LeadAssignedEvent(
                    this,
                    customer.getId(),
                    customer.getRealName(),
                    customer.getPhone(),
                    customer.getSource() != null ? customer.getSource() : "系统分配",
                    salesId
            ));
            log.info("[客户指派] 客户(id={}, name={}) 已指派给销售人员(id={})", customerId, customer.getRealName(), salesId);
        }

        return success;
    }
}
