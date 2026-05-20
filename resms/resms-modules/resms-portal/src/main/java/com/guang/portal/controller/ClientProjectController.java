package com.guang.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.result.CommonResult;
import com.guang.house.domain.dto.ProjectQueryDTO;
import com.guang.house.domain.vo.ProjectPageVO;
import com.guang.house.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "楼盘浏览（C端）")
@RestController
@RequestMapping("/api/portal/v1/projects")
@RequiredArgsConstructor
public class ClientProjectController {

    private final ProjectService projectService;

    @Operation(summary = "分页查询在售新房楼盘")
    @GetMapping("/page")
    public CommonResult<Page<ProjectPageVO>> page(ProjectQueryDTO queryDTO) {
        queryDTO.setProjectType((byte) 1);
        queryDTO.setStatus((byte) 1);
        return CommonResult.success(projectService.pageProjectsForPortal(queryDTO));
    }

    @Operation(summary = "获取楼盘详情")
    @GetMapping("/{id}")
    public CommonResult<ProjectPageVO> detail(@PathVariable Integer id) {
        return CommonResult.success(projectService.getProjectDetailForPortal(id));
    }
}
