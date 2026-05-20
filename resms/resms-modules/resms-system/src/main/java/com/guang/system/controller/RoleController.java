package com.guang.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.system.domain.dto.RoleQueryDTO;
import com.guang.system.domain.dto.RoleSaveDTO;
import com.guang.system.entity.Role;
import com.guang.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 角色管理 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Tag(name = "角色管理", description = "系统角色相关接口")
@RestController
@RequestMapping("/api/system/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "查询所有角色 (下拉列表用)")
    @GetMapping("/all")
    public CommonResult<List<Role>> all() {
        return CommonResult.success(roleService.list());
    }

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:role:query')")
    public CommonResult<Page<Role>> list(RoleQueryDTO queryDTO) {
        return CommonResult.success(roleService.pageRoles(queryDTO));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    @Log(title = "角色管理", businessType = "ROLE", operatorType = "ADD")
    public CommonResult<Boolean> save(@RequestBody RoleSaveDTO saveDTO) {
        return CommonResult.success(roleService.saveRole(saveDTO));
    }

    @Operation(summary = "更新角色")
    @PutMapping
    @PreAuthorize("hasAuthority('system:role:edit')")
    @Log(title = "角色管理", businessType = "ROLE", operatorType = "UPDATE")
    public CommonResult<Boolean> update(@RequestBody RoleSaveDTO saveDTO) {
        return CommonResult.success(roleService.saveRole(saveDTO));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @Log(title = "角色管理", businessType = "ROLE", operatorType = "DELETE")
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(roleService.deleteRole(id));
    }

    @Operation(summary = "修改状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @Log(title = "角色管理", businessType = "ROLE", operatorType = "STATUS")
    public CommonResult<Boolean> updateStatus(@PathVariable Integer id, @RequestParam Byte status) {
        return CommonResult.success(roleService.updateStatus(id, status));
    }

    @Operation(summary = "获取角色关联的菜单ID")
    @GetMapping("/{id}/menuIds")
    @PreAuthorize("hasAuthority('system:role:query')")
    public CommonResult<List<Integer>> getMenuIds(@PathVariable Integer id) {
        return CommonResult.success(roleService.getRoleMenuIds(id));
    }
}
