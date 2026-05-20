package com.guang.trade.controller.system;

import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.trade.domain.dto.LoanApplyDTO;
import com.guang.trade.domain.dto.LoanAuditDTO;
import com.guang.trade.entity.LoanRecord;
import com.guang.trade.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/trade/v1/loans")
@Tag(name = "贷款管理")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @Operation(summary = "查询贷款信息")
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAuthority('trade:order:query')")
    public CommonResult<LoanRecord> getInfo(@PathVariable Integer transactionId) {
        return CommonResult.success(loanService.getByTransactionId(transactionId));
    }

    @Operation(summary = "提交贷款申请")
    @PostMapping
    @PreAuthorize("hasAuthority('trade:order:edit')")
    @Log(title = "贷款管理", businessType = "LOAN", operatorType = "SAVE")
    public CommonResult<LoanRecord> apply(@Validated @RequestBody LoanApplyDTO applyDTO) {
        return CommonResult.success(loanService.apply(applyDTO));
    }

    @Operation(summary = "贷款审核/放款")
    @PutMapping("/audit")
    @PreAuthorize("hasAuthority('trade:order:edit')")
    @Log(title = "贷款管理", businessType = "LOAN", operatorType = "AUDIT")
    public CommonResult<LoanRecord> audit(@Validated @RequestBody LoanAuditDTO auditDTO) {
        return CommonResult.success(loanService.audit(auditDTO));
    }
}
