package com.guang.aiassistant.model;

import org.springframework.lang.Nullable;
import java.util.List;

/**
 * 会话搜索状态快照模型
 */
public record SearchState(
        boolean profileFetched,
        boolean profileHasData,
        @Nullable String personaText,
        @Nullable String personaJson,
        List<SearchRecord> records
) {
    public static SearchState empty() {
        return new SearchState(false, false, null, null, List.of());
    }
}
