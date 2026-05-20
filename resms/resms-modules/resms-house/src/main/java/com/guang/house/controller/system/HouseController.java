package com.guang.house.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.house.domain.dto.HouseAuditDTO;
import com.guang.house.domain.dto.HouseQueryDTO;
import com.guang.house.domain.dto.HouseSaveDTO;
import com.guang.house.domain.vo.HousePageVO;
import com.guang.house.domain.vo.HouseStatisticsVO;
import com.guang.house.domain.vo.HouseVO;
import com.guang.house.entity.House;
import com.guang.house.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 房源管理 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Tag(name = "房源管理", description = "房源主表相关接口")
@RestController
@RequestMapping("/api/system/house/v1/houses")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;

    @Operation(summary = "分页查询房源")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('house:house:list')")
    public CommonResult<Page<HousePageVO>> list(HouseQueryDTO queryDTO) {
        return CommonResult.success(houseService.pageHouses(queryDTO));
    }

    @Operation(summary = "导出房源列表")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('house:house:export')")
    public void export(HouseQueryDTO queryDTO, HttpServletResponse response) {
        houseService.exportHouses(queryDTO, response);
    }

    @Operation(summary = "新增/修改房源")
    @PostMapping
    @PreAuthorize("hasAuthority('house:house:save')")
    @Log(title = "房源管理", businessType = "HOUSE", operatorType = "SAVE")
    public CommonResult<Boolean> save(@RequestBody HouseSaveDTO saveDTO) {
        return CommonResult.success(houseService.saveHouse(saveDTO));
    }

    @Operation(summary = "获取房源详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('house:house:query')")
    public CommonResult<HouseVO> getDetail(@PathVariable Integer id) {
        return CommonResult.success(houseService.getHouseDetail(id));
    }

    @Operation(summary = "房源审核")
    @PostMapping("/audit")
    @PreAuthorize("hasAuthority('house:house:audit')")
    @Log(title = "房源管理", businessType = "HOUSE", operatorType = "AUDIT")
    public CommonResult<Boolean> audit(@RequestBody HouseAuditDTO auditDTO) {
        return CommonResult.success(houseService.auditHouse(auditDTO));
    }

    @Operation(summary = "房源统计概览（顶部看板）")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('house:house:list')")
    public CommonResult<HouseStatisticsVO> statistics() {
        return CommonResult.success(houseService.getStatistics());
    }
}
