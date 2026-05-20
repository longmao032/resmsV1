package com.guang.system.service;

import com.guang.common.exception.ApiException;
import com.guang.common.security.JwtProperties;
import com.guang.common.security.JwtTokenUtil;
import com.guang.common.security.LoginUser;
import com.guang.common.security.RedisService;
import com.guang.system.domain.dto.LoginDTO;
import com.guang.system.domain.vo.LoginVO;
import com.guang.system.domain.vo.UserVO;
import com.guang.system.entity.User;
import com.guang.system.security.AdminUserDetails;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证业务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService; // 使用 RedisService

    /**
     * 登录功能
     */
    public LoginVO login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        // 1. 加载用户
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!(userDetails instanceof AdminUserDetails adminUserDetails)) {
            throw new ApiException("用户不存在");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new ApiException("用户名或密码错误");
        }

        // 3. 校验账号状态
        if (!userDetails.isEnabled()) {
            throw new ApiException("账号已被禁用");
        }

        // 4. 封装缓存对象并存入 Redis (key: login:{userId})
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(adminUserDetails.getUser().getId());
        loginUser.setUsername(adminUserDetails.getUsername());
        loginUser.setNickName(adminUserDetails.getUser().getNickName());
        loginUser.setRealName(adminUserDetails.getUser().getRealName());
        loginUser.setPassword(adminUserDetails.getPassword());
        loginUser.setDeptId(adminUserDetails.getUser().getDeptId());
        loginUser.setDataScope(adminUserDetails.getHighestDataScope());
        // 获取权限列表
        loginUser.setPermissions(adminUserDetails.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .toList());

        String redisKey = "login:" + loginUser.getUserId();
        // 使用 RedisService 保存，时间与 Token 过期时间一致
        redisService.set(redisKey, loginUser, jwtProperties.getExpiration());

        // 5. 生成 Token (包含 userId)
        String token = jwtTokenUtil.generateToken(loginUser.getUserId(), username);

        // 6. 设置上下文
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 7. 封装返回对象
        User user = adminUserDetails.getUser();
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        userVO.setDataScope(adminUserDetails.getHighestDataScope());
        
        return LoginVO.builder()
                .token(token)
                .tokenHead(jwtProperties.getTokenHead())
                .username(username)
                .user(userVO)
                .build();
    }

    /**
     * 登出功能
     */
    public void logout() {
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            String redisKey = "login:" + loginUser.getUserId();
            redisService.del(redisKey);
        }
        SecurityContextHolder.clearContext();
    }
}
