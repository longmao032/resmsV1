package com.guang.house.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.house.domain.dto.ProjectQueryDTO;
import com.guang.house.domain.dto.ProjectSaveDTO;
import com.guang.house.domain.vo.ProjectStatisticsVO;
import com.guang.house.domain.vo.ProjectVO;
import com.guang.house.entity.Project;
import com.guang.house.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 楼盘项目管理 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Tag(name = "楼盘项目", description = "楼盘项目相关接口")
@RestController
@RequestMapping("/api/system/house/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "分页查询项目")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('house:project:list')")
    public CommonResult<Page<ProjectVO>> list(ProjectQueryDTO queryDTO) {
        return CommonResult.success(projectService.pageProjects(queryDTO));
    }

    @Operation(summary = "新增/修改项目")
    @PostMapping
    @PreAuthorize("hasAuthority('house:project:save')")
    @Log(title = "楼盘项目", businessType = "PROJECT", operatorType = "SAVE")
    public CommonResult<Boolean> save(@RequestBody ProjectSaveDTO saveDTO) {
        return CommonResult.success(projectService.saveProject(saveDTO));
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('house:project:delete')")
    @Log(title = "楼盘项目", businessType = "PROJECT", operatorType = "DELETE")
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(projectService.deleteProject(id));
    }

    @Operation(summary = "获取项目详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('house:project:query')")
    public CommonResult<ProjectVO> getDetail(@PathVariable Integer id) {
        return CommonResult.success(projectService.getProjectDetail(id));
    }

    @Operation(summary = "导出项目列表")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('house:project:export')")
    public void export(ProjectQueryDTO queryDTO, HttpServletResponse response) {
        projectService.exportProjects(queryDTO, response);
    }

    @Operation(summary = "项目统计概览（顶部看板）")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('house:project:list')")
    public CommonResult<ProjectStatisticsVO> statistics() {
        return CommonResult.success(projectService.getStatistics());
    }
}
