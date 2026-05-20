package com.guang.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.common.util.SecurityUtils;
import com.guang.system.domain.dto.PasswordChangeDTO;
import com.guang.system.domain.dto.ProfileEditDTO;
import com.guang.system.domain.dto.UserQueryDTO;
import com.guang.system.domain.dto.UserSaveDTO;
import com.guang.system.domain.vo.UserVO;
import com.guang.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 系统用户表 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Tag(name = "用户管理", description = "系统用户相关接口")
@RestController
@RequestMapping("/api/system/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('system:user:query', 'team:user')")
    public CommonResult<Page<UserVO>> list(UserQueryDTO queryDTO) {
        return CommonResult.success(userService.pageUsers(queryDTO));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    @Log(title = "用户管理", businessType = "USER", operatorType = "ADD")
    public CommonResult<Boolean> save(@RequestBody UserSaveDTO saveDTO) {
        return CommonResult.success(userService.saveUser(saveDTO));
    }

    @Operation(summary = "更新用户")
    @PutMapping
    @PreAuthorize("hasAuthority('system:user:edit')")
    @Log(title = "用户管理", businessType = "USER", operatorType = "UPDATE")
    public CommonResult<Boolean> update(@RequestBody UserSaveDTO saveDTO) {
        return CommonResult.success(userService.saveUser(saveDTO));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @Log(title = "用户管理", businessType = "USER", operatorType = "DELETE")
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(userService.deleteUser(id));
    }

    @Operation(summary = "修改状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @Log(title = "用户管理", businessType = "USER", operatorType = "STATUS")
    public CommonResult<Boolean> updateStatus(@PathVariable Integer id, @RequestParam Byte status) {
        return CommonResult.success(userService.updateStatus(id, status));
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:password')")
    @Log(title = "用户管理", businessType = "USER", operatorType = "PASSWORD", description = "重置用户密码")
    public CommonResult<Boolean> resetPassword(@PathVariable Integer id, @RequestParam String password) {
        return CommonResult.success(userService.resetPassword(id, password));
    }

    @Operation(summary = "获取销售人员选项列表（下拉框用）")
    @GetMapping("/sales-options")
    public CommonResult<java.util.List<com.guang.system.domain.vo.SalesOptionVO>> salesOptions(@RequestParam(required = false) String realName) {
        return CommonResult.success(userService.listSalesOptions(realName));
    }

    @Operation(summary = "获取指定部门下的用户列表（下拉框用）")
    @GetMapping("/by-dept")
    @PreAuthorize("hasAnyAuthority('message:notice:add', 'team:user')")
    public CommonResult<java.util.List<com.guang.system.domain.vo.SalesOptionVO>> listByDept(@RequestParam Integer deptId) {
        return CommonResult.success(userService.listByDept(deptId));
    }

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public CommonResult<UserVO> profile() {
        Integer userId = SecurityUtils.getUserId();
        return CommonResult.success(userService.getProfile(userId));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    @Log(title = "用户管理", businessType = "USER", operatorType = "UPDATE", description = "修改个人信息")
    public CommonResult<Boolean> updateProfile(@RequestBody ProfileEditDTO editDTO) {
        Integer userId = SecurityUtils.getUserId();
        return CommonResult.success(userService.updateProfile(userId, editDTO));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/profile/password")
    @Log(title = "用户管理", businessType = "USER", operatorType = "PASSWORD", description = "修改个人密码")
    public CommonResult<Boolean> changePassword(@Valid @RequestBody PasswordChangeDTO dto) {
        Integer userId = SecurityUtils.getUserId();
        return CommonResult.success(userService.changePassword(userId, dto));
    }
}
