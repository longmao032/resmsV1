package com.guang.resms.service;

import com.guang.resms.domain.vo.DashboardActivityVO;
import com.guang.resms.domain.vo.DashboardStatsVO;
import com.guang.resms.domain.vo.DashboardTrendVO;

import java.util.List;

public interface DashboardService {

    DashboardStatsVO getStats();

    List<DashboardTrendVO> getTrend(int days);

    List<DashboardActivityVO> getActivities();
}
