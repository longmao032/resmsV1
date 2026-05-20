package com.guang.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.trade.domain.dto.HistoryDTO;
import com.guang.trade.domain.vo.FootprintVO;
import com.guang.trade.entity.UserBrowseHistory;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户浏览历史记录表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface UserBrowseHistoryService extends IService<UserBrowseHistory> {

    /**
     * 分页查询客户足迹 (管理端)
     */
    Page<FootprintVO> pageFootprints(Integer pageNum, Integer pageSize, String customerName, String actionType);

    /**
     * 记录浏览历史
     */
    Boolean recordHistory(HistoryDTO historyDTO);

    /**
     * 分页获取我的浏览历史
     */
    Page<UserBrowseHistory> pageMyHistory(Integer pageNum, Integer pageSize, Byte resourceType);

    /**
     * 清空我的浏览历史
     */
    Boolean clearMyHistory();
}
