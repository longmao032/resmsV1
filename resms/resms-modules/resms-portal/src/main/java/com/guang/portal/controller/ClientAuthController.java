package com.guang.portal.controller;

import com.guang.common.result.CommonResult;
import com.guang.portal.domain.dto.ClientLoginDTO;
import com.guang.portal.domain.dto.ClientRegisterDTO;
import com.guang.portal.domain.vo.ClientLoginVO;
import com.guang.portal.service.ClientAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "C端认证模块")
@RestController
@RequestMapping("/api/portal/v1/auth")
@RequiredArgsConstructor
public class ClientAuthController {

    private final ClientAuthService clientAuthService;

    @Operation(summary = "C端用户注册")
    @PostMapping("/register")
    public CommonResult<ClientLoginVO> register(@Valid @RequestBody ClientRegisterDTO dto) {
        return CommonResult.success(clientAuthService.register(dto));
    }

    @Operation(summary = "C端用户登录")
    @PostMapping("/login")
    public CommonResult<ClientLoginVO> login(@Valid @RequestBody ClientLoginDTO dto) {
        return CommonResult.success(clientAuthService.login(dto));
    }

    @Operation(summary = "C端用户登出")
    @PostMapping("/logout")
    public CommonResult<String> logout() {
        clientAuthService.logout();
        return CommonResult.success("登出成功");
    }
}
