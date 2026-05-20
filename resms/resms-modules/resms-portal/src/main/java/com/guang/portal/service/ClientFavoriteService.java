package com.guang.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.portal.domain.dto.FavoriteDTO;
import com.guang.portal.domain.vo.FavoriteItemVO;

public interface ClientFavoriteService {

    /**
     * 分页获取当前 C 端用户的收藏列表
     */
    Page<FavoriteItemVO> pageFavorites(Integer pageNum, Integer pageSize);

    /**
     * 获取指定用户的收藏列表（供 AI/后台使用）
     */
    Page<FavoriteItemVO> pageFavorites(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 添加收藏
     */
    void addFavorite(FavoriteDTO dto);

    /**
     * 按收藏记录 ID 取消收藏
     */
    void removeFavoriteById(Integer id);
}
