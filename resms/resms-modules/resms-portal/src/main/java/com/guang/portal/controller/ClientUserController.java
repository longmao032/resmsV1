package com.guang.portal.controller;

import com.guang.common.result.CommonResult;
import com.guang.portal.domain.dto.UpdatePasswordDTO;
import com.guang.portal.domain.dto.UpdateProfileDTO;
import com.guang.portal.domain.vo.UserProfileVO;
import com.guang.portal.service.ClientUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "C端用户中心")
@RestController
@RequestMapping("/api/portal/v1/user")
@RequiredArgsConstructor
public class ClientUserController {

    private final ClientUserService clientUserService;

    @Operation(summary = "获取个人资料（含统计数据）")
    @GetMapping("/profile")
    public CommonResult<UserProfileVO> getProfile() {
        return CommonResult.success(clientUserService.getProfile());
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/profile")
    public CommonResult<String> updateProfile(@RequestBody UpdateProfileDTO dto) {
        clientUserService.updateProfile(dto);
        return CommonResult.success("更新成功");
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public CommonResult<String> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        clientUserService.updatePassword(dto);
        return CommonResult.success("修改成功");
    }
}
