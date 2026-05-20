package com.guang.finance.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.event.PaymentApprovedEvent;
import com.guang.common.exception.ApiException;
import com.guang.common.util.CodeGeneratorUtil;
import com.guang.common.util.SecurityUtils;
import com.guang.common.annotation.DataScope;
import com.guang.finance.domain.dto.PaymentAuditDTO;
import com.guang.finance.domain.dto.PaymentQueryDTO;
import com.guang.finance.domain.dto.PaymentRefundDTO;
import com.guang.finance.domain.dto.PaymentSubmitDTO;
import com.guang.finance.domain.vo.PaymentExportVO;
import com.guang.finance.domain.vo.PaymentVO;
import com.guang.finance.entity.Payment;
import com.guang.finance.mapper.PaymentMapper;
import com.guang.finance.service.PaymentService;
import com.guang.trade.entity.PaymentPlan;
import com.guang.trade.entity.Transaction;
import com.guang.trade.mapper.PaymentPlanMapper;
import com.guang.trade.mapper.TransactionMapper;
import com.guang.trade.strategy.OrderTriggerFactory;
import com.guang.trade.strategy.OrderTriggerStrategy;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guang.common.util.ExcelUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 收退款记录表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    private final ApplicationEventPublisher eventPublisher;
    private final PaymentPlanMapper paymentPlanMapper;
    private final TransactionMapper transactionMapper;
    private final OrderTriggerFactory orderTriggerFactory;

    @Override
    @DataScope(userField = "finance_id", joinUserDept = true)
    public Page<PaymentVO> pagePayments(PaymentQueryDTO queryDTO) {
        Page<PaymentVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return baseMapper.selectPaymentPage(page, queryDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitPayment(PaymentSubmitDTO submitDTO) {
        // 1. 前置校验：校验账单计划
        PaymentPlan plan = paymentPlanMapper.selectById(submitDTO.getPaymentPlanId());
        if (plan == null) {
            throw new ApiException("关联的应收账单不存在");
        }
        if (plan.getStatus() == 2) {
            throw new ApiException("该账单已结清，无需再次支付");
        }
        if (plan.getStatus() == 3) {
            throw new ApiException("该账单已取消，无法录入支付流水");
        }
        if (!plan.getTransactionId().equals(submitDTO.getTransactionId())) {
            throw new ApiException("账单与交易不匹配，请核实数据");
        }

        // 2. 前置校验：校验交易状态
        Transaction tx = transactionMapper.selectById(submitDTO.getTransactionId());
        if (tx == null) {
            throw new ApiException("关联的交易记录不存在");
        }
        if (tx.getStatus() == 5) {
            throw new ApiException("关联交易已取消，无法录入支付流水");
        }

        // 3. 校验是否存在待审核的流水（防止重复提交）
        LambdaQueryWrapper<Payment> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Payment::getPaymentPlanId, submitDTO.getPaymentPlanId())
                .eq(Payment::getPaymentStatus, (byte) 0)
                .eq(Payment::getFlowType, (byte) 1)
                .eq(Payment::getIsDeleted, (byte) 0);
        Long pendingCount = this.count(pendingWrapper);
        if (pendingCount > 0) {
            throw new ApiException("该账单已有流水正在审核中，请等待审核结果后再提交");
        }

        Payment payment = BeanUtil.copyProperties(submitDTO, Payment.class);
        payment.setReceiptNo(CodeGeneratorUtil.generateReceiptNo()); // 自动生成收据编号
        payment.setPaymentStatus((byte) 0); // 待确认
        payment.setCreateTime(LocalDateTime.now());
        payment.setFinanceId(SecurityUtils.getUserId()); // 经办人 = 当前提交人（销售）
        if (payment.getPaymentTime() == null) {
            payment.setPaymentTime(LocalDateTime.now()); // 未传支付时间则默认当前时间
        }

        return this.save(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean auditPayment(PaymentAuditDTO auditDTO) {
        Payment payment = this.getById(auditDTO.getId());
        if (payment == null) {
            throw new ApiException("流水记录不存在");
        }
        if (payment.getPaymentStatus() != 0) {
            throw new ApiException("该记录已被审核，不能重复操作");
        }

        Byte targetStatus = auditDTO.getPaymentStatus();
        // 驳回：status=0 → 3
        if (targetStatus == 0) {
            if (auditDTO.getRemark() == null || auditDTO.getRemark().isBlank()) {
                throw new ApiException("驳回必须填写原因");
            }
            payment.setPaymentStatus((byte) 3); // 已驳回
            payment.setRemark(auditDTO.getRemark());
            payment.setAuditUserId(SecurityUtils.getUserId());
            payment.setAuditTime(LocalDateTime.now());
            return this.updateById(payment);
        }

        // 通过：status=0 → 1
        payment.setPaymentStatus((byte) 1);
        payment.setActualAmount(auditDTO.getActualAmount() != null ? auditDTO.getActualAmount() : payment.getAmount());
        if (auditDTO.getRemark() != null) {
            payment.setRemark(auditDTO.getRemark());
        }
        payment.setAuditUserId(SecurityUtils.getUserId());
        payment.setAuditTime(LocalDateTime.now());
        payment.setFinanceId(SecurityUtils.getUserId()); // 经办财务 = 审核人

        boolean success = this.updateById(payment);
        if (!success) return false;

        // 发布审核事件
        Transaction tx = transactionMapper.selectById(payment.getTransactionId());
        PaymentPlan plan = paymentPlanMapper.selectById(payment.getPaymentPlanId());
        if (tx != null && plan != null) {
            eventPublisher.publishEvent(new com.guang.common.event.PaymentAuditedEvent(
                    this, payment.getTransactionId(), plan.getPayName(),
                    payment.getActualAmount(), (byte) 1, auditDTO.getRemark(), tx.getSalesId()));
        }

        // 核销账单
        doReconciliation(payment);
        return true;
    }

    /**
     * 核销账单 + 通过策略模式推进交易状态
     */
    private void doReconciliation(Payment payment) {
        Integer planId = payment.getPaymentPlanId();
        if (planId == null) return;

        PaymentPlan plan = paymentPlanMapper.selectById(planId);
        if (plan == null) return;

        // 1. 核销：更新账单已收金额
        BigDecimal actualAmount = payment.getActualAmount() != null ? payment.getActualAmount() : payment.getAmount();
        BigDecimal oldPaid = plan.getPaidAmount() != null ? plan.getPaidAmount() : BigDecimal.ZERO;
        plan.setPaidAmount(oldPaid.add(actualAmount));
        plan.setStatus(plan.getPaidAmount().compareTo(plan.getReceivableAmount()) >= 0 ? (byte) 2 : (byte) 1);
        paymentPlanMapper.updateById(plan);

        // 2. 获取交易（加行锁防止并发覆盖）
        Transaction tx = transactionMapper.selectByIdForUpdate(plan.getTransactionId());
        if (tx == null) return;

        // 3. 判断是否全部账单已结清
        LambdaQueryWrapper<PaymentPlan> qw = new LambdaQueryWrapper<>();
        qw.eq(PaymentPlan::getTransactionId, plan.getTransactionId())
           .ne(PaymentPlan::getStatus, (byte) 2);
        boolean allSettled = paymentPlanMapper.selectCount(qw) == 0;

        // 4. 委托策略处理交易状态推进（根据付款方式动态匹配）
        OrderTriggerStrategy strategy = orderTriggerFactory.getStrategy((int) tx.getPaymentType());
        strategy.handlePaymentSuccess(tx, payment.getPaymentType(), actualAmount, allSettled);

        // 5. 发布核销完成事件（用于触发通知公告等）
        eventPublisher.publishEvent(new PaymentApprovedEvent(
                this, payment.getTransactionId(), payment.getPaymentType(), actualAmount));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean voidPayment(Integer id) {
        Payment payment = this.getById(id);
        if (payment == null) throw new ApiException("流水记录不存在");
        if (payment.getPaymentStatus() != 1) throw new ApiException("仅有效记录可作废");

        payment.setPaymentStatus((byte) 2); // 已作废
        payment.setAuditUserId(SecurityUtils.getUserId());
        payment.setAuditTime(LocalDateTime.now());
        return this.updateById(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean applyRefund(PaymentRefundDTO refundDTO) {
        // 校验原收款记录
        Payment original = this.getById(refundDTO.getOriginalPaymentId());
        if (original == null) throw new ApiException("原收款记录不存在");
        if (original.getPaymentStatus() != 1) throw new ApiException("仅有效收款记录可发起退款");

        Payment refund = new Payment();
        refund.setTransactionId(original.getTransactionId());
        refund.setPaymentPlanId(original.getPaymentPlanId());
        refund.setPaymentType(original.getPaymentType());
        refund.setFlowType((byte) 2); // 退款
        refund.setAmount(refundDTO.getAmount());
        refund.setPaymentMethod(refundDTO.getRefundMethod());
        refund.setPayerInfo(refundDTO.getPayerInfo());
        refund.setProofUrl(refundDTO.getProofUrl());
        refund.setRemark(refundDTO.getRemark());
        refund.setReceiptNo(CodeGeneratorUtil.generateReceiptNo());
        refund.setPaymentStatus((byte) 0); // 待审核
        refund.setCreateTime(LocalDateTime.now());
        refund.setFinanceId(SecurityUtils.getUserId());
        refund.setPaymentTime(LocalDateTime.now());
        return this.save(refund);
    }

    @Override
    @DataScope(userField = "finance_id", joinUserDept = true)
    public void exportPayments(PaymentQueryDTO queryDTO, HttpServletResponse response) {
        List<PaymentVO> list = baseMapper.selectPaymentExportList(queryDTO);

        List<PaymentExportVO> exportList = list.stream().map(p -> {
            PaymentExportVO vo = BeanUtil.copyProperties(p, PaymentExportVO.class);

            // 资金流向
            if (p.getFlowType() != null) {
                vo.setFlowTypeText(p.getFlowType() == 1 ? "收款" : "退款");
            }

            // 款项类型
            if (p.getPaymentType() != null) {
                String typeText;
                switch (p.getPaymentType()) {
                    case 1:  typeText = "定金"; break;
                    case 2:  typeText = "首付款"; break;
                    case 3:  typeText = "尾款"; break;
                    case 4:  typeText = "中介费"; break;
                    case 5:  typeText = "贷款"; break;
                    default: typeText = "未知"; break;
                }
                vo.setPaymentTypeText(typeText);
            }

            // 审核状态
            if (p.getPaymentStatus() != null) {
                String statusText;
                switch (p.getPaymentStatus()) {
                    case 0:  statusText = "待审核"; break;
                    case 1:  statusText = "有效"; break;
                    case 2:  statusText = "已作废"; break;
                    case 3:  statusText = "已驳回"; break;
                    default: statusText = "未知"; break;
                }
                vo.setStatusText(statusText);
            }

            return vo;
        }).collect(Collectors.toList());

        ExcelUtils.exportExcel(response, "支付流水_" + System.currentTimeMillis(), "支付流水", PaymentExportVO.class, exportList);
    }
}
