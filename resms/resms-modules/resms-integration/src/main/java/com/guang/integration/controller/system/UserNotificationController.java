package com.guang.integration.controller.system;

import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.integration.domain.vo.UserNotificationVO;
import com.guang.integration.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户通知关联表 前端控制器 (用户端)
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Tag(name = "用户个人收件箱")
@RestController
@RequestMapping("/api/system/v1/user-notification")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    @Operation(summary = "分页查询我的通知列表")
    @GetMapping("/my-page")
    public CommonResult<Page<UserNotificationVO>> myPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         @RequestParam(required = false) Byte isRead) {
        return CommonResult.success(userNotificationService.pageMyNotifications(pageNum, pageSize, isRead));
    }

    @Operation(summary = "标记为已读")
    @PutMapping("/read/{id}")
    @Log(title = "用户通知", businessType = "USER_NOTIFICATION", operatorType = "UPDATE")
    public CommonResult<Boolean> read(@PathVariable Long id) {
        return CommonResult.success(userNotificationService.markAsRead(id));
    }

    @Operation(summary = "全部标记为已读")
    @PutMapping("/read-all")
    @Log(title = "用户通知", businessType = "USER_NOTIFICATION", operatorType = "UPDATE")
    public CommonResult<Boolean> readAll() {
        return CommonResult.success(userNotificationService.markAllRead());
    }

    @Operation(summary = "获取未读通知数量")
    @GetMapping("/unread-count")
    public CommonResult<Long> unreadCount() {
        return CommonResult.success(userNotificationService.getUnreadCount());
    }
}
