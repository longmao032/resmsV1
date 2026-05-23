package com.guang.aiassistant.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.guang.aiassistant.model.RankedHouse;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 房源二次排序引擎 — 纯 Java 计算，无 LLM 调用。
 * 五维评分：标签匹配 + 画像适配 + 语义相关 + 价格匹配 + 位置匹配。
 */
@Component
public class HouseReranker {

    private final double wTag;
    private final double wPersona;
    private final double wSemantic;
    private final double wPrice;
    private final double wLocation;

    public HouseReranker(
            @Value("${search.reranker.weight-tag:0.25}") double wTag,
            @Value("${search.reranker.weight-persona:0.20}") double wPersona,
            @Value("${search.reranker.weight-semantic:0.25}") double wSemantic,
            @Value("${search.reranker.weight-price:0.15}") double wPrice,
            @Value("${search.reranker.weight-location:0.15}") double wLocation) {
        this.wTag = wTag;
        this.wPersona = wPersona;
        this.wSemantic = wSemantic;
        this.wPrice = wPrice;
        this.wLocation = wLocation;
    }

    /**
     * 对候选房源进行多维评分并排序。
     *
     * @param candidates         向量搜索返回的候选文档列表
     * @param userQuery          用户原始查询文本（用于标签匹配）
     * @param requestedDistrict  用户本轮明确提到的区县
     * @param minPrice           用户最低预算（万元）
     * @param maxPrice           用户最高预算（万元）
     * @param personaData        画像 data 节点（可为 null）
     * @param userTagPreferences 用户偏好标签列表（从 generalQuery 提取）
     * @return 按 compositeScore 降序排列的 RankedHouse 列表
     */
    public List<RankedHouse> rerank(
            List<Document> candidates,
            String userQuery,
            String requestedDistrict,
            Integer minPrice,
            Integer maxPrice,
            JsonNode personaData,
            List<String> userTagPreferences) {

        if (candidates == null || candidates.isEmpty()) return List.of();

        // 合并查询文本和偏好标签，用于标签匹配
        Set<String> allTagKeywords = new HashSet<>();
        if (userTagPreferences != null) allTagKeywords.addAll(userTagPreferences);
        if (userQuery != null && !userQuery.isBlank()) {
            HouseSearchTool.KNOWN_TAGS.stream()
                    .filter(userQuery::contains)
                    .forEach(allTagKeywords::add);
        }

        // 画像区县
        String personaDistrict = null;
        if (personaData != null) {
            JsonNode hard = personaData.get("hardConstraints");
            if (hard != null) {
                JsonNode districts = hard.get("districts");
                if (districts != null && districts.size() > 0) personaDistrict = districts.get(0).asText();
            }
        }

        List<RankedHouse> ranked = new ArrayList<>();
        int total = candidates.size();

        for (int i = 0; i < total; i++) {
            Document doc = candidates.get(i);
            Map<String, Object> meta = doc.getMetadata();

            double tagScore = computeTagScore(meta, allTagKeywords);
            double personaScore = computePersonaScore(meta, personaData);
            double semanticScore = computeSemanticScore(i, total);
            double priceScore = computePriceScore(meta, minPrice, maxPrice);
            double locationScore = computeLocationScore(meta, requestedDistrict, personaDistrict);

            double composite = wTag * tagScore
                    + wPersona * personaScore
                    + wSemantic * semanticScore
                    + wPrice * priceScore
                    + wLocation * locationScore;

            ranked.add(new RankedHouse(doc, tagScore, personaScore, semanticScore,
                    priceScore, locationScore, composite));
        }

        ranked.sort((a, b) -> Double.compare(b.compositeScore(), a.compositeScore()));
        return ranked;
    }

    // ==================== 分项评分 ====================

    /** 标签匹配得分：文档 tags 包含的关键词数 / 总关键词数 */
    private double computeTagScore(Map<String, Object> meta, Set<String> keywords) {
        if (keywords.isEmpty()) return 0.5; // 无偏好时中性分
        String tags = str(meta, "tags");
        if (tags == null || tags.isBlank()) return 0.0;
        long matches = keywords.stream().filter(tags::contains).count();
        return Math.min(1.0, (double) matches / Math.max(1, keywords.size()));
    }

