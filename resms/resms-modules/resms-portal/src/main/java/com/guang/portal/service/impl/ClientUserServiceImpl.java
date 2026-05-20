package com.guang.portal.service.impl;


import com.guang.common.exception.ApiException;
import com.guang.common.util.SecurityUtils;
import com.guang.portal.domain.dto.UpdatePasswordDTO;
import com.guang.portal.domain.dto.UpdateProfileDTO;
import com.guang.portal.domain.vo.UserProfileVO;
import com.guang.portal.service.ClientUserService;
import com.guang.trade.entity.AppUser;
import com.guang.trade.entity.Customer;
import com.guang.trade.entity.CustomerFollowUpRecord;
import com.guang.trade.entity.UserBrowseHistory;
import com.guang.trade.entity.UserFavorite;
import com.guang.trade.service.AppUserService;
import com.guang.trade.service.CustomerFollowUpRecordService;
import com.guang.trade.service.CustomerService;
import com.guang.trade.service.UserBrowseHistoryService;
import com.guang.trade.service.UserFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientUserServiceImpl implements ClientUserService {

    private final AppUserService appUserService;
    private final UserFavoriteService userFavoriteService;
    private final CustomerFollowUpRecordService followUpService;
    private final CustomerService customerService;
    private final UserBrowseHistoryService browseHistoryService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileVO getProfile() {
        Long appUserId = SecurityUtils.getAppUserId();
        if (appUserId == null) {
            throw new ApiException("未登录或用户类型不匹配");
        }

        AppUser appUser = appUserService.getById(appUserId);
        if (appUser == null) {
            throw new ApiException("用户不存在");
        }

        Integer favoriteCount = countFavorites(appUserId);
        Integer appointmentCount = countAppointments(appUserId);
        Integer browseCount = countBrowseHistory(appUserId);

        return UserProfileVO.builder()
                .userId(appUser.getId())
                .phone(appUser.getPhone())
                .nickname(appUser.getNickname())
                .avatarUrl(appUser.getAvatarUrl())
                .gender(appUser.getGender() != null ? appUser.getGender().intValue() : 0)
                .email(appUser.getEmail())
                .createTime(appUser.getCreateTime() != null
                        ? appUser.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : null)
                .favoriteCount(favoriteCount)
                .appointmentCount(appointmentCount)
                .browseCount(browseCount)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UpdateProfileDTO dto) {
        Long appUserId = SecurityUtils.getAppUserId();
        if (appUserId == null) {
            throw new ApiException("未登录");
        }

        AppUser appUser = new AppUser();
        appUser.setId(appUserId);

        if (dto.getNickname() != null) {
            appUser.setNickname(dto.getNickname());
        }
        if (dto.getAvatarUrl() != null) {
            appUser.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getGender() != null) {
            appUser.setGender(dto.getGender().byteValue());
        }
        if (dto.getEmail() != null) {
            appUser.setEmail(dto.getEmail());
        }
        appUser.setUpdateTime(LocalDateTime.now());

        appUserService.updateById(appUser);
        log.info("[C端用户] 用户 {} 更新个人资料", appUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UpdatePasswordDTO dto) {
        Long appUserId = SecurityUtils.getAppUserId();
        if (appUserId == null) {
            throw new ApiException("未登录");
        }

        AppUser appUser = appUserService.getById(appUserId);
        if (appUser == null) {
            throw new ApiException("用户不存在");
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), appUser.getPassword())) {
            throw new ApiException("原密码错误");
        }

        AppUser update = new AppUser();
        update.setId(appUserId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        update.setUpdateTime(LocalDateTime.now());
        appUserService.updateById(update);

        log.info("[C端用户] 用户 {} 修改密码成功", appUserId);
    }

    private Integer countFavorites(Long appUserId) {
        long count = userFavoriteService.lambdaQuery()
                .eq(UserFavorite::getAppUserId, appUserId)
                .eq(UserFavorite::getTargetType, (byte) 1)
                .count();
        return (int) count;
    }

    private Integer countAppointments(Long appUserId) {
        Customer customer = customerService.lambdaQuery()
                .eq(Customer::getAppUserId, appUserId)
                .eq(Customer::getIsDeleted, 0)
                .one();

        if (customer == null) {
            return 0;
        }

        long count = followUpService.lambdaQuery()
                .eq(CustomerFollowUpRecord::getCustomerId, customer.getId())
                .eq(CustomerFollowUpRecord::getAppointType, (byte) 2)
                .eq(CustomerFollowUpRecord::getIsDeleted, 0)
                .count();
        return (int) count;
    }

    private Integer countBrowseHistory(Long appUserId) {
        long count = browseHistoryService.lambdaQuery()
                .eq(UserBrowseHistory::getAppUserId, appUserId)
                .count();
        return (int) count;
    }
}
