package com.guang.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.trade.domain.dto.FavoriteDTO;
import com.guang.trade.domain.vo.FavoriteFanVO;
import com.guang.trade.domain.vo.FavoriteHouseVO;
import com.guang.trade.entity.AppUser;
import com.guang.trade.entity.UserFavorite;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 用户收藏记录表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface UserFavoriteService extends IService<UserFavorite> {

    /**
     * 分页查询房源收藏统计 (管理端)
     */
    Page<FavoriteHouseVO> pageFavoriteHouses(Integer pageNum, Integer pageSize, String houseTitle);

    /**
     * 获取收藏房源的粉丝列表
     */
    List<AppUser> getFansByHouseId(Integer houseId);

    /**
     * 获取收藏房源的粉丝VO列表（含收藏时间）
     */
    List<FavoriteFanVO> getFanVOsByHouseId(Integer houseId);

    /**
     * 添加收藏
     */
    Boolean addFavorite(FavoriteDTO favoriteDTO);

    /**
     * 取消收藏
     */
    Boolean removeFavorite(FavoriteDTO favoriteDTO);

    /**
     * 分页获取当前用户收藏
     */
    Page<UserFavorite> pageMyFavorites(Integer pageNum, Integer pageSize, Byte targetType);
}
