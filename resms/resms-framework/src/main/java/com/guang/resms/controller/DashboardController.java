package com.guang.resms.controller;

import com.guang.common.result.CommonResult;
import com.guang.resms.domain.vo.DashboardActivityVO;
import com.guang.resms.domain.vo.DashboardStatsVO;
import com.guang.resms.domain.vo.DashboardTrendVO;
import com.guang.resms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "仪表盘", description = "首页仪表盘相关接口")
@RestController
@RequestMapping("/api/system/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取核心统计数据")
    @GetMapping("/stats")
    public CommonResult<DashboardStatsVO> getStats() {
        return CommonResult.success(dashboardService.getStats());
    }

    @Operation(summary = "获取经营走势数据")
    @GetMapping("/trend")
    public CommonResult<List<DashboardTrendVO>> getTrend(@RequestParam(defaultValue = "7") int days) {
        return CommonResult.success(dashboardService.getTrend(days));
    }

    @Operation(summary = "获取最新动态")
    @GetMapping("/activities")
    public CommonResult<List<DashboardActivityVO>> getActivities() {
        return CommonResult.success(dashboardService.getActivities());
    }
}
