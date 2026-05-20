package com.guang.house.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.result.CommonResult;
import com.guang.house.domain.dto.ProjectLogQueryDTO;
import com.guang.house.domain.vo.ProjectLogVO;
import com.guang.house.service.ProjectLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 项目变更日志 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-13
 */
@Tag(name = "项目日志", description = "项目变更日志接口")
@RestController
@RequestMapping("/api/system/house/v1/projects/logs")
@RequiredArgsConstructor
public class ProjectLogController {

    private final ProjectLogService projectLogService;

    @Operation(summary = "分页查询项目变更日志")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('house:project:log')")
    public CommonResult<Page<ProjectLogVO>> list(ProjectLogQueryDTO queryDTO) {
        return CommonResult.success(projectLogService.pageLogs(queryDTO));
    }
}
