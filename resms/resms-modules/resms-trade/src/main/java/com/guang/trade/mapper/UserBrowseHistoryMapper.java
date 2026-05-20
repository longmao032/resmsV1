package com.guang.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.trade.domain.vo.FootprintVO;
import com.guang.trade.entity.UserBrowseHistory;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 用户浏览历史记录表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface UserBrowseHistoryMapper extends BaseMapper<UserBrowseHistory> {

    /**
     * 分页查询客户足迹 (管理端)
     */
    IPage<FootprintVO> selectFootprintPage(Page<FootprintVO> page, 
                                          @Param("customerName") String customerName, 
                                          @Param("actionType") String actionType);
}
