package com.guang.aiassistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SearchStateService {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai"));
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbc;
    private volatile boolean available = true;

    public SearchStateService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() {
        try {
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS session_search_state (
                        session_id      VARCHAR(64) PRIMARY KEY,
                        profile_fetched BOOLEAN NOT NULL DEFAULT FALSE,
                        profile_has_data BOOLEAN NOT NULL DEFAULT FALSE,
                        search_records  JSONB NOT NULL DEFAULT '[]'::jsonb,
                        updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbc.execute("""
                    CREATE INDEX IF NOT EXISTS idx_session_search_state_updated_at
                    ON session_search_state(updated_at)
                    """);
            // 迁移：新增画像文本和 JSON 缓存列（IF NOT EXISTS 处理重复执行，异常冒泡到外层 catch 正确设置 available=false）
            jdbc.execute("ALTER TABLE session_search_state ADD COLUMN IF NOT EXISTS persona_text TEXT");
            jdbc.execute("ALTER TABLE session_search_state ADD COLUMN IF NOT EXISTS persona_json JSONB");
        } catch (DataAccessException e) {
            available = false;
            log.error("搜索状态表初始化失败，降级为无记忆模式", e);
        }
    }

    // ---- Public API ----

    public String getSearchContext(String sessionId) {
        State state = getState(sessionId);
        return buildSearchContext(state);
    }

    public void markProfileFetched(String sessionId, boolean hasData,
                                    @Nullable String personaText,
                                    @Nullable String personaJson) {
        if (!available) return;
        try {
            jdbc.update("""
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

    public void recordSearch(String sessionId, String tool, Map<String, Object> params, int count) {
        if (!available) return;
        try {
            Map<String, Object> record = Map.of(
                    "t", Instant.now().toString(),
                    "tool", tool,
                    "params", params != null ? params : Map.of(),
                    "count", count
            );
            String json = MAPPER.writeValueAsString(record);
            jdbc.update(
                    "UPDATE session_search_state SET search_records = search_records || ?::jsonb, updated_at = NOW() WHERE session_id = ?",
                    json, sessionId);
        } catch (Exception e) {
            available = false;
            log.warn("记录搜索失败 sessionId={}: {}", sessionId, e.getMessage());
        }
    }

    public void clearSession(String sessionId) {
        try {
            jdbc.update("DELETE FROM session_search_state WHERE session_id = ?", sessionId);
        } catch (DataAccessException e) {
            log.warn("清空搜索状态失败 sessionId={}: {}", sessionId, e.getMessage());
        }
    }

    // ---- Public API - Extended ----

    @Nullable
    public String getPersonaText(String sessionId) {
        return getState(sessionId).personaText();
    }

    @Nullable
    public String getPersonaJson(String sessionId) {
        return getState(sessionId).personaJson();
    }

    /** 从搜索历史中提取最近一次使用的城市，用于短消息的会话上下文补全 */
    @Nullable
    public String getLastCity(String sessionId) {
        State state = getState(sessionId);
        if (state.records().isEmpty()) return null;
        for (int i = state.records().size() - 1; i >= 0; i--) {
            var r = state.records().get(i);
            if ("queryHouses".equals(r.tool()) || "searchProjects".equals(r.tool())) {
                if (r.params() != null && r.params().get("city") instanceof String c && !c.isBlank()) {
                    return c;
                }
            }
        }
        return null;
    }

    // ---- Internal ----

    public State getState(String sessionId) {
        if (!available) return State.empty();
        try {
            List<State> rows = jdbc.query(
                    "SELECT profile_fetched, profile_has_data, persona_text, persona_json, search_records FROM session_search_state WHERE session_id = ?",
                    (rs, rowNum) -> {
                        boolean profileFetched = rs.getBoolean("profile_fetched");
                        boolean profileHasData = rs.getBoolean("profile_has_data");
                        String personaText = rs.getString("persona_text");
                        String personaJson = rs.getString("persona_json");
                        String json = rs.getString("search_records");
                        List<SearchRecord> records = parseRecords(json);
                        return new State(profileFetched, profileHasData, personaText, personaJson, records);
                    },
                    sessionId);
            return rows.isEmpty() ? State.empty() : rows.get(0);
        } catch (Exception e) {
            available = false;
            log.warn("读取搜索状态失败 sessionId={}: {}", sessionId, e.getMessage());
            return State.empty();
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

    private String buildSearchContext(State state) {
        StringBuilder sb = new StringBuilder();

        // Profile
        if (state.profileFetched) {
            sb.append("用户画像：已获取");
            sb.append(state.profileHasData ? "（有数据）" : "（无数据：暂无收藏/浏览/预约记录）");
            sb.append("。本轮无需重复查询！\n");
        } else {
            sb.append("用户画像：未获取。\n");
        }

        // Searches
        var records = state.records;
        if (records.isEmpty()) {
            sb.append("历史搜索记录：无\n");
        } else {
            sb.append("历史搜索记录：\n");
            for (int i = 0; i < records.size(); i++) {
                var r = records.get(i);
                sb.append(String.format("- 第%d次：%s → %d条",
                        i + 1, formatRecord(r), r.count));
                String time = formatTime(r.t);
                if (!time.isEmpty()) sb.append(" (").append(time).append(")");
                sb.append("\n");
            }
            // Anti-repeat guidance
            List<SearchRecord> emptyOnes = records.stream()
                    .filter(r -> r.count == 0).toList();
            if (!emptyOnes.isEmpty()) {
                sb.append("规划建议：以上 ");
                for (var e : emptyOnes) {
                    sb.append(formatRecord(e)).append("、");
                }
                sb.setLength(sb.length() - 1); // remove trailing 、
                sb.append(" 已确认为空，切勿重复搜索！若用户目标匹配这些组合，只生成 1 个 doTerminate 步骤告知用户。\n");
            }
        }

        return sb.toString().trim();
    }

    private String formatRecord(SearchRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.tool).append("(");
        if (r.params != null && !r.params.isEmpty()) {
            r.params.forEach((k, v) -> {
                if (v != null && !"null".equals(String.valueOf(v)) && !"".equals(String.valueOf(v))) {
                    sb.append(k).append("=").append(v).append(", ");
                }
            });
            int len = sb.length();
            if (sb.substring(len - 2).equals(", ")) {
                sb.setLength(len - 2);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String formatTime(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            return TIME_FMT.format(Instant.parse(iso));
        } catch (Exception e) {
            return "";
        }
    }

    // ---- Inner types ----

    public record State(boolean profileFetched, boolean profileHasData,
                         @Nullable String personaText, @Nullable String personaJson,
                         List<SearchRecord> records) {
        static State empty() {
            return new State(false, false, null, null, List.of());
        }
    }

    private record SearchRecord(String t, String tool, Map<String, Object> params, int count) {}
}
