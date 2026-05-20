package com.guang.house.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guang.house.domain.dto.HouseAuditDTO;
import com.guang.house.domain.dto.HouseQueryDTO;
import com.guang.house.domain.dto.HouseSaveDTO;
import com.guang.house.domain.vo.HousePageVO;
import com.guang.house.domain.vo.HouseStatisticsVO;
import com.guang.house.domain.vo.HouseVO;
import com.guang.house.entity.House;

import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * 房源主表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface HouseService extends IService<House> {

    /**
     * 分页查询房源 (支持地理位置搜索)
     */
    Page<HousePageVO> pageHouses(HouseQueryDTO queryDTO);

    /**
     * 导出房源列表
     */
    void exportHouses(HouseQueryDTO queryDTO, HttpServletResponse response);

    /**
     * 保存房源（含扩展信息同步）
     */
    Boolean saveHouse(HouseSaveDTO saveDTO);

    /**
     * 修改房源状态 (支持并发控制)
     *
     * @param id             房源ID
     * @param status         目标状态
     * @param expectedStatus 期望的前置状态（可选）
     * @param reason         变更原因
     * @return 是否成功
     */
    Boolean updateStatus(Integer id, Byte status, Byte expectedStatus, String reason);

    /**
     * 获取房源聚合详情
     */
    HouseVO getHouseDetail(Integer id);

    /**
     * 房源审核
     */
    Boolean auditHouse(HouseAuditDTO auditDTO);

    /**
     * 获取房源统计概览（顶部看板）
     */
    HouseStatisticsVO getStatistics();
}
