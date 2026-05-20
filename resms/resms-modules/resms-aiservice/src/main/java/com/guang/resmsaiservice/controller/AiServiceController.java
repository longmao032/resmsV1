package com.guang.resmsaiservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.result.CommonResult;
import com.guang.portal.domain.vo.BrowseHistoryItemVO;
import com.guang.portal.domain.vo.FavoriteItemVO;
import com.guang.resmsaiservice.service.AiCustomerService;
import com.guang.resmsaiservice.service.AiProjectService;
import com.guang.resmsaiservice.vo.ProjectVo;
import com.guang.resmsaiservice.vo.UserIntentProfileVO;
import com.guang.trade.domain.vo.FollowUpVO;
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

    @Operation(summary = "分页获取指定用户的收藏列表（供 AI 分析偏好）")
    @GetMapping("/customer/favorites")
    public CommonResult<Page<FavoriteItemVO>> pageFavorites(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(aiCustomerService.pageFavorites(userId, pageNum, pageSize));
    }

    @Operation(summary = "分页获取指定用户的浏览记录（供 AI 分析偏好）")
    @GetMapping("/customer/history")
    public CommonResult<Page<BrowseHistoryItemVO>> pageHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Byte resourceType) {
        return CommonResult.success(aiCustomerService.pageHistory(userId, pageNum, pageSize, resourceType));
    }

    @Operation(summary = "分页查询指定用户的预约（供 AI 提醒/查询）")
    @GetMapping("/customer/appointments")
    public CommonResult<Page<FollowUpVO>> pageMyAppointments(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(aiCustomerService.pageMyAppointments(userId, pageNum, pageSize));
    }

    @Operation(summary = "获取用户意向画像（含行为权重评分、特征聚合、区县均价指数）")
    @GetMapping("/customer/profile")
    public CommonResult<UserIntentProfileVO> getUserIntentProfile(@RequestParam Long userId) {
        return CommonResult.success(aiCustomerService.getUserIntentProfile(userId));
    }
}
