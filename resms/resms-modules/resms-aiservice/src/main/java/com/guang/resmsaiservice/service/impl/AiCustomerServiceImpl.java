package com.guang.resmsaiservice.service.impl;

import com.guang.resmsaiservice.service.AiCustomerService;
import com.guang.resmsaiservice.service.UserIntentScoreService;
import com.guang.resmsaiservice.vo.UserIntentProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiCustomerServiceImpl implements AiCustomerService {

    private final UserIntentScoreService userIntentScoreService;

    @Override
    public UserIntentProfileVO getUserIntentProfile(Long appUserId) {
        return userIntentScoreService.calculateUserProfile(appUserId);
    }
}
