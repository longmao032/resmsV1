package com.guang.aiassistant.schema;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 统一管理所有 DDL 迁移，在应用启动时按 @PostConstruct 顺序自动执行。
 * <p>
 * 所有操作均为幂等加法（ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS），
 * 不删除已有字段，支持零停机回滚。
 */
@Component
public class SchemaMigration {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    private final JdbcTemplate jdbc;

    public SchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void migrate() {
        log.info("开始数据库 Schema 迁移...");
        try {
            migrateVectorStoreColumns();
            migrateVectorStoreIndexes();
            backfillVectorStoreColumns();
            log.info("数据库 Schema 迁移完成");
        } catch (Exception e) {
            log.error("Schema 迁移异常，应用继续启动（非阻塞）: {}", e.getMessage());
        }
    }

    // ==================== vector_store 物理列 ====================

    private void migrateVectorStoreColumns() {
        String[] columns = {
                "ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS type VARCHAR(32)",
                "ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS city VARCHAR(64)",
                "ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS district VARCHAR(64)",
                "ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS price_num INTEGER",
                "ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS area_min INTEGER",
                "ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS area_max INTEGER",
        };
        for (String sql : columns) {
            try {
                jdbc.execute(sql);
            } catch (DataAccessException e) {
                log.warn("DDL 执行警告: {} — {}", sql.substring(0, 50), e.getMessage());
            }
        }
        log.info("vector_store 物理列迁移完成");
    }

    private void migrateVectorStoreIndexes() {
        String[] indexes = {
                "CREATE INDEX IF NOT EXISTS idx_vs_geo_lookup ON vector_store (city, district, price_num) WHERE type = 'house'",
                "CREATE INDEX IF NOT EXISTS idx_vs_area_lookup ON vector_store (area_min, area_max) WHERE type = 'house'",
        };
        for (String sql : indexes) {
            try {
                jdbc.execute(sql);
            } catch (DataAccessException e) {
                log.warn("索引创建警告: {} — {}", sql.substring(0, 60), e.getMessage());
            }
        }
        log.info("vector_store 索引迁移完成");
    }

    /**
     * 回填物理列数据：从 JSONB metadata 中提取已有字段。
     * 仅回填 type/city/district（纯文本字段），price_num/area_min/area_max 由 DataSyncService 全量同步时写入。
     */
    private void backfillVectorStoreColumns() {
        try {
            int updated = jdbc.update("""
                    UPDATE vector_store
                    SET type     = metadata->>'type',
                        city     = metadata->>'city',
                        district = metadata->>'district'
                    WHERE type IS NULL
                    """);
            if (updated > 0) {
                log.info("vector_store 物理列回填完成，更新 {} 行", updated);
            }
        } catch (DataAccessException e) {
            log.warn("vector_store 物理列回填失败: {}", e.getMessage());
        }
    }
}
