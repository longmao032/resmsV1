package com.guang.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.trade.domain.vo.FavoriteFanVO;
import com.guang.trade.domain.vo.FavoriteHouseVO;
import com.guang.trade.entity.AppUser;
import com.guang.trade.entity.UserFavorite;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户收藏记录表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * 分页查询房源收藏热度榜
     */
    IPage<FavoriteHouseVO> selectFavoriteHousePage(Page<FavoriteHouseVO> page, @Param("houseTitle") String houseTitle);

    /**
     * 查询收藏该房源的粉丝列表
     */
    List<AppUser> selectFansByHouseId(@Param("houseId") Integer houseId);

    /**
     * 查询收藏该房源的粉丝VO列表（含收藏时间）
     */
    List<FavoriteFanVO> selectFanVOsByHouseId(@Param("houseId") Integer houseId);
}
