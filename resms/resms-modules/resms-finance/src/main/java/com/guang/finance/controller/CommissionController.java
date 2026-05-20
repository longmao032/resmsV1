package com.guang.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.finance.entity.Commission;
import com.guang.finance.service.CommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 佣金记录表 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@RestController
@RequestMapping("/api/finance/v1/commissions")
@Tag(name = "佣金管理")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @Operation(summary = "分页查询佣金记录")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:commission:list')")
    public CommonResult<Page<Commission>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Byte status) {
        return CommonResult.success(commissionService.pageCommissions(pageNum, pageSize, userId, status));
    }

    @Operation(summary = "确认核算佣金")
    @PutMapping("/{id}/calculate")
    @PreAuthorize("hasAuthority('finance:commission:calculate')")
    @Log(title = "佣金管理", businessType = "COMMISSION", operatorType = "UPDATE")
    public CommonResult<Boolean> calculate(@PathVariable Integer id) {
        return CommonResult.success(commissionService.calculateCommission(id));
    }

    @Operation(summary = "确认发放佣金")
    @PutMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('finance:commission:issue')")
    @Log(title = "佣金管理", businessType = "COMMISSION", operatorType = "UPDATE")
    public CommonResult<Boolean> issue(@PathVariable Integer id) {
        return CommonResult.success(commissionService.issueCommission(id));
    }

    @Operation(summary = "查看佣金详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:commission:query')")
    public CommonResult<Commission> getInfo(@PathVariable Integer id) {
        return CommonResult.success(commissionService.getById(id));
    }
}
