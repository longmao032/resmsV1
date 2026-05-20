package com.guang.aiassistant.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

    // ==================== @Tool 方法 ====================

    @Tool(description = """
            搜索房源，支持三种模式：
            - precise：精准匹配结构化条件（城市/区域/户型/面积/价格/类型），速度最快，适合明确的筛选条件
            - semantic：自然语言语义搜索，适合模糊需求描述（如"适合养狗的安静小区"）
            - hybrid：结构化条件 + 语义偏好混合搜索（默认）

            重要：不限制的条件不要传参！例如不需要限制户型就别传 layout，
            不需要限制价格就别传 minPrice/maxPrice。不要传 0、空字符串、"0室0厅" 等占位值。""")
    public String queryHouses(
            @ToolParam(description = "搜索模式: precise/semantic/hybrid，默认 hybrid。无特殊需求时不传") String searchMode,
            @ToolParam(description = "城市，如'深圳'。不限制时不传") String city,
            @ToolParam(description = "区域，如'南山区'。不限制时不传") String district,
            @ToolParam(description = "户型，如'3室2厅'。用户未指定时不传，不要传'0室0厅'") String layout,
            @ToolParam(description = "最小面积(㎡)。不限制时不传，不要传0") Integer minArea,
            @ToolParam(description = "最大面积(㎡)。不限制时不传") Integer maxArea,
            @ToolParam(description = "最低总价(万元)。不限制时不传，不要传0") Integer minPrice,
            @ToolParam(description = "最高总价(万元)。不限制时不传") Integer maxPrice,
            @ToolParam(description = "类型：1新房 2二手房 3租房。不限制时不传") Integer houseType,
            @ToolParam(description = "自然语言偏好描述，仅 semantic/hybrid 模式使用。不限制时不传") String query) {

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

        recordQueryParams(searchMode, city, district, layout, minArea, maxArea, minPrice, maxPrice, houseType, 0);

        String result = switch (searchMode != null ? searchMode : "hybrid") {
            case "precise" -> preciseSearch(city, district, layout, minArea, maxArea,
                    minPrice, maxPrice, houseType);
            case "semantic" -> semanticSearch(query, 20);
            default -> hybridSearch(city, district, layout, minArea, maxArea,
                    minPrice, maxPrice, houseType, query);
        };

        lastQueryCount.set(result.equals("未找到匹配的房源。") || result.equals("查询异常，请稍后重试。") ? 0 : 1);
        return result;
    }

    @Tool(description = "搜索楼盘项目。按名称/城市/区域筛选，返回项目摘要")
    public String searchProjects(
            @ToolParam(description = "项目名称关键词") String keyword,
            @ToolParam(description = "城市") String city,
            @ToolParam(description = "区域") String district) {

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

    // ==================== 模式一：Precise 精准查询 ====================

    private String preciseSearch(String city, String district, String layout,
                                  Integer minArea, Integer maxArea,
                                  Integer minPrice, Integer maxPrice,
                                  Integer houseType) {
        // 全部走 metadata JSONB 路径，不依赖物理列（物理列由 SchemaMigration 异步添加）
        StringBuilder sql = new StringBuilder(
                "SELECT content, metadata FROM " + tableName + " WHERE metadata->>'type' = 'house'");
        List<Object> params = new ArrayList<>();

        if (city != null && !city.isBlank()) {
            sql.append(" AND metadata->>'city' = ?");
            params.add(city);
        }
        if (district != null && !district.isBlank()) {
            sql.append(" AND metadata->>'district' = ?");
            params.add(district);
        }
        if (houseType != null) {
            sql.append(" AND metadata->>'houseType' = ?");
            params.add(String.valueOf(houseType));
        }
        // price/area 不在此过滤，metadata 中 price_num/area_min 可能不存在 → Java 层后置过滤

        sql.append(" LIMIT ?");
        params.add(200);  // 扩大取数以补偿后置过滤

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

        // 后置过滤：layout(文本) + area(文本解析) + price(文本解析)
        var filtered = docs.stream()
                .filter(d -> layout == null || layout.isBlank() || matchesLayout(d, layout))
                .filter(d -> areaOverlap(d, minArea, maxArea))
                .filter(d -> priceOverlap(d, minPrice, maxPrice))
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
                .limit(20)
                .toList();

        lastSearchDocs.set(filtered);

        if (filtered.isEmpty()) return "未找到匹配的房源。";
        return filtered.stream().map(this::formatHouseResult).collect(Collectors.joining("\n"));
    }

    // ==================== 后置过滤 ====================

    private boolean matchesLayout(Document doc, String layout) {
        String roomType = stringMeta(doc, "roomType");
        return roomType != null && roomType.contains(layout);
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
                    "SELECT content, metadata FROM " + tableName + " WHERE metadata->>'type' = 'house' LIMIT ?",
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
        if (name == null || name.isBlank()) return name;
        String trimmed = name.trim();
        if (trimmed.endsWith("市") || trimmed.endsWith("县")) return trimmed;
        return trimmed + "市";
    }

    /**
     * 规范化区县名，对齐 metadata 存储格式。
     * "南山" → "南山区", "福田" → "福田区", "南山区" → "南山区"（不变）
     */
    private String normalizeDistrict(String name) {
        if (name == null || name.isBlank()) return name;
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
