package com.guang.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.result.CommonResult;
import com.guang.portal.domain.dto.FavoriteDTO;
import com.guang.portal.domain.vo.FavoriteItemVO;
import com.guang.portal.service.ClientFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "C端收藏管理")
@RestController
@RequestMapping("/api/portal/v1/user/favorites")
@RequiredArgsConstructor
public class ClientFavoriteController {

    private final ClientFavoriteService favoriteService;

    @Operation(summary = "分页获取我的收藏列表")
    @GetMapping
    public CommonResult<Page<FavoriteItemVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(favoriteService.pageFavorites(pageNum, pageSize));
    }

    @Operation(summary = "添加收藏")
    @PostMapping
    public CommonResult<Void> add(@Valid @RequestBody FavoriteDTO dto) {
        favoriteService.addFavorite(dto);
        return CommonResult.success(null);
    }

    @Operation(summary = "取消收藏（按收藏记录ID）")
    @DeleteMapping("/{id:\\d+}")
    public CommonResult<Void> remove(@PathVariable Integer id) {
        favoriteService.removeFavoriteById(id);
        return CommonResult.success(null);
    }
}
