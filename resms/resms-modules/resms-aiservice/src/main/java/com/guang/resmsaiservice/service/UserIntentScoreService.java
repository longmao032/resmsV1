package com.guang.resmsaiservice.service;

import com.guang.resmsaiservice.vo.UserIntentProfileVO;

/**
 * 用户意向评分与画像特征提取服务
 */
public interface UserIntentScoreService {

    /**
     * 基于用户 30 天内的浏览/收藏/预约行为，计算用户意向画像
     *
     * @param appUserId C端用户ID
     * @return 结构化的用户意向画像（含单价锚点、区县均价指数）
     */
    UserIntentProfileVO calculateUserProfile(Long appUserId);
}
