package com.guang.house.controller.system;

import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.house.domain.dto.HouseImageSaveDTO;
import com.guang.house.service.HouseImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 房源图片管理 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Tag(name = "房源图片", description = "房源图片相关接口")
@RestController
@RequestMapping("/api/system/house/v1/images")
@RequiredArgsConstructor
public class HouseImageController {

    private final HouseImageService houseImageService;

    @Operation(summary = "批量保存房源图片")
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('house:image:save')")
    @Log(title = "房源图片", businessType = "HOUSE_IMAGE", operatorType = "SAVE")
    public CommonResult<Boolean> batchSave(@RequestBody List<HouseImageSaveDTO> imageDTOs) {
        return CommonResult.success(houseImageService.batchSave(imageDTOs));
    }

    @Operation(summary = "设为封面")
    @PutMapping("/{id}/cover")
    @PreAuthorize("hasAuthority('house:image:edit')")
    @Log(title = "房源图片", businessType = "HOUSE_IMAGE", operatorType = "UPDATE")
    public CommonResult<Boolean> setAsCover(@PathVariable Integer id) {
        return CommonResult.success(houseImageService.setAsCover(id));
    }

    @Operation(summary = "图片排序")
    @PostMapping("/sort")
    @PreAuthorize("hasAuthority('house:image:edit')")
    @Log(title = "房源图片", businessType = "HOUSE_IMAGE", operatorType = "UPDATE")
    public CommonResult<Boolean> sort(@RequestBody List<Integer> imageIds) {
        return CommonResult.success(houseImageService.sortImages(imageIds));
    }

    @Operation(summary = "查询孤儿图片（过期照片）")
    @GetMapping("/orphaned")
    @PreAuthorize("hasAuthority('house:image:query')")
    public CommonResult<List<String>> findOrphaned() {
        return CommonResult.success(houseImageService.findOrphanedImages());
    }
}
