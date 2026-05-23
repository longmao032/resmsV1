package com.guang.aiassistant.repository;

import java.util.List;

public interface VectorStoreRepository {

    /**
     * 回填向量表里的物理过滤字段（city, district, price_num, area_min, area_max 等）
     */
    void backfillPhysicalColumns();

    /**
     * 获取向量表中所有房源的物理 ID
     *
     * @return 房源ID列表
     */
    List<Integer> getAllHouseIds();
}
