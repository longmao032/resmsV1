package com.guang.trade.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.trade.domain.dto.HistoryDTO;
import com.guang.trade.domain.vo.FootprintVO;
import com.guang.trade.entity.UserBrowseHistory;
import com.guang.trade.service.UserBrowseHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户浏览历史记录表 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@RestController
@RequestMapping("/api/system/trade/v1/histories")
@Tag(name = "足迹管理")
@RequiredArgsConstructor
public class UserBrowseHistoryController {

    private final UserBrowseHistoryService userBrowseHistoryService;

    @Operation(summary = "管理端分页查询客户足迹")
    @GetMapping
    @PreAuthorize("hasAuthority('trade:history:query')")
    public CommonResult<Page<FootprintVO>> listFootprints(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String actionType) {
        return CommonResult.success(userBrowseHistoryService.pageFootprints(pageNum, pageSize, customerName, actionType));
    }

    @Operation(summary = "记录浏览历史")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Log(title = "足迹管理", businessType = "BROWSE", operatorType = "SAVE")
    public CommonResult<Boolean> record(@Validated @RequestBody HistoryDTO historyDTO) {
        return CommonResult.success(userBrowseHistoryService.recordHistory(historyDTO));
    }

    @Operation(summary = "我的浏览历史")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Page<UserBrowseHistory>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Byte resourceType) {
        return CommonResult.success(userBrowseHistoryService.pageMyHistory(pageNum, pageSize, resourceType));
    }

    @Operation(summary = "清空浏览历史")
    @DeleteMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Log(title = "足迹管理", businessType = "BROWSE", operatorType = "DELETE")
    public CommonResult<Boolean> clear() {
        return CommonResult.success(userBrowseHistoryService.clearMyHistory());
    }
}
