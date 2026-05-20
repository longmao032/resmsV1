package com.guang.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.trade.domain.dto.CustomerQueryDTO;
import com.guang.trade.domain.dto.CustomerSaveDTO;
import com.guang.trade.domain.vo.CustomerVO;
import com.guang.trade.entity.Customer;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 客户信息表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface CustomerService extends IService<Customer> {

    /**
     * 分页查询客户列表 (VO)
     */
    Page<CustomerVO> pageCustomers(CustomerQueryDTO queryDTO);

    /**
     * 保存或更新客户
     */
    Boolean saveCustomer(CustomerSaveDTO saveDTO);

    /**
     * 获取明文手机号
     */
    String getRealPhone(Integer id);

    /**
     * 根据 C 端用户 ID 获取客户 ID
     */
    Integer getIdByAppUserId(Long appUserId);

    /**
     * 指派客户给销售人员
     */
    Boolean assignCustomer(Integer customerId, Integer salesId);
}
