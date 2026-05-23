package com.guang.aiassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.guang.aiassistant.model.RankedHouse;
import com.guang.aiassistant.model.SearchResult;
import com.guang.aiassistant.tool.HouseReranker;
import com.guang.aiassistant.tool.HouseSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * 搜索管道编排器 — 召回 → 二次排序 → 缓存 → 降级。
 * 替代 ChatServiceImpl 中的 trySearch() + doQuery()。
 */
@Slf4j
@Service
public class SearchPipeline {

    private final HouseSearchTool houseSearchTool;
    private final HouseReranker reranker;
    private final SearchCacheService cacheService;

    private final int minResults;
    private final int resultLimit;

    public SearchPipeline(HouseSearchTool houseSearchTool,
                          HouseReranker reranker,
                          SearchCacheService cacheService,
                          @Value("${search.pipeline.min-results:3}") int minResults,
                          @Value("${search.pipeline.result-limit:5}") int resultLimit) {
        this.houseSearchTool = houseSearchTool;
        this.reranker = reranker;
        this.cacheService = cacheService;
        this.minResults = minResults;
        this.resultLimit = resultLimit;
    }

    /**
     * 执行搜索管道。
     *
     * @param city               城市
     * @param district           区县（可为 null）
     * @param houseType          房型（可为 null）
     * @param layout             户型（可为 null）
     * @param minArea            最小面积（可为 null）
     * @param maxArea            最大面积（可为 null）
     * @param minPrice           最低价格（万元，可为 null）
     * @param maxPrice           最高价格（万元，可为 null）
     * @param semanticQuery      语义查询文本
     * @param personaData        画像 data 节点（可为 null）
     * @param userTagPreferences 用户偏好标签列表
     * @param requestedDistrict  用户本轮明确提到的区县（用于评分）
     * @return 搜索结果（含排序后的房源和降级信息）
     */
    public SearchResult execute(
            String city, String district, Integer houseType,
            String layout, Integer minArea, Integer maxArea,
            Integer minPrice, Integer maxPrice,
            String semanticQuery, JsonNode personaData,
            List<String> userTagPreferences, String requestedDistrict) {

        // 1. 检查缓存
        var cached = cacheService.get(city, district, houseType, minPrice, maxPrice, layout, userTagPreferences);
        if (cached.isPresent()) {
            List<RankedHouse> limited = cached.get().stream().limit(resultLimit).toList();
            return SearchResult.of(limited);
        }

        // 2. 首次召回 + 排序
        List<Document> candidates = houseSearchTool.queryHousesRaw(
                city, district, layout, minArea, maxArea, minPrice, maxPrice, houseType, semanticQuery, false);
        List<RankedHouse> ranked = reranker.rerank(
                candidates, semanticQuery, requestedDistrict, minPrice, maxPrice, personaData, userTagPreferences);

        if (ranked.size() >= minResults) {
            List<RankedHouse> limited = ranked.stream().limit(resultLimit).toList();
            cacheService.put(city, district, houseType, minPrice, maxPrice, layout, userTagPreferences, limited);
            return SearchResult.of(limited);
        }

        // 3. 渐进式降级
        List<String> removed = new ArrayList<>();
        String curDistrict = district;
        String curLayout = layout;
        Integer curMinArea = minArea, curMaxArea = maxArea;
        Integer curHouseType = houseType;
        Integer curMinPrice = minPrice, curMaxPrice = maxPrice;

        // L1: 去掉区县
        if (curDistrict != null) {
            curDistrict = null;
            removed.add("扩大至全市范围");
            ranked = recallAndRerank(city, curDistrict, curHouseType, curLayout,
                    curMinArea, curMaxArea, curMinPrice, curMaxPrice,
                    semanticQuery, personaData, userTagPreferences, requestedDistrict);
            if (ranked.size() >= minResults) return buildResult(ranked, removed, city, district,
                    houseType, minPrice, maxPrice, layout, userTagPreferences);
        }

        // L2: 去掉户型/面积
        if (curLayout != null || curMinArea != null || curMaxArea != null) {
            curLayout = null;
            curMinArea = curMaxArea = null;
            removed.add("放宽户型和面积");
            ranked = recallAndRerank(city, curDistrict, curHouseType, curLayout,
                    curMinArea, curMaxArea, curMinPrice, curMaxPrice,
                    semanticQuery, personaData, userTagPreferences, requestedDistrict);
            if (ranked.size() >= minResults) return buildResult(ranked, removed, city, district,
                    houseType, minPrice, maxPrice, layout, userTagPreferences);
        }

        // L3: 去掉房型
        if (curHouseType != null) {
            curHouseType = null;
            removed.add("放宽房屋类型");
            ranked = recallAndRerank(city, curDistrict, curHouseType, curLayout,
                    curMinArea, curMaxArea, curMinPrice, curMaxPrice,
                    semanticQuery, personaData, userTagPreferences, requestedDistrict);
            if (ranked.size() >= minResults) return buildResult(ranked, removed, city, district,
                    houseType, minPrice, maxPrice, layout, userTagPreferences);
        }

        // L4: 价格 ±15%
        if (curMinPrice != null || curMaxPrice != null) {
            curMinPrice = curMinPrice != null ? (int) (curMinPrice * 0.85) : null;
            curMaxPrice = curMaxPrice != null ? (int) (curMaxPrice * 1.15) : null;
            removed.add("放宽预算");
            ranked = recallAndRerank(city, curDistrict, curHouseType, curLayout,
                    curMinArea, curMaxArea, curMinPrice, curMaxPrice,
                    semanticQuery, personaData, userTagPreferences, requestedDistrict);
            if (ranked.size() >= minResults) return buildResult(ranked, removed, city, district,
                    houseType, minPrice, maxPrice, layout, userTagPreferences);
        }

        // L5: 纯城市兜底（保留语义查询）
        removed.add("扩大至全市搜索");
        ranked = recallAndRerank(city, null, null, null, null, null, null, null,
                semanticQuery, personaData, userTagPreferences, requestedDistrict);

        // L6: 热门房源兜底（去掉语义约束，仅保留城市过滤）
        if (ranked.isEmpty()) {
            removed.add("为您推荐热门房源");
            ranked = recallAndRerank(city, null, null, null, null, null, null, null,
                    null, personaData, userTagPreferences, requestedDistrict);
        }

        List<RankedHouse> limited = ranked.stream().limit(resultLimit).toList();
        String reason = String.join("、", removed);
        cacheService.put(city, district, houseType, minPrice, maxPrice, layout, userTagPreferences, limited);
        return SearchResult.degraded(limited, removed.size(), reason);
    }

