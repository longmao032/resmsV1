package com.guang.aiassistant.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 房源搜索工具 — 精准查询 / 语义搜索 / 混合搜索 三模合一。
 * <p>
 * Precise: JdbcTemplate 直查物理列，B-Tree 索引，无 embedding 开销
 * Semantic: VectorStore 纯语义搜索 + type=house 过滤
 * Hybrid: VectorStore metadata 过滤 + 语义排序 + Java 后置面积/价格过滤
 */
@Component
public class HouseSearchTool {

    private static final Logger log = LoggerFactory.getLogger(HouseSearchTool.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 已知标签词库 — 从项目/房源 tags 元数据中提取的高频搜索偏好词 */
    public static final List<String> KNOWN_TAGS = List.of(
            "学区房", "名校学区", "带学位",
            "豪宅顶奢", "顶配豪宅", "江景豪宅", "老牌豪宅", "豪宅",
            "一线海景", "海景视野", "环幕海景", "海景",
            "一线江景", "江景", "一线湖景", "湖景",
            "地铁沿线", "双地铁", "双地铁口", "地铁上盖", "近地铁", "地铁",
            "精装空置", "精装", "拎包入住", "家电齐全", "家电全",
            "现房", "现房销售",
            "低密度", "大平层",
            "满五唯一",
            "环境优美", "环境安静", "安静舒适", "园林景观",
            "配套成熟", "配套醇熟", "交通便利",
            "高性价比", "低总价", "刚需首选",
            "超大商圈", "近购物中心", "万象天地", "COCOCity",
            "私家泳池", "带私家花园", "超高层", "楼王单位",
            "红本在手", "红本无贷", "高赠送", "全景视野", "全景飘窗"
    );

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final ThreadLocal<Integer> lastQueryCount = new ThreadLocal<>();
    private final ThreadLocal<Map<String, Object>> lastQueryParams = new ThreadLocal<>();
    /** 缓存最近一次搜索的原始 Document 列表，供 PlanningFlow 提取结构化 HouseItem */
    private final ThreadLocal<List<Document>> lastSearchDocs = new ThreadLocal<>();

    public HouseSearchTool(VectorStore vectorStore, JdbcTemplate jdbcTemplate,
                           @Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}") String tableName) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    // ==================== 公开方法 ====================

    public String queryHouses(
            String searchMode,
            String city,
            String district,
            String layout,
            Integer minArea,
            Integer maxArea,
            Integer minPrice,
            Integer maxPrice,
            Integer houseType,
            String query) {

        // 名称规范化："深圳"→"深圳市"、"南山"→"南山区"，对齐 metadata 中的存储格式
        city = normalizeCity(city);
        district = normalizeDistrict(district);
        // "搜全城"语义：district 等于 city 时视为不限定区域
        if (district != null && district.equals(city)) {
            district = null;
        }

        // 防御性清理 LLM 虚构的占位值 —— 避免"0室0厅"、0、"深圳市"等假值过滤掉全部结果
        layout = sanitizeLayout(layout);
        if (minArea != null && minArea <= 0) minArea = null;
        if (maxArea != null && maxArea >= 10000) maxArea = null;
        if (minPrice != null && minPrice <= 0) minPrice = null;
        if (maxPrice != null && maxPrice >= 99999) maxPrice = null;

        // 无任何过滤条件 → 降级热门推荐，避免全库跨城污染
        if (city == null && district == null && houseType == null
                && minArea == null && maxArea == null && minPrice == null && maxPrice == null) {
            String popular = fallbackPopularHouses(20);
            List<Document> fallbackDocs = lastSearchDocs.get();
            lastQueryCount.set(fallbackDocs != null ? fallbackDocs.size() : 0);
            return popular;
        }

        recordQueryParams(searchMode, city, district, layout, minArea, maxArea, minPrice, maxPrice, houseType, 0);

        String result = switch (searchMode != null ? searchMode : "hybrid") {
            case "precise" -> preciseSearch(city, district, layout, minArea, maxArea,
                    minPrice, maxPrice, houseType);
            case "semantic" -> semanticSearch(query, 20);
            default -> hybridSearch(city, district, layout, minArea, maxArea,
                    minPrice, maxPrice, houseType, query);
        };

        List<Document> resultDocs = lastSearchDocs.get();
        lastQueryCount.set(resultDocs != null ? resultDocs.size() : 0);
        return result;
    }

    public String searchProjects(
            String keyword,
            String city,
            String district) {

        city = normalizeCity(city);
        district = normalizeDistrict(district);
        if (district != null && district.equals(city)) {
            district = null;
        }

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op cur = b.eq("type", "project");
        if (city != null && !city.isBlank()) {
            cur = b.and(cur, b.eq("city", city));
        }
        if (district != null && !district.isBlank()) {
            cur = b.and(cur, b.eq("district", district));
        }

        StringBuilder q = new StringBuilder();
        if (city != null) q.append(city).append(" ");
        if (district != null) q.append(district).append(" ");
        if (keyword != null) q.append(keyword).append(" ");
        q.append("楼盘项目");

        List<Document> docs;
        try {
            docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(q.toString())
                            .filterExpression(cur.build())
                            .topK(20)
                            .similarityThreshold(0.25)
                            .build());
        } catch (Exception e) {
            log.warn("searchProjects 查询异常: {}", e.getMessage());
            return "查询异常，请稍后重试。";
        }

        // 关键词后置过滤
        var filtered = docs.stream()
                .filter(d -> keyword == null || keyword.isBlank() || matchesProjectName(d, keyword))
                .toList();

        Map<String, Object> params = new HashMap<>();
        if (keyword != null) params.put("keyword", keyword);
        if (city != null) params.put("city", city);
        if (district != null) params.put("district", district);
        lastQueryCount.set(filtered.size());
        lastQueryParams.set(params);
        lastSearchDocs.set(new ArrayList<>(filtered));

        if (filtered.isEmpty()) return "未找到匹配的楼盘。";
        return filtered.stream().map(this::formatProjectResult).collect(Collectors.joining("\n"));
    }

