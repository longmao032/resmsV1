package com.guang.trade.controller.system;

import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.trade.domain.dto.FeeSaveDTO;
import com.guang.trade.entity.TransactionFee;
import com.guang.trade.service.TransactionFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/trade/v1/fees")
@Tag(name = "交易费用管理")
@RequiredArgsConstructor
public class FeeController {

    private final TransactionFeeService transactionFeeService;

    @Operation(summary = "查询交易费用列表")
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAuthority('trade:order:query')")
    public CommonResult<List<TransactionFee>> list(@PathVariable Integer transactionId) {
        return CommonResult.success(transactionFeeService.listByTransactionId(transactionId));
    }

    @Operation(summary = "记录交易费用")
    @PostMapping
    @PreAuthorize("hasAuthority('trade:order:edit')")
    @Log(title = "交易费用", businessType = "FEE", operatorType = "SAVE")
    public CommonResult<TransactionFee> save(@Validated @RequestBody FeeSaveDTO saveDTO) {
        return CommonResult.success(transactionFeeService.save(saveDTO));
    }
}
