package com.guang.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.result.CommonResult;
import com.guang.integration.domain.vo.UserNotificationVO;
import com.guang.integration.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "C端通知中心")
@RestController
@RequestMapping("/api/portal/v1/notification")
@RequiredArgsConstructor
public class ClientNotificationController {

    private final UserNotificationService userNotificationService;

    @Operation(summary = "分页查询我的通知")
    @GetMapping("/page")
    public CommonResult<Page<UserNotificationVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Byte isRead) {
        return CommonResult.success(userNotificationService.pageMyNotifications(pageNum, pageSize, isRead));
    }

    @Operation(summary = "标记通知已读")
    @PutMapping("/read/{id}")
    public CommonResult<Boolean> read(@PathVariable Long id) {
        return CommonResult.success(userNotificationService.markAsRead(id));
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public CommonResult<Boolean> readAll() {
        return CommonResult.success(userNotificationService.markAllRead());
    }

    @Operation(summary = "获取未读通知数量")
    @GetMapping("/unread-count")
    public CommonResult<Long> unreadCount() {
        return CommonResult.success(userNotificationService.getUnreadCount());
    }
}
