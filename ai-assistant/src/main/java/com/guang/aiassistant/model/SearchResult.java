package com.guang.aiassistant.model;

import java.util.Collections;
import java.util.List;

/**
 * 搜索管道输出 — 召回 + 二次排序 + 降级后的最终结果。
 */
public record SearchResult(
        List<RankedHouse> rankedHouses,
        int degradationLevel,
        String degradationReason
) {
    public SearchResult {
        rankedHouses = rankedHouses != null ? List.copyOf(rankedHouses) : Collections.emptyList();
    }

    public static SearchResult of(List<RankedHouse> rankedHouses) {
        return new SearchResult(rankedHouses, 0, null);
    }

    public static SearchResult degraded(List<RankedHouse> rankedHouses, int level, String reason) {
        return new SearchResult(rankedHouses, level, reason);
    }
}
