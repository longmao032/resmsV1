package com.guang.finance.service.impl;

import com.guang.finance.domain.vo.MemberMonthlyPerformanceVO;
import com.guang.finance.domain.vo.ProjectReportVO;
import com.guang.finance.domain.vo.SalesReportVO;
import com.guang.finance.domain.vo.TeamPerformanceVO;
import com.guang.finance.domain.vo.TrendReportVO;
import com.guang.finance.mapper.FinanceReportMapper;
import com.guang.finance.service.FinanceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 财务报表服务实现类
 *
 * @author blackDuck
 */
@Service
@RequiredArgsConstructor
public class FinanceReportServiceImpl implements FinanceReportService {

    private final FinanceReportMapper financeReportMapper;

    @Override
    public List<ProjectReportVO> getProjectReport() {
        return financeReportMapper.selectProjectReport();
    }

    @Override
    public List<SalesReportVO> getSalesReport() {
        return financeReportMapper.selectSalesReport();
    }

    @Override
    public List<TrendReportVO> getTrendReport(Integer days) {
        if (days == null || days <= 0) {
            days = 30;
        }
        return financeReportMapper.selectTrendReport(days);
    }

    @Override
    public List<MemberMonthlyPerformanceVO> getMemberMonthlyPerformance(List<Integer> userIds, String month) {
        return financeReportMapper.selectMemberMonthlyPerformance(userIds, month);
    }

    @Override
    public TeamPerformanceVO getTeamPerformance() {
        TeamPerformanceVO vo = new TeamPerformanceVO();

        // 顶部概览
        TeamPerformanceVO.SummaryStats summary = financeReportMapper.selectSummaryStats();
        if (summary != null) {
            summary.setTargetRate(new java.math.BigDecimal("68.5")); // 目标达成率由业务层定义
        }
        vo.setSummary(summary != null ? summary : new TeamPerformanceVO.SummaryStats());

        // 月度趋势
        vo.setMonthlyTrend(financeReportMapper.selectMonthlyTrend());

        // 销售排行
        vo.setSalesRanking(financeReportMapper.selectSalesReport());

        // 房源类型分布
        List<TeamPerformanceVO.HouseTypeDistItem> dist = financeReportMapper.selectHouseTypeDistribution();
        if (dist != null) {
            int total = dist.stream().mapToInt(TeamPerformanceVO.HouseTypeDistItem::getValue).sum();
            // 计算百分比并赋予颜色
            String[] colors = {"#1a73e8", "#34a853", "#fbbc04", "#ea4335"};
            for (int i = 0; i < dist.size() && i < colors.length; i++) {
                TeamPerformanceVO.HouseTypeDistItem item = dist.get(i);
                int pct = total > 0 ? (int) Math.round(item.getValue() * 100.0 / total) : 0;
                item.setValue(pct);
                item.setColor(colors[i]);
            }
        }
        vo.setHouseTypeDistribution(dist);

        // 客源转化漏斗
        TeamPerformanceVO.FunnelData funnel = financeReportMapper.selectCustomerFunnel();
        if (funnel == null) {
            funnel = new TeamPerformanceVO.FunnelData();
        }
        vo.setFunnel(funnel);

        return vo;
    }
}
