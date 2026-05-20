package com.guang.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.finance.domain.dto.PaymentAuditDTO;
import com.guang.finance.domain.dto.PaymentQueryDTO;
import com.guang.finance.domain.dto.PaymentRefundDTO;
import com.guang.finance.domain.dto.PaymentSubmitDTO;
import com.guang.finance.entity.Payment;
import com.guang.finance.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.guang.finance.domain.vo.PaymentVO;

/**
 * <p>
 * 收退款记录表 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@RestController
@RequestMapping("/api/finance/v1/payments")
@Tag(name = "财务流水管理")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "分页查询支付流水")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('fin:payment:query', 'trade:order:query')")
    public CommonResult<Page<PaymentVO>> list(PaymentQueryDTO queryDTO) {
        return CommonResult.success(paymentService.pagePayments(queryDTO));
    }

    @Operation(summary = "提交支付流水 (经纪人)")
    @PostMapping
    @PreAuthorize("hasAuthority('fin:payment:add')")
    @Log(title = "财务流水管理", businessType = "PAYMENT", operatorType = "SAVE")
    public CommonResult<Boolean> submit(@Validated @RequestBody PaymentSubmitDTO submitDTO) {
        return CommonResult.success(paymentService.submitPayment(submitDTO));
    }

    @Operation(summary = "审核支付流水 (财务)")
    @PutMapping("/audit")
    @PreAuthorize("hasAuthority('fin:payment:audit')")
    @Log(title = "财务流水管理", businessType = "PAYMENT", operatorType = "AUDIT")
    public CommonResult<Boolean> audit(@Validated @RequestBody PaymentAuditDTO auditDTO) {
        return CommonResult.success(paymentService.auditPayment(auditDTO));
    }

    @Operation(summary = "查看流水详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('fin:payment:query', 'trade:order:query')")
    public CommonResult<Payment> getInfo(@PathVariable Integer id) {
        return CommonResult.success(paymentService.getById(id));
    }

    @Operation(summary = "作废已通过的流水")
    @PutMapping("/{id}/void")
    @PreAuthorize("hasAuthority('fin:payment:audit')")
    @Log(title = "财务流水管理", businessType = "PAYMENT", operatorType = "DELETE")
    public CommonResult<Boolean> voidPayment(@PathVariable Integer id) {
        return CommonResult.success(paymentService.voidPayment(id));
    }

    @Operation(summary = "发起退款申请")
    @PostMapping("/refund")
    @PreAuthorize("hasAuthority('fin:payment:add')")
    @Log(title = "财务流水管理", businessType = "PAYMENT", operatorType = "SAVE", description = "发起退款申请")
    public CommonResult<Boolean> applyRefund(@Validated @RequestBody PaymentRefundDTO refundDTO) {
        return CommonResult.success(paymentService.applyRefund(refundDTO));
    }

    @Operation(summary = "导出支付流水")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('fin:payment:query')")
    public void export(PaymentQueryDTO queryDTO, HttpServletResponse response) {
        paymentService.exportPayments(queryDTO, response);
    }
}
