package com.guang.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.trade.domain.dto.CustomerQueryDTO;
import com.guang.trade.domain.vo.CustomerVO;
import com.guang.trade.entity.Customer;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 客户信息表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 分页查询客户列表 (含销售姓名)
     */
    IPage<CustomerVO> selectCustomerPage(Page<CustomerVO> page, @Param("query") CustomerQueryDTO query);
}
