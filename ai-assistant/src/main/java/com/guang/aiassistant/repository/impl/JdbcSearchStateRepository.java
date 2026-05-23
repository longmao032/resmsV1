package com.guang.aiassistant.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guang.aiassistant.model.SearchRecord;
import com.guang.aiassistant.model.SearchState;
import com.guang.aiassistant.repository.SearchStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class JdbcSearchStateRepository implements SearchStateRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private volatile boolean available = true;

    public JdbcSearchStateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void initSchema() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS session_search_state (
                        session_id      VARCHAR(64) PRIMARY KEY,
                        profile_fetched BOOLEAN NOT NULL DEFAULT FALSE,
                        profile_has_data BOOLEAN NOT NULL DEFAULT FALSE,
                        search_records  JSONB NOT NULL DEFAULT '[]'::jsonb,
                        updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_session_search_state_updated_at
                    ON session_search_state(updated_at)
                    """);
            jdbcTemplate.execute("ALTER TABLE session_search_state ADD COLUMN IF NOT EXISTS persona_text TEXT");
            jdbcTemplate.execute("ALTER TABLE session_search_state ADD COLUMN IF NOT EXISTS persona_json JSONB");
        } catch (DataAccessException e) {
            available = false;
            log.error("搜索状态表初始化失败，降级为无记忆模式", e);
        }
    }

    @Override
    public SearchState getState(String sessionId) {
        if (!available) return SearchState.empty();
        try {
            List<SearchState> rows = jdbcTemplate.query(
                    "SELECT profile_fetched, profile_has_data, persona_text, persona_json, search_records FROM session_search_state WHERE session_id = ?",
                    (rs, rowNum) -> {
                        boolean profileFetched = rs.getBoolean("profile_fetched");
                        boolean profileHasData = rs.getBoolean("profile_has_data");
                        String personaText = rs.getString("persona_text");
                        String personaJson = rs.getString("persona_json");
                        String json = rs.getString("search_records");
                        List<SearchRecord> records = parseRecords(json);
                        return new SearchState(profileFetched, profileHasData, personaText, personaJson, records);
                    },
                    sessionId);
            return rows.isEmpty() ? SearchState.empty() : rows.get(0);
        } catch (Exception e) {
            available = false;
            log.warn("读取搜索状态失败 sessionId={}: {}", sessionId, e.getMessage());
            return SearchState.empty();
        }
    }

    @Override
    public void saveOrUpdateProfile(String sessionId, boolean hasData, String personaText, String personaJson) {
        if (!available) return;
        try {
            jdbcTemplate.update("""
                    INSERT INTO session_search_state
                        (session_id, profile_fetched, profile_has_data, persona_text, persona_json)
                    VALUES (?, TRUE, ?, ?, ?::jsonb)
                    ON CONFLICT (session_id) DO UPDATE SET
                        profile_fetched = TRUE,
                        profile_has_data = ?,
                        persona_text = ?,
                        persona_json = ?::jsonb,
                        updated_at = NOW()
                    """, sessionId, hasData, personaText, personaJson,
                    hasData, personaText, personaJson);
        } catch (DataAccessException e) {
            available = false;
            log.warn("标记画像状态失败 sessionId={}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void appendSearchRecord(String sessionId, String recordJson) {
        if (!available) return;
        try {
            jdbcTemplate.update("""
                    INSERT INTO session_search_state (session_id, search_records)
                    VALUES (?, jsonb_build_array(?::jsonb))
                    ON CONFLICT (session_id) DO UPDATE SET
                        search_records = COALESCE(session_search_state.search_records, '[]'::jsonb) || EXCLUDED.search_records,
                        updated_at = NOW()
                    """, sessionId, recordJson);
        } catch (Exception e) {
            available = false;
            log.warn("记录搜索失败 sessionId={}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void deleteSession(String sessionId) {
        try {
            jdbcTemplate.update("DELETE FROM session_search_state WHERE session_id = ?", sessionId);
        } catch (DataAccessException e) {
            log.warn("清空搜索状态失败 sessionId={}: {}", sessionId, e.getMessage());
        }
    }

    private List<SearchRecord> parseRecords(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return List.of();
        }
        try {
            List<Map<String, Object>> raw = MAPPER.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            List<SearchRecord> records = new ArrayList<>();
            for (Map<String, Object> r : raw) {
                records.add(new SearchRecord(
                        (String) r.get("t"),
                        (String) r.get("tool"),
                        castParams(r.get("params")),
                        r.get("count") instanceof Number n ? n.intValue() : -1
                ));
            }
            return records;
        } catch (Exception e) {
            log.warn("解析搜索记录 JSON 失败: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castParams(Object params) {
        if (params instanceof Map) return (Map<String, Object>) params;
        return Map.of();
    }
}
