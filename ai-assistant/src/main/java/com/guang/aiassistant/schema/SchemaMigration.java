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
    private final String tableName;

    public SchemaMigration(JdbcTemplate jdbc,
                           @org.springframework.beans.factory.annotation.Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}") String tableName) {
        this.jdbc = jdbc;
        this.tableName = tableName;
    }

    @PostConstruct
    public void migrate() {
        log.info("开始数据库 Schema 迁移，目标表：{}...", tableName);
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
                "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS type VARCHAR(32)",
                "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS city VARCHAR(64)",
                "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS district VARCHAR(64)",
                "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS price_num INTEGER",
                "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS area_min INTEGER",
                "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS area_max INTEGER",
        };
        for (String sql : columns) {
            try {
                jdbc.execute(sql);
            } catch (DataAccessException e) {
                log.warn("DDL 执行警告: {} — {}", sql.substring(0, Math.min(50, sql.length())), e.getMessage());
            }
        }
        log.info("{} 物理列迁移完成", tableName);
    }

    private void migrateVectorStoreIndexes() {
        // 索引名称也可以根据表名动态化，以防止不同表名复用同一个索引名报错，但 PostgreSQL 索引是 schema 级别的，通常同一库里不重名就行。
        // 为了安全起见，这里可以使用动态索引名，或者保留统一的索引名（因为表名一般在不同环境下只有一张）。
        String[] indexes = {
                "CREATE INDEX IF NOT EXISTS idx_" + tableName + "_geo_lookup ON " + tableName + " (city, district, price_num) WHERE type = 'house'",
                "CREATE INDEX IF NOT EXISTS idx_" + tableName + "_area_lookup ON " + tableName + " (area_min, area_max) WHERE type = 'house'",
        };
        for (String sql : indexes) {
            try {
                jdbc.execute(sql);
            } catch (DataAccessException e) {
                log.warn("索引创建警告: {} — {}", sql.substring(0, Math.min(60, sql.length())), e.getMessage());
            }
        }
        log.info("{} 索引迁移完成", tableName);
    }

    /**
     * 回填物理列数据：从 JSONB metadata 中提取已有字段。
     * 仅回填 type/city/district（纯文本字段），price_num/area_min/area_max 由 DataSyncService 全量同步时写入。
     */
    private void backfillVectorStoreColumns() {
        try {
            int updated = jdbc.update("""
                    UPDATE %s
                    SET type     = metadata->>'type',
                        city     = metadata->>'city',
                        district = metadata->>'district'
                    WHERE type IS NULL
                    """.formatted(tableName));
            if (updated > 0) {
                log.info("{} 物理列回填完成，更新 {} 行", tableName, updated);
            }
        } catch (DataAccessException e) {
            log.warn("{} 物理列回填失败: {}", tableName, e.getMessage());
        }
    }
}