    /** 画像适配得分：逐项检查区县/户型/房型/面积是否匹配画像 */
    private double computePersonaScore(Map<String, Object> meta, JsonNode personaData) {
        if (personaData == null) return 0.5;
        int checks = 0, hits = 0;

        JsonNode hard = personaData.get("hardConstraints");
        if (hard != null) {
            // 区县
            JsonNode districts = hard.get("districts");
            if (districts != null && districts.size() > 0) {
                checks++;
                String docDistrict = str(meta, "district");
                if (docDistrict != null) {
                    for (JsonNode d : districts) {
                        if (docDistrict.contains(d.asText())) { hits++; break; }
                    }
                }
            }
            // 户型
            JsonNode rooms = hard.get("layoutRooms");
            if (rooms != null && rooms.size() > 0) {
                checks++;
                String docLayout = str(meta, "roomType");
                if (docLayout != null) {
                    for (JsonNode r : rooms) {
                        if (docLayout.contains(r.asInt() + "室")) { hits++; break; }
                    }
                }
            }
            // 面积
            if (hard.has("minArea") && hard.has("maxArea")) {
                checks++;
                int docMin = intVal(meta, "area_min", 0);
                int docMax = intVal(meta, "area_max", 99999);
                int pMin = hard.get("minArea").asInt() * 100;
                int pMax = hard.get("maxArea").asInt() * 100;
                if (docMin <= pMax && docMax >= pMin) hits++;
            }
        }

        // 房型
        JsonNode soft = personaData.get("softPreferences");
        if (soft != null && soft.has("preferredHouseType")) {
            checks++;
            String prefType = soft.get("preferredHouseType").asText();
            Integer mapped = CustomerProfileTool.houseTypeToInt(prefType);
            if (mapped != null) {
                int docType = intVal(meta, "houseType", -1);
                if (docType == mapped) hits++;
            }
        }

        return checks == 0 ? 0.5 : (double) hits / checks;
    }

    /** 语义相关得分：位置越靠前得分越高 */
    private double computeSemanticScore(int index, int total) {
        if (total <= 1) return 1.0;
        return 1.0 - (double) index / (total - 1);
    }

    /** 价格匹配得分：房价与预算中点的距离 */
    private double computePriceScore(Map<String, Object> meta, Integer minPrice, Integer maxPrice) {
        int docPrice = intVal(meta, "price_num", -1);
        if (docPrice <= 0) return 0.5;

        if (minPrice == null && maxPrice == null) return 0.5;

        double mid;
        if (minPrice != null && maxPrice != null) {
            mid = (minPrice + maxPrice) / 2.0;
        } else if (maxPrice != null) {
            mid = maxPrice;
        } else {
            mid = minPrice;
        }

        if (mid <= 0) return 0.5;
        double ratio = Math.abs(docPrice - mid) / mid;
        return Math.max(0.0, Math.min(1.0, 1.0 - ratio));
    }

    /** 位置匹配得分：区县 + 地铁 */
    private double computeLocationScore(Map<String, Object> meta,
                                        String requestedDistrict, String personaDistrict) {
        double score = 0.0;
        String docDistrict = str(meta, "district");
        String tags = str(meta, "tags");

        if (requestedDistrict != null && docDistrict != null && docDistrict.contains(requestedDistrict)) {
            score += 1.0;
        } else if (personaDistrict != null && docDistrict != null && docDistrict.contains(personaDistrict)) {
            score += 0.5;
        }
        if (tags != null && (tags.contains("地铁") || tags.contains("交通便利"))) {
            score += 0.3;
        }
        return Math.min(1.0, score);
    }

    // ==================== 工具方法 ====================

    private static String str(Map<String, Object> meta, String key) {
        Object val = meta.get(key);
        return val != null ? val.toString() : null;
    }

    private static int intVal(Map<String, Object> meta, String key, int def) {
        Object val = meta.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { /* ignore */ }
        }
        return def;
    }
}
