package com.guang.resmsaiservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.result.CommonResult;
import com.guang.resmsaiservice.service.AiCustomerService;
import com.guang.resmsaiservice.service.AiProjectService;
import com.guang.resmsaiservice.vo.ProjectVo;
import com.guang.resmsaiservice.vo.UserIntentProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI 服务专用接口")
@RestController
@RequestMapping("/api/ai/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('rbot')")
public class AiServiceController {

    private final AiProjectService aiProjectService;
    private final AiCustomerService aiCustomerService;

    @Operation(summary = "获取全量房产信息（供 AI 知识库同步使用）")
    @GetMapping("/projects/list-all")
    public CommonResult<List<ProjectVo>> listAll() {
        return CommonResult.success(aiProjectService.listAllProjectsWithHouses());
    }



    @Operation(summary = "获取用户意向画像（含行为权重评分、特征聚合、区县均价指数）")
    @GetMapping("/customer/profile")
    public CommonResult<UserIntentProfileVO> getUserIntentProfile(@RequestParam Long userId) {
        return CommonResult.success(aiCustomerService.getUserIntentProfile(userId));
    }
}
