package com.guang.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.result.CommonResult;
import com.guang.portal.domain.dto.BrowseHistoryDTO;
import com.guang.portal.domain.vo.BrowseHistoryItemVO;
import com.guang.portal.service.ClientBrowseHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "C端浏览记录")
@RestController
@RequestMapping("/api/portal/v1/user/browse-history")
@RequiredArgsConstructor
public class ClientBrowseHistoryController {

    private final ClientBrowseHistoryService browseHistoryService;

    @Operation(summary = "分页获取浏览记录")
    @GetMapping
    public CommonResult<Page<BrowseHistoryItemVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Byte resourceType) {
        return CommonResult.success(browseHistoryService.pageHistory(pageNum, pageSize, resourceType));
    }

    @Operation(summary = "添加浏览记录")
    @PostMapping
    public CommonResult<Void> add(@RequestBody BrowseHistoryDTO dto) {
        browseHistoryService.addHistory(dto);
        return CommonResult.success(null);
    }

    @Operation(summary = "删除单条浏览记录")
    @DeleteMapping("/{id:\\d+}")
    public CommonResult<Void> remove(@PathVariable Long id) {
        browseHistoryService.removeById(id);
        return CommonResult.success(null);
    }

    @Operation(summary = "清空所有浏览记录")
    @DeleteMapping
    public CommonResult<Void> clear() {
        browseHistoryService.clearAll();
        return CommonResult.success(null);
    }
}
