package com.guang.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.common.util.SecurityUtils;
import com.guang.trade.domain.dto.FavoriteDTO;
import com.guang.trade.domain.vo.FavoriteFanVO;
import com.guang.trade.domain.vo.FavoriteHouseVO;
import com.guang.trade.entity.AppUser;
import com.guang.trade.entity.UserFavorite;
import com.guang.trade.mapper.UserFavoriteMapper;
import com.guang.trade.service.UserFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 用户收藏记录表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements UserFavoriteService {

    private final UserFavoriteMapper favoriteMapper;

    @Override
    public Page<FavoriteHouseVO> pageFavoriteHouses(Integer pageNum, Integer pageSize, String houseTitle) {
        Page<FavoriteHouseVO> page = new Page<>(pageNum, pageSize);
        return (Page<FavoriteHouseVO>) favoriteMapper.selectFavoriteHousePage(page, houseTitle);
    }

    @Override
    public List<AppUser> getFansByHouseId(Integer houseId) {
        return favoriteMapper.selectFansByHouseId(houseId);
    }

    @Override
    public List<FavoriteFanVO> getFanVOsByHouseId(Integer houseId) {
        return favoriteMapper.selectFanVOsByHouseId(houseId);
    }

    @Override
    public Boolean addFavorite(FavoriteDTO favoriteDTO) {
        Integer userId = SecurityUtils.getUserId();
        
        // 检查是否已收藏
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getAppUserId, userId)
                .eq(UserFavorite::getTargetType, favoriteDTO.getTargetType())
                .eq(UserFavorite::getTargetId, favoriteDTO.getTargetId());
        
        if (this.count(wrapper) > 0) {
            return true; // 已收藏，幂等处理
        }

        UserFavorite favorite = new UserFavorite();
        favorite.setAppUserId(Long.valueOf(userId));
        favorite.setTargetType(favoriteDTO.getTargetType());
        favorite.setTargetId(favoriteDTO.getTargetId());
        favorite.setCreateTime(LocalDateTime.now());
        
        return this.save(favorite);
    }

    @Override
    public Boolean removeFavorite(FavoriteDTO favoriteDTO) {
        Integer userId = SecurityUtils.getUserId();
        
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getAppUserId, userId)
                .eq(UserFavorite::getTargetType, favoriteDTO.getTargetType())
                .eq(UserFavorite::getTargetId, favoriteDTO.getTargetId());
        
        return this.remove(wrapper);
    }

    @Override
    public Page<UserFavorite> pageMyFavorites(Integer pageNum, Integer pageSize, Byte targetType) {
        Integer userId = SecurityUtils.getUserId();
        Page<UserFavorite> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getAppUserId, userId)
                .eq(targetType != null, UserFavorite::getTargetType, targetType)
                .orderByDesc(UserFavorite::getCreateTime);
        
        return this.page(page, wrapper);
    }
}
