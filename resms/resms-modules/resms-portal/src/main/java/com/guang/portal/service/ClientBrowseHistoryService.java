package com.guang.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.portal.domain.dto.BrowseHistoryDTO;
import com.guang.portal.domain.vo.BrowseHistoryItemVO;

public interface ClientBrowseHistoryService {

    /**
     * 分页获取当前 C 端用户的浏览记录
     */
    Page<BrowseHistoryItemVO> pageHistory(Integer pageNum, Integer pageSize, Byte resourceType);

    /**
     * 获取指定用户的浏览记录（供 AI/后台使用）
     */
    Page<BrowseHistoryItemVO> pageHistory(Long userId, Integer pageNum, Integer pageSize, Byte resourceType);

    /**
     * 添加浏览记录（10分钟内同一资源去重）
     */
    void addHistory(BrowseHistoryDTO dto);

    /**
     * 删除单条浏览记录
     */
    void removeById(Long id);

    /**
     * 清空当前用户的所有浏览记录
     */
    void clearAll();
}
