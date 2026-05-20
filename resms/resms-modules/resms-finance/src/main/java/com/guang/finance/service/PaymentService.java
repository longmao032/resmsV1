package com.guang.finance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.finance.domain.dto.PaymentAuditDTO;
import com.guang.finance.domain.dto.PaymentQueryDTO;
import com.guang.finance.domain.dto.PaymentRefundDTO;
import com.guang.finance.domain.dto.PaymentSubmitDTO;
import com.guang.finance.entity.Payment;
import com.guang.finance.domain.vo.PaymentVO;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * 收退款记录表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface PaymentService extends IService<Payment> {

    /**
     * 分页查询支付流水
     */
    Page<PaymentVO> pagePayments(PaymentQueryDTO queryDTO);

    /**
     * 提交支付流水（待审核）
     */
    Boolean submitPayment(PaymentSubmitDTO submitDTO);

    /**
     * 审核支付流水（通过=1 / 驳回=0）
     */
    Boolean auditPayment(PaymentAuditDTO auditDTO);

    /**
     * 作废已通过的流水（status=1 → 2）
     */
    Boolean voidPayment(Integer id);

    /**
     * 发起退款申请（新增 flow_type=2 记录）
     */
    Boolean applyRefund(PaymentRefundDTO refundDTO);

    /**
     * 导出支付流水
     */
    void exportPayments(PaymentQueryDTO queryDTO, HttpServletResponse response);
}
