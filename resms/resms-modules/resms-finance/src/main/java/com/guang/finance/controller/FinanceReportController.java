package com.guang.finance.controller;

import com.guang.common.result.CommonResult;
import com.guang.finance.domain.vo.MemberMonthlyPerformanceVO;
import com.guang.finance.domain.vo.ProjectReportVO;
import com.guang.finance.domain.vo.SalesReportVO;
import com.guang.finance.domain.vo.TeamPerformanceVO;
import com.guang.finance.domain.vo.TrendReportVO;
import com.guang.finance.service.FinanceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 财务报表控制器
 *
 * @author blackDuck
 */
@RestController
@RequestMapping("/api/finance/v1/reports")
@Tag(name = "经营报表")
@RequiredArgsConstructor
public class FinanceReportController {

    private final FinanceReportService financeReportService;

    @Operation(summary = "楼盘维度统计报表")
    @GetMapping("/project")
    @PreAuthorize("hasAuthority('finance:report:project')")
    public CommonResult<List<ProjectReportVO>> projectReport() {
        return CommonResult.success(financeReportService.getProjectReport());
    }

    @Operation(summary = "销售业绩统计报表")
    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('finance:report:sales')")
    public CommonResult<List<SalesReportVO>> salesReport() {
        return CommonResult.success(financeReportService.getSalesReport());
    }

    @Operation(summary = "资金收支趋势报表")
    @GetMapping("/trend")
    @PreAuthorize("hasAuthority('finance:report:trend')")
    public CommonResult<List<TrendReportVO>> trendReport(@RequestParam(defaultValue = "30") Integer days) {
        return CommonResult.success(financeReportService.getTrendReport(days));
    }

    @Operation(summary = "成员月度业绩")
    @GetMapping("/member-performance")
    @PreAuthorize("hasAnyAuthority('finance:report:sales', 'team:user', 'team:performance')")
    public CommonResult<List<MemberMonthlyPerformanceVO>> memberPerformance(
            @RequestParam List<Integer> userIds, @RequestParam String month) {
        return CommonResult.success(financeReportService.getMemberMonthlyPerformance(userIds, month));
    }

    @Operation(summary = "团队业绩看板")
    @GetMapping("/team-performance")
    @PreAuthorize("hasAnyAuthority('finance:report:sales', 'team:user', 'team:performance')")
    public CommonResult<TeamPerformanceVO> teamPerformance() {
        return CommonResult.success(financeReportService.getTeamPerformance());
    }
}
