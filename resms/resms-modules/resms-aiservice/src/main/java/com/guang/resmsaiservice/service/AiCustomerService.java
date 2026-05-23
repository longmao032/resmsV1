package com.guang.resmsaiservice.service;


import com.guang.resmsaiservice.vo.UserIntentProfileVO;

public interface AiCustomerService {

    /**
     * 获取用户意向画像（含行为权重评分、特征聚合、区县均价指数）
     */
    UserIntentProfileVO getUserIntentProfile(Long appUserId);

}
