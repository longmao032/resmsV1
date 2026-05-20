package com.guang.resms.config;

import com.guang.common.security.JwtAuthenticationTokenFilter;
import com.guang.common.security.RestAuthenticationEntryPoint;
import com.guang.common.security.RestfulAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6.x 核心配置 (Lambda DSL)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestfulAccessDeniedHandler restfulAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. 禁用 CSRF
            .csrf(AbstractHttpConfigurer::disable)
            // 2. 开启 CORS
            .cors(Customizer.withDefaults())
            // 3. 设置 Session 策略为 STATELESS
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 4. 鉴权规则配置
            .authorizeHttpRequests(auth -> auth
                // 放行B端登录接口
                .requestMatchers("/api/system/v1/auth/login", "/api/system/v1/auth/register").permitAll()
                // 放行C端登录/注册接口（portal模块）
                .requestMatchers("/api/portal/v1/auth/login", "/api/portal/v1/auth/register").permitAll()
                // 放行C端房源浏览（公开数据，portal模块）
                .requestMatchers(HttpMethod.GET, "/api/portal/v1/houses/**").permitAll()
                // 放行C端楼盘浏览（公开数据，portal模块）
                .requestMatchers(HttpMethod.GET, "/api/portal/v1/projects/**").permitAll()
                // 放行旧C端路径（兼容过渡期，后续移除）
                .requestMatchers("/api/client/v1/auth/login", "/api/client/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/client/v1/houses/**").permitAll()
                // 放行静态资源、私有文件下载流与 WebSocket
                .requestMatchers(HttpMethod.GET, "/", "/*.html", "/**/*.html", "/**/*.css", "/**/*.js", "/api/profile/**", "/api/v1/common/download").permitAll()
                // 放行 WebSocket 端点
                .requestMatchers("/api/ws/**").permitAll()
                // 放行 SpringDoc 相关路径 (Swagger UI, OpenAPI Docs)
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml", "/v3/api-docs-yml", "/swagger-resources/**").permitAll()
                // 其他所有接口需认证
                .anyRequest().authenticated()
            )
            // 5. 异常处理
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restfulAccessDeniedHandler)
            )
            // 6. 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
