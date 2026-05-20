package com.guang.finance.mapper;

import com.guang.finance.domain.vo.MemberMonthlyPerformanceVO;
import com.guang.finance.domain.vo.ProjectReportVO;
import com.guang.finance.domain.vo.SalesReportVO;
import com.guang.finance.domain.vo.TeamPerformanceVO;
import com.guang.finance.domain.vo.TrendReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 财务报表统计 Mapper
 *
 * @author blackDuck
 */
@Mapper
public interface FinanceReportMapper {

    /**
     * 统计项目维度报表
     */
    List<ProjectReportVO> selectProjectReport();

    /**
     * 统计销售维度报表
     */
    List<SalesReportVO> selectSalesReport();

    /**
     * 统计近N天的收支趋势
     * @param days 天数
     */
    List<TrendReportVO> selectTrendReport(@Param("days") Integer days);

    /**
     * 查询指定成员在指定月份的成交业绩
     * @param userIds 用户ID列表
     * @param month 月份 YYYY-MM
     */
    List<MemberMonthlyPerformanceVO> selectMemberMonthlyPerformance(@Param("userIds") List<Integer> userIds, @Param("month") String month);

    /**
     * 查询近6个月月度成交趋势
     */
    List<TeamPerformanceVO.MonthlyTrendItem> selectMonthlyTrend();

    /**
     * 查询房源类型分布
     */
    List<TeamPerformanceVO.HouseTypeDistItem> selectHouseTypeDistribution();

    /**
     * 查询客源转化漏斗
     */
    TeamPerformanceVO.FunnelData selectCustomerFunnel();

    /**
     * 查询顶部概览统计
     */
    TeamPerformanceVO.SummaryStats selectSummaryStats();
}
