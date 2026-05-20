package com.guang.house.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.result.CommonResult;
import com.guang.house.domain.dto.HouseStatusLogQueryDTO;
import com.guang.house.domain.vo.HouseStatusLogVO;
import com.guang.house.service.HouseStatusLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 房源状态流转日志表 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Tag(name = "房源日志", description = "房源状态流转日志接口")
@RestController
@RequestMapping("/api/system/house/v1/status-logs")
@RequiredArgsConstructor
public class HouseStatusLogController {

    private final HouseStatusLogService houseStatusLogService;

    @Operation(summary = "分页查询房源状态日志")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('house:log:query', 'house:house:query', 'trade:order:query')")
    public CommonResult<Page<HouseStatusLogVO>> list(HouseStatusLogQueryDTO queryDTO) {
        return CommonResult.success(houseStatusLogService.pageLogs(queryDTO));
    }
}
