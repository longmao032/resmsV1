package com.guang.integration.controller.system;

import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.integration.domain.dto.NotificationSaveDTO;
import com.guang.integration.domain.vo.NotificationVO;
import com.guang.integration.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 工作通知表 前端控制器 (管理端)
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Tag(name = "通知公告管理")
@RestController
@RequestMapping("/api/system/v1/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "发布通知公告")
    @PostMapping("/publish")
    @PreAuthorize("hasAuthority('message:notice:add')")
    @Log(title = "通知公告管理", businessType = "NOTIFICATION", operatorType = "SAVE")
    public CommonResult<Boolean> publish(@Validated @RequestBody NotificationSaveDTO saveDTO) {
        return CommonResult.success(notificationService.publishNotification(saveDTO));
    }

    @Operation(summary = "分页查询通知列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('message:notice:query', 'team:user')")
    public CommonResult<Page<NotificationVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) String title,
                                             @RequestParam(required = false) Byte noticeType) {
        return CommonResult.success(notificationService.pageNotifications(pageNum, pageSize, title, noticeType));
    }

    @Operation(summary = "撤回通知")
    @PutMapping("/withdraw/{id}")
    @PreAuthorize("hasAuthority('message:notice:withdraw')")
    @Log(title = "通知公告管理", businessType = "NOTIFICATION", operatorType = "UPDATE")
    public CommonResult<Boolean> withdraw(@PathVariable Integer id) {
        return CommonResult.success(notificationService.withdrawNotification(id));
    }

    @Operation(summary = "批量删除通知")
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('message:notice:delete')")
    @Log(title = "通知公告管理", businessType = "NOTIFICATION", operatorType = "DELETE")
    public CommonResult<Boolean> delete(@RequestBody List<Integer> ids) {
        return CommonResult.success(notificationService.deleteNotifications(ids));
    }

    @Operation(summary = "编辑通知公告")
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('message:notice:edit')")
    @Log(title = "通知公告管理", businessType = "NOTIFICATION", operatorType = "UPDATE")
    public CommonResult<Boolean> update(@PathVariable Integer id,
                                        @Validated @RequestBody NotificationSaveDTO saveDTO) {
        return CommonResult.success(notificationService.updateNotification(id, saveDTO));
    }
}
