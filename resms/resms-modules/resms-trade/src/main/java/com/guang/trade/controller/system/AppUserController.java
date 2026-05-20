package com.guang.trade.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.trade.domain.dto.AppUserQueryDTO;
import com.guang.trade.domain.vo.AppUserStatisticsVO;
import com.guang.trade.entity.AppUser;
import com.guang.trade.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * C端移动端用户账号表 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-10
 */
@RestController
@RequestMapping("/api/system/trade/v1/app-users")
@Tag(name = "C端用户管理")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;

    @Operation(summary = "分页查询C端用户列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('trade:appuser:query', 'trade:customer:query')")
    public com.guang.common.result.CommonResult<Page<AppUser>> list(AppUserQueryDTO queryDTO) {
        return com.guang.common.result.CommonResult.success(appUserService.pageAppUsers(queryDTO));
    }

    @Operation(summary = "修改C端用户状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('trade:appuser:status')")
    @Log(title = "C端用户管理", businessType = "APP_USER", operatorType = "STATUS")
    public CommonResult<Boolean> changeStatus(@PathVariable Long id, @RequestParam Byte status) {
        return CommonResult.success(appUserService.changeStatus(id, status));
    }

    @Operation(summary = "删除C端用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('trade:appuser:delete')")
    @Log(title = "C端用户管理", businessType = "APP_USER", operatorType = "DELETE")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        return CommonResult.success(appUserService.removeById(id));
    }

    @Operation(summary = "获取C端用户统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('trade:appuser:query')")
    public CommonResult<AppUserStatisticsVO> statistics() {
        return CommonResult.success(appUserService.getStatistics());
    }

    @Operation(summary = "导出C端用户")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('trade:appuser:export')")
    public void export(AppUserQueryDTO queryDTO, HttpServletResponse response) {
        appUserService.exportAppUsers(queryDTO, response);
    }
}
