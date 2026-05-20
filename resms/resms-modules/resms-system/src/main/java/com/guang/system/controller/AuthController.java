package com.guang.system.controller;

import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.system.domain.dto.LoginDTO;
import com.guang.system.domain.vo.LoginVO;
import com.guang.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制层
 */
@RestController
@RequestMapping("/api/system/v1/auth")
@Tag(name = "AuthController", description = "认证模块")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录并返回令牌")
    @PostMapping("/login")
    @Log(title = "身份认证", businessType = "AUTH", operatorType = "LOGIN")
    public CommonResult<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = authService.login(loginDTO);
        return CommonResult.success(loginVO);
    }

    @Operation(summary = "登出系统")
    @PostMapping("/logout")
    @Log(title = "身份认证", businessType = "AUTH", operatorType = "LOGOUT")
    public CommonResult<String> logout() {
        authService.logout();
        return CommonResult.success("登出成功");
    }
}