    // ==================== 内部方法 ====================

    private List<RankedHouse> recallAndRerank(
            String city, String district, Integer houseType,
            String layout, Integer minArea, Integer maxArea,
            Integer minPrice, Integer maxPrice,
            String semanticQuery, JsonNode personaData,
            List<String> userTagPreferences, String requestedDistrict) {

        log.info("管道降级召回: city={}, district={}, houseType={}, layout={}, area={}-{}, price={}-{}",
                city, district, houseType, layout, minArea, maxArea, minPrice, maxPrice);

        List<Document> candidates = houseSearchTool.queryHousesRaw(
                city, district, layout, minArea, maxArea, minPrice, maxPrice, houseType, semanticQuery, true);
        return reranker.rerank(candidates, semanticQuery, requestedDistrict,
                minPrice, maxPrice, personaData, userTagPreferences);
    }

    private SearchResult buildResult(List<RankedHouse> ranked, List<String> removed,
                                     String city, String district, Integer houseType,
                                     Integer minPrice, Integer maxPrice, String layout,
                                     List<String> userTagPreferences) {
        List<RankedHouse> limited = ranked.stream().limit(resultLimit).toList();
        String reason = String.join("、", removed);
        cacheService.put(city, district, houseType, minPrice, maxPrice, layout, userTagPreferences, limited);
        return SearchResult.degraded(limited, removed.size(), reason);
    }
}