    // ==================== 管道专用：返回原始 Document 列表 ====================

    /**
     * 搜索管道专用方法 — 返回原始 Document 列表，不走 ThreadLocal，不格式化。
     *
     * @param relaxedFilters true 时跳过价格/面积后置过滤（降级时使用）
     */
    public List<Document> queryHousesRaw(
            String city, String district, String layout,
            Integer minArea, Integer maxArea,
            Integer minPrice, Integer maxPrice,
            Integer houseType, String query,
            boolean relaxedFilters) {

        city = normalizeCity(city);
        district = normalizeDistrict(district);
        if (district != null && district.equals(city)) district = null;
        layout = sanitizeLayout(layout);
        if (minArea != null && minArea <= 0) minArea = null;
        if (maxArea != null && maxArea >= 10000) maxArea = null;
        if (minPrice != null && minPrice <= 0) minPrice = null;
        if (maxPrice != null && maxPrice >= 99999) maxPrice = null;

        // lambda 捕获用的 final 副本
        final String fCity = city;
        final String fDistrict = district;
        final String fLayout = layout;
        final Integer fMinArea = minArea, fMaxArea = maxArea;
        final Integer fMinPrice = minPrice, fMaxPrice = maxPrice;

        // 构建 metadata filter
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op cur = b.eq("type", "house");
        if (fCity != null && !fCity.isBlank()) cur = b.and(cur, b.eq("city", fCity));
        if (fDistrict != null && !fDistrict.isBlank()) cur = b.and(cur, b.eq("district", fDistrict));
        if (houseType != null) cur = b.and(cur, b.eq("houseType", houseType));

        // 查询文本
        StringBuilder q = new StringBuilder();
        if (fCity != null) q.append(fCity).append(" ");
        if (fDistrict != null) q.append(fDistrict).append(" ");
        q.append("房产");
        if (query != null) q.append(" ").append(query);
        if (fLayout != null) q.append(" ").append(fLayout);

        List<Document> docs;
        try {
            docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(q.toString())
                            .filterExpression(cur.build())
                            .topK(150)
                            .similarityThreshold(0.15)
                            .build());
        } catch (Exception e) {
            log.warn("queryHousesRaw 查询异常: {}", e.getMessage());
            return List.of();
        }

