package com.guang.system.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.system.domain.dto.DeptTargetSaveDTO;
import com.guang.system.domain.vo.DeptTargetVO;
import com.guang.system.entity.Dept;
import com.guang.system.entity.DeptTarget;
import com.guang.system.service.DeptService;
import com.guang.system.service.DeptTargetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门目标", description = "部门月度目标接口")
@RestController
@RequestMapping("/api/system/v1/dept-targets")
@RequiredArgsConstructor
public class DeptTargetController {

    private final DeptTargetService deptTargetService;
    private final DeptService deptService;

    @Operation(summary = "获取部门月度目标")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:dept:query', 'team:user', 'team:performance')")
    public CommonResult<DeptTargetVO> getTarget(@RequestParam Integer deptId, @RequestParam String month) {
        DeptTarget target = deptTargetService.getOne(
                new LambdaQueryWrapper<DeptTarget>()
                        .eq(DeptTarget::getDeptId, deptId)
                        .eq(DeptTarget::getTargetMonth, month));
        if (target == null) {
            return CommonResult.success(null);
        }
        DeptTargetVO vo = BeanUtil.copyProperties(target, DeptTargetVO.class);
        Dept dept = deptService.getById(deptId);
        if (dept != null) {
            vo.setDeptName(dept.getDeptName());
        }
        return CommonResult.success(vo);
    }

    @Operation(summary = "保存部门月度目标（新增或更新）")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:dept:edit', 'team:user:edit')")
    @Log(title = "部门目标", businessType = "DEPT_TARGET", operatorType = "SAVE")
    public CommonResult<Boolean> saveTarget(@Valid @RequestBody DeptTargetSaveDTO saveDTO) {
        DeptTarget target = BeanUtil.copyProperties(saveDTO, DeptTarget.class);
        return CommonResult.success(deptTargetService.saveOrUpdate(target));
    }
}
