package com.guang.finance.service;

import com.guang.finance.domain.vo.MemberMonthlyPerformanceVO;
import com.guang.finance.domain.vo.ProjectReportVO;
import com.guang.finance.domain.vo.SalesReportVO;
import com.guang.finance.domain.vo.TeamPerformanceVO;
import com.guang.finance.domain.vo.TrendReportVO;

import java.util.List;

/**
 * 财务报表服务接口
 *
 * @author blackDuck
 */
public interface FinanceReportService {

    /**
     * 获取项目经营报表
     */
    List<ProjectReportVO> getProjectReport();

    /**
     * 获取销售业绩报表
     */
    List<SalesReportVO> getSalesReport();

    /**
     * 获取收支趋势报表
     * @param days 天数
     */
    List<TrendReportVO> getTrendReport(Integer days);

    /**
     * 获取成员月度业绩
     * @param userIds 用户ID列表
     * @param month 月份 YYYY-MM
     */
    List<MemberMonthlyPerformanceVO> getMemberMonthlyPerformance(List<Integer> userIds, String month);

    /**
     * 获取团队业绩看板数据
     */
    TeamPerformanceVO getTeamPerformance();
}