        // 后置过滤
        var filtered = docs.stream()
                .filter(d -> fLayout == null || fLayout.isBlank() || matchesLayout(d, fLayout))
                .filter(d -> relaxedFilters || areaOverlap(d, fMinArea, fMaxArea))
                .filter(d -> relaxedFilters || priceOverlap(d, fMinPrice, fMaxPrice))
                .toList();

        return new ArrayList<>(filtered);
    }

    // ==================== 模式一：Precise 精准查询 ====================

    private String preciseSearch(String city, String district, String layout,
                                  Integer minArea, Integer maxArea,
                                  Integer minPrice, Integer maxPrice,
                                  Integer houseType) {
        StringBuilder sql = new StringBuilder(
                "SELECT content, metadata FROM " + tableName + " WHERE type = 'house'");
        List<Object> params = new ArrayList<>();

        if (city != null && !city.isBlank()) {
            sql.append(" AND city = ?");
            params.add(city);
        }
        if (district != null && !district.isBlank()) {
            sql.append(" AND district = ?");
            params.add(district);
        }
        if (houseType != null) {
            sql.append(" AND (metadata->>'houseType')::int = ?");
            params.add(houseType);
        }
        if (minPrice != null) {
            sql.append(" AND price_num >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND price_num <= ?");
            params.add(maxPrice);
        }
        if (minArea != null) {
            sql.append(" AND area_max >= ?");
            params.add(minArea);
        }
        if (maxArea != null) {
            sql.append(" AND area_min <= ?");
            params.add(maxArea);
        }

        sql.append(" LIMIT ?");
        params.add(100);  // 缩减取数，价格和面积已在数据库级过滤，仅 layout 需后置匹配

        List<Document> docs;
        try {
            docs = jdbcTemplate.query(sql.toString(), (rs, i) -> {
                Map<String, Object> meta = parseJsonMap(rs.getString("metadata"));
                return new Document(rs.getString("content"), meta);
            }, params.toArray());
        } catch (Exception e) {
            log.warn("Precise 查询异常: {}", e.getMessage());
            return "查询异常，请稍后重试。";
        }

        // 仅后置过滤 layout(文本)
        var filtered = docs.stream()
                .filter(d -> layout == null || layout.isBlank() || matchesLayout(d, layout))
                .limit(20)
                .toList();

        lastSearchDocs.set(filtered);

        if (filtered.isEmpty()) return "未找到匹配的房源。";
        return filtered.stream().map(this::formatHouseResult).collect(Collectors.joining("\n"));
    }

    // ==================== 模式二：Semantic 语义搜索 ====================

    private String semanticSearch(String query, int limit) {
        if (query == null || query.isBlank()) {
            return fallbackPopularHouses(limit);
        }

        // type=house 基础过滤，防止非房源文档混入（修复 B-06）
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression filter = b.eq("type", "house").build();

        List<Document> docs;
        try {
            docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .filterExpression(filter)
                            .topK(limit)
                            .similarityThreshold(0.35)
                            .build());
        } catch (Exception e) {
            log.warn("Semantic 查询异常: {}", e.getMessage());
            return "查询异常，请稍后重试。";
        }

        lastSearchDocs.set(docs);
        if (docs.isEmpty()) return "未找到匹配的房源，请尝试更具体的描述。";
        return docs.stream().map(this::formatHouseResult).collect(Collectors.joining("\n"));
    }

    // ==================== 模式三：Hybrid 混合搜索 ====================

    private String hybridSearch(String city, String district, String layout,
                                 Integer minArea, Integer maxArea,
                                 Integer minPrice, Integer maxPrice,
                                 Integer houseType, String query) {
        // 构建 metadata filter（仅过滤 JSONB 中存在的字段，面积/价格不通过 FilterExpressionBuilder 过滤）
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op cur = b.eq("type", "house");
        if (city != null && !city.isBlank()) {
            cur = b.and(cur, b.eq("city", city));
        }
        if (district != null && !district.isBlank()) {
            cur = b.and(cur, b.eq("district", district));
        }
        if (houseType != null) {
            cur = b.and(cur, b.eq("houseType", houseType));
        }

        // 查询文本
        StringBuilder q = new StringBuilder();
        if (city != null) q.append(city).append(" ");
        if (district != null) q.append(district).append(" ");
        q.append("房产");
        if (query != null) q.append(" ").append(query);
        if (layout != null) q.append(" ").append(layout);

        // 扩大 topK 补偿后置过滤损耗
        boolean hasPostFilter = minArea != null || maxArea != null || minPrice != null || maxPrice != null;
        int fetchK = hasPostFilter ? 100 : 50;

        List<Document> docs;
        try {
            docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(q.toString())
                            .filterExpression(cur.build())
                            .topK(fetchK)
                            .similarityThreshold(0.25)
                            .build());
        } catch (Exception e) {
            log.warn("Hybrid 查询异常: {}", e.getMessage());
            return "查询异常，请稍后重试。";
        }

        // 后置过滤：layout(文本) + area(数值区间) + price(数值区间)
        // 面积和价格在 Java 层做，不通过 FilterExpressionBuilder（修复 B-04）
        var filtered = docs.stream()
                .filter(d -> layout == null || layout.isBlank() || matchesLayout(d, layout))
                .filter(d -> areaOverlap(d, minArea, maxArea))
                .filter(d -> priceOverlap(d, minPrice, maxPrice))
                .toList();

        // 标签感知排序：metadata.tags 匹配用户查询关键词的文档优先
        List<String> tagKeywords = extractTagKeywords(query);
        List<Document> sorted;
        if (!tagKeywords.isEmpty() && !filtered.isEmpty()) {
            var tagMatched = filtered.stream()
                    .filter(d -> matchesAnyTag(d, tagKeywords))
                    .toList();
            var rest = filtered.stream()
                    .filter(d -> !matchesAnyTag(d, tagKeywords))
                    .toList();
            sorted = new ArrayList<>(tagMatched);
            sorted.addAll(rest);
            log.info("标签匹配: {} 个关键词命中 {} / {} 条结果", tagKeywords, tagMatched.size(), filtered.size());
        } else {
            sorted = new ArrayList<>(filtered);
        }

        lastSearchDocs.set(sorted.stream().limit(20).toList());

        if (sorted.isEmpty()) return "未找到匹配的房源。";
        return sorted.stream().limit(20).map(this::formatHouseResult).collect(Collectors.joining("\n"));
    }

    // ==================== 后置过滤 ====================

    private boolean matchesLayout(Document doc, String layout) {
        String roomType = stringMeta(doc, "roomType");
        return roomType != null && roomType.contains(layout);
    }

    /**
     * 从用户查询中提取已知标签关键词。
     * 匹配策略：查询文本包含标签值（如 "南山 学区房" 包含 "学区房"）。
     */
    private List<String> extractTagKeywords(String query) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.trim();
        return KNOWN_TAGS.stream().filter(q::contains).toList();
    }

    /** 检查文档的 tags 元数据是否包含任意一个关键词 */
    private boolean matchesAnyTag(Document doc, List<String> keywords) {
        String tags = stringMeta(doc, "tags");
        if (tags == null || tags.isBlank()) return false;
        return keywords.stream().anyMatch(tags::contains);
    }

    /** 面积区间重叠判断：用户 [min, max] 与文档 [area_min, area_max] 是否有交集 */
    private boolean areaOverlap(Document doc, Integer minArea, Integer maxArea) {
        if (minArea == null && maxArea == null) return true;
        int docMin = intMeta(doc, "area_min", 0);
        int docMax = intMeta(doc, "area_max", 9999);
        if (docMin == 0 && docMax == 9999) return true; // 无面积数据不过滤
        if (minArea != null && docMax < minArea) return false;
        if (maxArea != null && docMin > maxArea) return false;
        return true;
    }

    /** 价格区间重叠判断 */
    private boolean priceOverlap(Document doc, Integer minPrice, Integer maxPrice) {
        if (minPrice == null && maxPrice == null) return true;
        int docPrice = intMeta(doc, "price_num", -1);
        if (docPrice <= 0) return true; // 无价格数据不过滤
        if (minPrice != null && docPrice < minPrice) return false;
        if (maxPrice != null && docPrice > maxPrice) return false;
        return true;
    }

    private boolean matchesProjectName(Document doc, String keyword) {
        String name = stringMeta(doc, "projectName");
        return name != null && name.contains(keyword);
    }

    // ==================== 降级兜底 ====================

    /** 无查询条件时返回热门推荐 */
    private String fallbackPopularHouses(int limit) {
        try {
            List<Document> docs = jdbcTemplate.query(
                    "SELECT content, metadata FROM " + tableName + " WHERE type = 'house' LIMIT ?",
                    (rs, i) -> new Document(rs.getString("content"),
                            parseJsonMap(rs.getString("metadata"))),
                    limit);
            lastSearchDocs.set(docs);
            if (docs.isEmpty()) return "暂无房源数据。";
            return docs.stream().map(this::formatHouseResult).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("热门推荐查询异常: {}", e.getMessage());
            return "查询异常，请稍后重试。";
        }
    }

    // ==================== 格式化 ====================

    private String formatHouseResult(Document doc) {
        String project = stringMeta(doc, "projectName");
        String city = stringMeta(doc, "city");
        String district = stringMeta(doc, "district");
        String roomType = stringMeta(doc, "roomType");
        String area = stringMeta(doc, "areaText");
        String price = stringMeta(doc, "priceText");
        String tags = stringMeta(doc, "tags");
        String houseId = stringMeta(doc, "houseId");
        String coverImage = stringMeta(doc, "coverImage");
        return String.format("项目：%s | 房源ID：%s | %s-%s | %s | %s | %s | %s | 封面图：%s",
                project, houseId != null ? houseId : "未知", city, district, roomType, area, price,
                tags != null ? tags : "",
                coverImage != null ? coverImage : "无");
    }

    private String formatProjectResult(Document doc) {
        String name = stringMeta(doc, "projectName");
        String city = stringMeta(doc, "city");
        String district = stringMeta(doc, "district");
        String address = stringMeta(doc, "address");
        String developer = stringMeta(doc, "developer");
        String houseCount = stringMeta(doc, "houseCount");
        String layouts = stringMeta(doc, "layouts");
        String tags = stringMeta(doc, "tags");
        String coverUrl = stringMeta(doc, "coverUrl");

        // 价格/面积区间
        String priceRange = "";
        String priceMin = stringMeta(doc, "priceMin");
        String priceMax = stringMeta(doc, "priceMax");
        if (priceMin != null && priceMax != null && !priceMin.equals(priceMax)) {
            priceRange = priceMin + "万-" + priceMax + "万";
        } else if (priceMin != null) {
            priceRange = priceMin + "万";
        }

        String areaRange = "";
        String areaMin = stringMeta(doc, "areaMin");
        String areaMax = stringMeta(doc, "areaMax");
        if (areaMin != null && areaMax != null && !areaMin.equals(areaMax)) {
            areaRange = areaMin + "-" + areaMax + "㎡";
        } else if (areaMin != null) {
            areaRange = areaMin + "㎡";
        }

        return String.format("楼盘：%s | %s-%s | %s套在售 | 价格：%s | 面积：%s | 户型：%s | 开发商：%s | 地址：%s | 标签：%s | 封面图：%s",
                name,
                city, district,
                houseCount != null ? houseCount : "?",
                !priceRange.isEmpty() ? priceRange : "暂无",
                !areaRange.isEmpty() ? areaRange : "暂无",
                layouts != null ? layouts : "暂无",
                developer != null ? developer : "暂无",
                address != null ? address : "暂无",
                tags != null ? tags : "无",
                coverUrl != null ? coverUrl : "无");
    }

    // ==================== 名称规范化 ====================

    /**
     * 规范化城市名，对齐 metadata 存储格式。
     * "深圳" → "深圳市", "南宁" → "南宁市", "深圳市" → "深圳市"（不变）
     */
    private String normalizeCity(String name) {
        if (name == null || name.isBlank()) return null;
        String trimmed = name.trim();
        if (trimmed.endsWith("市") || trimmed.endsWith("县")) return trimmed;
        return trimmed + "市";
    }

    /**
     * 规范化区县名，对齐 metadata 存储格式。
     * "南山" → "南山区", "福田" → "福田区", "南山区" → "南山区"（不变）
     */
    private String normalizeDistrict(String name) {
        if (name == null || name.isBlank()) return null;
        String trimmed = name.trim();
        if (trimmed.endsWith("区") || trimmed.endsWith("县") || trimmed.endsWith("市")) return trimmed;
        return trimmed + "区";
    }

    // ==================== 工具方法 ====================

    /**
     * 防御性清理 layout 参数。LLM 有时会传 "0室0厅" 当作"不限"，
     * 但 matchesLayout 会走 contains 匹配导致零命中。
     * 不含 "室" 或以 "0室" 开头的 layout 无法匹配任何真实户型，视为无效。
     */
    static String sanitizeLayout(String layout) {
        if (layout == null || layout.isBlank()) return null;
        if (!layout.contains("室")) return null;
        if (layout.startsWith("0室")) return null;
        return layout;
    }

    private String stringMeta(Document doc, String key) {
        Object val = doc.getMetadata().get(key);
        return val != null ? val.toString() : null;
    }

    private int intMeta(Document doc, String key, int defaultValue) {
        Object val = doc.getMetadata().get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { /* fall through */ }
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("JSON 解析失败: {}", e.getMessage());
            return Map.of();
        }
    }

    // ==================== ThreadLocal 参数追踪 ====================

    private void recordQueryParams(String searchMode, String city, String district,
                                    String layout, Integer minArea, Integer maxArea,
                                    Integer minPrice, Integer maxPrice,
                                    Integer houseType, int count) {
        lastQueryCount.set(count);
        Map<String, Object> params = new HashMap<>();
        if (searchMode != null) params.put("searchMode", searchMode);
        if (city != null) params.put("city", city);
        if (district != null) params.put("district", district);
        if (layout != null) params.put("layout", layout);
        if (minArea != null) params.put("minArea", minArea);
        if (maxArea != null) params.put("maxArea", maxArea);
        if (minPrice != null) params.put("minPrice", minPrice);
        if (maxPrice != null) params.put("maxPrice", maxPrice);
        if (houseType != null) params.put("houseType", houseType);
        lastQueryParams.set(params);
    }

    public int getLastQueryCount() {
        Integer v = lastQueryCount.get();
        return v != null ? v : -1;
    }

    public Map<String, Object> getLastQueryParams() {
        return lastQueryParams.get();
    }

    public List<Document> getLastSearchDocs() {
        return lastSearchDocs.get();
    }

    /** 仅清理查询统计/参数，保留搜索文档缓存供 PlanningFlow 提取结构化数据 */
    public void clearLastQuery() {
        lastQueryCount.remove();
        lastQueryParams.remove();
    }

    /** 在 PlanningFlow 提取完结构化数据后调用，清理文档缓存 */
    public void clearLastSearchDocs() {
        lastSearchDocs.remove();
    }
}
