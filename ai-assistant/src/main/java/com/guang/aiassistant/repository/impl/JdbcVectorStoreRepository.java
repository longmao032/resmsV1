package com.guang.aiassistant.repository.impl;

import com.guang.aiassistant.repository.VectorStoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
public class JdbcVectorStoreRepository implements VectorStoreRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    public JdbcVectorStoreRepository(JdbcTemplate jdbcTemplate,
                                     @org.springframework.beans.factory.annotation.Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}") String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public void backfillPhysicalColumns() {
        try {
            int updated = jdbcTemplate.update("""
                    UPDATE %s
                    SET type      = metadata->>'type',
                        city      = metadata->>'city',
                        district  = metadata->>'district',
                        price_num = COALESCE((metadata->>'price_num')::int, 0),
                        area_min  = COALESCE((metadata->>'area_min')::int, 0),
                        area_max  = COALESCE((metadata->>'area_max')::int, 0)
                    WHERE type IS NULL
                    """.formatted(tableName));
            if (updated > 0) {
                log.info("{} 物理列回填完成，更新 {} 行", tableName, updated);
            }
        } catch (Exception e) {
            log.warn("{} 物理列回填失败: {}", tableName, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Integer> getAllHouseIds() {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT (metadata->>'houseId')::int FROM " + tableName + " WHERE type = 'house' AND metadata->>'houseId' IS NOT NULL",
                    Integer.class
            );
        } catch (Exception e) {
            log.error("查询 {} 中所有房源ID失败", tableName, e);
            return Collections.emptyList();
        }
    }
}
