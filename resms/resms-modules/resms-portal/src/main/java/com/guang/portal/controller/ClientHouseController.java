package com.guang.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.result.CommonResult;
import com.guang.house.domain.dto.HouseQueryDTO;
import com.guang.house.domain.vo.HousePageVO;
import com.guang.house.domain.vo.HouseVO;
import com.guang.house.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "房源浏览（C端）")
@RestController
@RequestMapping("/api/portal/v1/houses")
@RequiredArgsConstructor
public class ClientHouseController {

    private final HouseService houseService;

    @Operation(summary = "分页查询在售房源")
    @GetMapping("/page")
    public CommonResult<Page<HousePageVO>> page(HouseQueryDTO queryDTO) {
        // C端只展示在售房源
        queryDTO.setStatus((byte) 1);
        return CommonResult.success(houseService.pageHouses(queryDTO));
    }

    @Operation(summary = "获取房源详情")
    @GetMapping("/{id}")
    public CommonResult<HouseVO> detail(@PathVariable Integer id) {
        return CommonResult.success(houseService.getHouseDetail(id));
    }
}
