package com.guang.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.result.CommonResult;
import com.guang.system.domain.dto.LogQueryDTO;
import com.guang.system.entity.OperationLog;
import com.guang.system.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 操作日志管理 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Tag(name = "操作日志", description = "系统操作日志相关接口")
@RestController
@RequestMapping("/api/system/v1/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:log:query')")
    public CommonResult<Page<OperationLog>> list(LogQueryDTO queryDTO) {
        return CommonResult.success(operationLogService.pageLogs(queryDTO));
    }
}
