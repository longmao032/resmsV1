package com.guang.system.controller;

import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.system.domain.dto.DeptQueryDTO;
import com.guang.system.domain.dto.DeptSaveDTO;
import com.guang.system.domain.vo.DeptTreeVO;
import com.guang.system.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 部门管理 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Tag(name = "部门管理", description = "系统部门相关接口")
@RestController
@RequestMapping("/api/system/v1/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "获取部门树")
    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('system:dept:query', 'team:user')")
    public CommonResult<List<DeptTreeVO>> tree(DeptQueryDTO queryDTO) {
        return CommonResult.success(deptService.getDeptTree(queryDTO));
    }

    @Operation(summary = "新增部门")
    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:add')")
    @Log(title = "部门管理", businessType = "DEPT", operatorType = "ADD")
    public CommonResult<Boolean> save(@RequestBody DeptSaveDTO saveDTO) {
        return CommonResult.success(deptService.saveDept(saveDTO));
    }

    @Operation(summary = "更新部门")
    @PutMapping
    @PreAuthorize("hasAuthority('system:dept:edit')")
    @Log(title = "部门管理", businessType = "DEPT", operatorType = "UPDATE")
    public CommonResult<Boolean> update(@RequestBody DeptSaveDTO saveDTO) {
        return CommonResult.success(deptService.saveDept(saveDTO));
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    @Log(title = "部门管理", businessType = "DEPT", operatorType = "DELETE")
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(deptService.deleteDept(id));
    }
}
