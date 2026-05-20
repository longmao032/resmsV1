package com.guang.trade.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.trade.domain.dto.FavoriteDTO;
import com.guang.trade.domain.vo.FavoriteFanVO;
import com.guang.trade.domain.vo.FavoriteHouseVO;
import com.guang.trade.entity.AppUser;
import com.guang.trade.entity.UserFavorite;
import com.guang.trade.service.UserFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户收藏记录表 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@RestController
@RequestMapping("/api/system/trade/v1/favorites")
@Tag(name = "收藏管理")
@RequiredArgsConstructor
public class UserFavoriteController {

    private final UserFavoriteService userFavoriteService;

    @Operation(summary = "管理端分页查询房源收藏热度")
    @GetMapping
    @PreAuthorize("hasAuthority('trade:favorite:query')")
    public CommonResult<Page<FavoriteHouseVO>> listFavoriteHouses(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String houseTitle) {
        return CommonResult.success(userFavoriteService.pageFavoriteHouses(pageNum, pageSize, houseTitle));
    }

    @Operation(summary = "管理端查询收藏房源的粉丝")
    @GetMapping("/house/{houseId}/fans")
    @PreAuthorize("hasAuthority('trade:favorite:query')")
    public CommonResult<List<FavoriteFanVO>> listFans(@PathVariable Integer houseId) {
        return CommonResult.success(userFavoriteService.getFanVOsByHouseId(houseId));
    }

    @Operation(summary = "添加收藏")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Log(title = "收藏管理", businessType = "FAVORITE", operatorType = "ADD")
    public CommonResult<Boolean> add(@Validated @RequestBody FavoriteDTO favoriteDTO) {
        return CommonResult.success(userFavoriteService.addFavorite(favoriteDTO));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Log(title = "收藏管理", businessType = "FAVORITE", operatorType = "DELETE")
    public CommonResult<Boolean> remove(@Validated @RequestBody FavoriteDTO favoriteDTO) {
        return CommonResult.success(userFavoriteService.removeFavorite(favoriteDTO));
    }

    @Operation(summary = "我的收藏列表")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Page<UserFavorite>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Byte targetType) {
        return CommonResult.success(userFavoriteService.pageMyFavorites(pageNum, pageSize, targetType));
    }
}
