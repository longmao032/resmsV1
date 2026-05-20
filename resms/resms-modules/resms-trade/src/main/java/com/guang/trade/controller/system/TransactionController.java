package com.guang.trade.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.trade.domain.dto.TransactionQueryDTO;
import com.guang.trade.domain.dto.TransactionSaveDTO;
import com.guang.trade.domain.dto.TransactionStatusUpdateDTO;
import com.guang.trade.domain.dto.CancelWithRefundDTO;
import com.guang.trade.domain.vo.TransactionVO;
import com.guang.trade.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 交易信息表 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@RestController
@RequestMapping("/api/system/trade/v1/transactions")
@Tag(name = "交易管理")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "分页查询交易列表")
    @GetMapping
    @PreAuthorize("hasAuthority('trade:order:query')")
    public CommonResult<Page<TransactionVO>> list(TransactionQueryDTO queryDTO) {
        return CommonResult.success(transactionService.pageTransactions(queryDTO));
    }

    @Operation(summary = "创建交易 (录入意向/发起定金)")
    @PostMapping
    @PreAuthorize("hasAuthority('trade:order:add')")
    @Log(title = "交易管理", businessType = "TRANSACTION", operatorType = "ADD")
    public CommonResult<Boolean> create(@Validated @RequestBody TransactionSaveDTO saveDTO) {
        return CommonResult.success(transactionService.createTransaction(saveDTO));
    }

    @Operation(summary = "更新交易状态 (流转/联动)")
    @PutMapping("/status")
    @PreAuthorize("hasAuthority('trade:order:edit')")
    @Log(title = "交易管理", businessType = "TRANSACTION", operatorType = "STATUS")
    public CommonResult<Boolean> updateStatus(@Validated @RequestBody TransactionStatusUpdateDTO updateDTO) {
        return CommonResult.success(transactionService.updateStatus(updateDTO));
    }

    @Operation(summary = "查看交易详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('trade:order:query')")
    public CommonResult<TransactionVO> getInfo(@PathVariable Integer id) {
        return CommonResult.success(transactionService.getTransactionDetail(id));
    }

    @Operation(summary = "获取交易账单计划")
    @GetMapping("/{id}/payment-plans")
    @PreAuthorize("hasAuthority('trade:order:query')")
    public CommonResult<java.util.List<com.guang.trade.entity.PaymentPlan>> getPaymentPlans(@PathVariable Integer id) {
        return CommonResult.success(transactionService.getPaymentPlans(id));
    }

    @Operation(summary = "检查交易锁定状态")
    @GetMapping("/{id}/lock-status")
    @PreAuthorize("hasAuthority('trade:order:query')")
    public CommonResult<Boolean> isLocked(@PathVariable Integer id) {
        return CommonResult.success(transactionService.isTransactionLocked(id));
    }

    @Operation(summary = "取消交易并退款（自动为已入账的收款创建退款流水）")
    @PutMapping("/cancel-with-refund")
    @PreAuthorize("hasAuthority('trade:order:edit')")
    @Log(title = "交易管理", businessType = "TRANSACTION", operatorType = "DELETE", description = "取消交易并退款")
    public CommonResult<Boolean> cancelWithRefund(@Validated @RequestBody CancelWithRefundDTO dto) {
        return CommonResult.success(transactionService.cancelWithRefund(dto.getId(), dto.getReason()));
    }

    @Operation(summary = "导出交易订单")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('trade:order:export')")
    public void export(TransactionQueryDTO queryDTO, HttpServletResponse response) {
        transactionService.exportTransactions(queryDTO, response);
    }
}
