package com.guang.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.finance.domain.dto.PaymentQueryDTO;
import com.guang.finance.domain.vo.PaymentVO;
import com.guang.finance.entity.Payment;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 收退款记录表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface PaymentMapper extends BaseMapper<Payment> {

    Page<PaymentVO> selectPaymentPage(Page<PaymentVO> page, @Param("query") PaymentQueryDTO query);

    java.util.List<PaymentVO> selectPaymentExportList(@Param("query") PaymentQueryDTO query);

}
