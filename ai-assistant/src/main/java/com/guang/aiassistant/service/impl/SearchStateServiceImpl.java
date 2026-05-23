package com.guang.aiassistant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guang.aiassistant.model.SearchRecord;
import com.guang.aiassistant.model.SearchState;
import com.guang.aiassistant.repository.SearchStateRepository;
import com.guang.aiassistant.service.SearchStateService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SearchStateServiceImpl implements SearchStateService {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai"));

    private final SearchStateRepository searchStateRepository;
    private final ObjectMapper objectMapper;

    public SearchStateServiceImpl(SearchStateRepository searchStateRepository,
                                  ObjectMapper objectMapper) {
        this.searchStateRepository = searchStateRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        searchStateRepository.initSchema();
    }

    @Override
    public String getSearchContext(String sessionId) {
        SearchState state = getState(sessionId);
        return buildSearchContext(state);
    }

    @Override
    public void markProfileFetched(String sessionId, boolean hasData,
                                    @Nullable String personaText,
                                    @Nullable String personaJson) {
        searchStateRepository.saveOrUpdateProfile(sessionId, hasData, personaText, personaJson);
    }

    @Override
    public void recordSearch(String sessionId, String tool, Map<String, Object> params, int count) {
        try {
            Map<String, Object> record = Map.of(
                    "t", Instant.now().toString(),
                    "tool", tool,
                    "params", params != null ? params : Map.of(),
                    "count", count
            );
            String json = objectMapper.writeValueAsString(record);
            searchStateRepository.appendSearchRecord(sessionId, json);
        } catch (Exception e) {
            log.warn("记录搜索失败 sessionId={}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void clearSession(String sessionId) {
        searchStateRepository.deleteSession(sessionId);
    }

    @Override
    @Nullable
    public String getPersonaText(String sessionId) {
        return getState(sessionId).personaText();
    }

    @Override
    @Nullable
    public String getPersonaJson(String sessionId) {
        return getState(sessionId).personaJson();
    }

    @Override
    @Nullable
    public String getLastCity(String sessionId) {
        SearchState state = getState(sessionId);
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

    @Override
    public SearchState getState(String sessionId) {
        return searchStateRepository.getState(sessionId);
    }

    private String buildSearchContext(SearchState state) {
        StringBuilder sb = new StringBuilder();

        if (state.profileFetched()) {
            sb.append("用户画像：已获取");
            sb.append(state.profileHasData() ? "（有数据）" : "（无数据：暂无收藏/浏览/预约记录）");
            sb.append("。本轮无需重复查询！\n");
        } else {
            sb.append("用户画像：未获取。\n");
        }

        var records = state.records();
        if (records.isEmpty()) {
            sb.append("历史搜索记录：无\n");
        } else {
            sb.append("历史搜索记录：\n");
            for (int i = 0; i < records.size(); i++) {
                var r = records.get(i);
                sb.append(String.format("- 第%d次：%s → %d条",
                        i + 1, formatRecord(r), r.count()));
                String time = formatTime(r.t());
                if (!time.isEmpty()) sb.append(" (").append(time).append(")");
                sb.append("\n");
            }
            List<SearchRecord> emptyOnes = records.stream()
                    .filter(r -> r.count() == 0).toList();
            if (!emptyOnes.isEmpty()) {
                sb.append("规划建议：以上 ");
                for (var e : emptyOnes) {
                    sb.append(formatRecord(e)).append("、");
                }
                sb.setLength(sb.length() - 1);
                sb.append(" 已确认为空，切勿重复搜索！若用户目标匹配这些组合，只生成 1 个 doTerminate 步骤告知用户。\n");
            }
        }

        return sb.toString().trim();
    }

    private String formatRecord(SearchRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.tool()).append("(");
        if (r.params() != null && !r.params().isEmpty()) {
            r.params().forEach((k, v) -> {
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
}

