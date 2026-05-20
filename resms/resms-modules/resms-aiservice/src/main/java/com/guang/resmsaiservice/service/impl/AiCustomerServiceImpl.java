package com.guang.resmsaiservice.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.portal.domain.vo.BrowseHistoryItemVO;
import com.guang.portal.domain.vo.FavoriteItemVO;
import com.guang.portal.service.ClientAppointmentService;
import com.guang.portal.service.ClientBrowseHistoryService;
import com.guang.portal.service.ClientFavoriteService;
import com.guang.resmsaiservice.service.AiCustomerService;
import com.guang.resmsaiservice.service.UserIntentScoreService;
import com.guang.resmsaiservice.vo.UserIntentProfileVO;
import com.guang.trade.domain.vo.FollowUpVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiCustomerServiceImpl implements AiCustomerService {

    private final ClientFavoriteService favoriteService;
    private final ClientBrowseHistoryService browseHistoryService;
    private final ClientAppointmentService appointmentService;
    private final UserIntentScoreService userIntentScoreService;

    @Override
    public Page<FavoriteItemVO> pageFavorites(Long userId, Integer pageNum, Integer pageSize) {
        return favoriteService.pageFavorites(userId, pageNum, pageSize);
    }

    @Override
    public Page<BrowseHistoryItemVO> pageHistory(Long userId, Integer pageNum, Integer pageSize, Byte resourceType) {
        return browseHistoryService.pageHistory(userId, pageNum, pageSize, resourceType);
    }

    @Override
    public Page<FollowUpVO> pageMyAppointments(Long userId, Integer pageNum, Integer pageSize) {
        return appointmentService.pageMyAppointments(userId, pageNum, pageSize);
    }

    @Override
    public UserIntentProfileVO getUserIntentProfile(Long appUserId) {
        return userIntentScoreService.calculateUserProfile(appUserId);
    }
}
