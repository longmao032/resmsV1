package com.guang.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 核心过滤器 (Jakarta Servlet + Redis 缓存)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final JwtProperties jwtProperties;
    private final RedisService redisService; // 使用 RedisService 替代直接使用 RedisTemplate

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        // 1. 从 Header 提取 Bearer Token
        String authHeader = request.getHeader(jwtProperties.getTokenHeader());
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(jwtProperties.getTokenHead())) {
            chain.doFilter(request, response);
            return;
        }

        String authToken = authHeader.substring(jwtProperties.getTokenHead().length());
        
        try {
            // 2. 解析 Token 获取 userId 和 userType
            Integer userId = jwtTokenUtil.getUserIdFromToken(authToken);
            String userType = jwtTokenUtil.getUserTypeFromToken(authToken);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 3. 根据 userType 选择 Redis key: C端 app_login:{userId}, B端 login:{userId}
                String redisKey = "APP".equals(userType) ? "app_login:" + userId : "login:" + userId;
                LoginUser loginUser = (LoginUser) redisService.get(redisKey);

                if (loginUser != null && !jwtTokenUtil.isTokenExpired(authToken)) {
                    // 4. 验证通过，封装权限信息
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            loginUser, null, loginUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 5. 设置到 SecurityContextHolder
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            log.error("JWT 身份验证失败: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}
