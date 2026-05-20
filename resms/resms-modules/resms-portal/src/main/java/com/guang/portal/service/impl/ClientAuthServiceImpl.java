package com.guang.portal.service.impl;

import com.guang.common.exception.ApiException;
import com.guang.common.security.JwtProperties;
import com.guang.common.security.JwtTokenUtil;
import com.guang.common.security.LoginUser;
import com.guang.common.security.RedisService;
import com.guang.portal.domain.dto.ClientLoginDTO;
import com.guang.portal.domain.dto.ClientRegisterDTO;
import com.guang.portal.domain.vo.ClientLoginVO;
import com.guang.portal.service.ClientAuthService;
import com.guang.trade.entity.AppUser;
import com.guang.trade.service.AppUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAuthServiceImpl implements ClientAuthService {

    private final AppUserService appUserService;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;

    private static final String USER_TYPE_APP = "APP";

    @Override
    public ClientLoginVO register(ClientRegisterDTO dto) {
        AppUser appUser = appUserService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AppUser>()
                        .eq(AppUser::getPhone, dto.getPhone())
        );
        if (appUser != null) {
            return login(dto.getPhone(), dto.getPassword());
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        appUser = appUserService.register(dto.getPhone(), encodedPassword, dto.getNickname(), dto.getAvatarUrl());

        return buildLoginVO(appUser);
    }

    @Override
    public ClientLoginVO login(ClientLoginDTO dto) {
        return login(dto.getPhone(), dto.getPassword());
    }

    private ClientLoginVO login(String phone, String password) {
        AppUser appUser = appUserService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AppUser>()
                        .eq(AppUser::getPhone, phone)
        );
        if (appUser == null) {
            throw new ApiException("用户不存在");
        }

        if (passwordEncoder.matches(password, appUser.getPassword())) {
            // BCrypt 匹配成功
        } else if (password.equals(appUser.getPassword())) {
            appUser.setPassword(passwordEncoder.encode(password));
            appUserService.updateById(appUser);
            log.info("[C端登录] 用户 {} 的密码已自动升级为加密存储", phone);
        } else {
            throw new ApiException("手机号或密码错误");
        }

        if (appUser.getStatus() != null && appUser.getStatus() == 0) {
            throw new ApiException("账号已被封禁");
        }

        return buildLoginVO(appUser);
    }

    @Override
    public void logout() {
        LoginUser loginUser = com.guang.common.util.SecurityUtils.getLoginUser();
        if (loginUser != null && loginUser.isAppUser()) {
            String redisKey = "app_login:" + loginUser.getUserId();
            redisService.del(redisKey);
        }
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private ClientLoginVO buildLoginVO(AppUser appUser) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(appUser.getId().intValue());
        loginUser.setUsername(appUser.getPhone());
        loginUser.setNickName(appUser.getNickname());
        loginUser.setAvatar(appUser.getAvatarUrl());
        loginUser.setPassword(appUser.getPassword());
        loginUser.setUserType(USER_TYPE_APP);
        loginUser.setPermissions(List.of());

        String redisKey = "app_login:" + appUser.getId();
        redisService.set(redisKey, loginUser, jwtProperties.getExpiration());

        String token = jwtTokenUtil.generateToken(
                appUser.getId().intValue(),
                appUser.getPhone(),
                USER_TYPE_APP
        );

        return ClientLoginVO.builder()
                .token(token)
                .tokenHead(jwtProperties.getTokenHead())
                .userId(appUser.getId())
                .phone(appUser.getPhone())
                .nickname(appUser.getNickname())
                .avatarUrl(appUser.getAvatarUrl())
                .build();
    }
}
