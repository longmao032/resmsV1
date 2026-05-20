package com.guang.system.controller;

import com.guang.common.annotation.Log;
import com.guang.common.security.LoginUser;
import com.guang.common.result.CommonResult;
import com.guang.system.domain.vo.MenuVO;
import com.guang.system.entity.Menu;
import com.guang.system.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理 前端控制器
 */
@Tag(name = "菜单管理", description = "系统菜单相关接口")
@RestController
@RequestMapping("/api/system/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "获取当前用户的动态菜单树")
    @GetMapping("/nav")
    public CommonResult<List<MenuVO>> getNav() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            return CommonResult.unauthorized(null);
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        Integer userId = loginUser.getUserId();
        return CommonResult.success(menuService.getMenuTreeByUserId(userId));
    }

    @Operation(summary = "查询菜单列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:menu:query')")
    public CommonResult<List<MenuVO>> list(Menu query) {
        return CommonResult.success(menuService.getMenuList(query));
    }

    @Operation(summary = "获取菜单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:query')")
    public CommonResult<Menu> getById(@PathVariable("id") Integer id) {
        return CommonResult.success(menuService.getById(id));
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    @Log(title = "菜单管理", businessType = "MENU", operatorType = "ADD")
    public CommonResult<Boolean> add(@RequestBody Menu menu) {
        return CommonResult.success(menuService.save(menu));
    }

    @Operation(summary = "修改菜单")
    @PutMapping
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @Log(title = "菜单管理", businessType = "MENU", operatorType = "UPDATE")
    public CommonResult<Boolean> update(@RequestBody Menu menu) {
        return CommonResult.success(menuService.updateById(menu));
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    @Log(title = "菜单管理", businessType = "MENU", operatorType = "DELETE")
    public CommonResult<Boolean> delete(@PathVariable("id") Integer id) {
        return CommonResult.success(menuService.removeById(id));
    }
}
