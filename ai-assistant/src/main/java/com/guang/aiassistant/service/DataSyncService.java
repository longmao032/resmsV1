package com.guang.aiassistant.service;

import com.guang.aiassistant.model.dto.HouseSyncDTO;
import com.guang.aiassistant.model.dto.ProjectSyncDTO;

import java.util.List;

/**
 * 数据同步服务接口契约 — 声明房源/楼盘向量化同步对外规范。
 */
public interface DataSyncService {

    /** 全量同步所有房源到向量库，返回写入的房源向量记录数 */
    int syncAllHouses();

    /** 增量 Upsert 单条房源 */
    void upsertHouse(HouseSyncDTO dto);

    /** 增量批量 Upsert 房源 */
    void upsertHouseBatch(List<HouseSyncDTO> dtos);

    /** 删除单条房源 */
    void deleteHouse(Integer houseId);

    /** 增量 Upsert 单条楼盘 */
    void upsertProject(ProjectSyncDTO dto);

    /** 删除单条楼盘 */
    void deleteProject(Integer projectId);

    /** 获取向量库中所有房源 ID */
    List<Integer> getAllHouseIds();
}
