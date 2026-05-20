package com.guang.house.service;

import com.guang.house.entity.HouseStatusLog;
import com.baomidou.mybatisplus.extension.service.IService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.house.domain.dto.HouseStatusLogQueryDTO;
import com.guang.house.domain.vo.HouseStatusLogVO;

/**
 * <p>
 * 房源状态流转日志表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface HouseStatusLogService extends IService<HouseStatusLog> {

    /**
     * 分页查询状态日志
     */
    Page<HouseStatusLogVO> pageLogs(HouseStatusLogQueryDTO queryDTO);
}
