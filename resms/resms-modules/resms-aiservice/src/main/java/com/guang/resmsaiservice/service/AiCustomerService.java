package com.guang.resmsaiservice.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.portal.domain.vo.BrowseHistoryItemVO;
import com.guang.portal.domain.vo.FavoriteItemVO;
import com.guang.resmsaiservice.vo.UserIntentProfileVO;
import com.guang.trade.domain.vo.FollowUpVO;

public interface AiCustomerService {

    /**
     * 分页获取指定 C 端用户的收藏列表
     */
    Page<FavoriteItemVO> pageFavorites(Long userId, Integer pageNum, Integer pageSize);
    /**
     * 分页获取指定 C 端用户的浏览记录
     */
    Page<BrowseHistoryItemVO> pageHistory(Long userId, Integer pageNum, Integer pageSize, Byte resourceType);

    /**
     * 分页查询指定用户的预约
     */
    Page<FollowUpVO> pageMyAppointments(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 获取用户意向画像（含行为权重评分、特征聚合、区县均价指数）
     */
    UserIntentProfileVO getUserIntentProfile(Long appUserId);

}
