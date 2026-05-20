package com.guang.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.trade.domain.vo.FollowUpVO;
import com.guang.trade.entity.CustomerFollowUpRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CustomerFollowUpRecordMapper extends BaseMapper<CustomerFollowUpRecord> {

    /**
     * 查询客户跟进记录列表（含销售姓名、房源标题）
     */
    IPage<FollowUpVO> selectFollowUpPage(Page<FollowUpVO> page, @Param("customerId") Integer customerId);

    /**
     * 查询客户的所有跟进记录（不分页）
     */
    List<FollowUpVO> selectFollowUpByCustomerId(@Param("customerId") Integer customerId);

    /**
     * 查询客户的预约列表（status=1 且未过期）
     */
    List<FollowUpVO> selectAppointmentsByCustomerId(@Param("customerId") Integer customerId);
}
