package com.guang.aiassistant.service;

import com.guang.aiassistant.client.EstateSystemClient;
import com.guang.aiassistant.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据同步服务 — 按需调用，暂不启用定时同步
 */
@Service
public class DataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DataSyncService.class);

    private final EstateSystemClient client;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public DataSyncService(EstateSystemClient client, VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.client = client;
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 全量同步房源数据 — 展平 Project → ProjectHouse，每个房源一条向量记录。
     * 当前仅支持手动调用，定时同步待后续开启。
     */
    public int syncAllHouses() {
        log.info("开始全量同步房源数据...");

        List<Project> projects = client.listAllProjects();
        log.info("获取到 {} 个楼盘", projects.size());

        // ---- 1. 项目级文档 ----
        List<Document> projectDocs = new ArrayList<>();
        for (Project project : projects) {
            if (project.houses() != null && !project.houses().isEmpty()) {
                projectDocs.add(toProjectDocument(project));
            }
        }
        if (!projectDocs.isEmpty()) {
            vectorStore.delete(new FilterExpressionBuilder()
                    .eq("type", "project")
                    .build());
            writeBatch(projectDocs, "项目");
        }

        // ---- 2. 房源级文档 ----
        List<Document> houseDocs = new ArrayList<>();
        for (Project project : projects) {
            for (Project.ProjectHouse house : project.houses()) {
                houseDocs.add(toDocument(project, house));
            }
        }

        if (!houseDocs.isEmpty()) {
            vectorStore.delete(new FilterExpressionBuilder()
                    .eq("type", "house")
                    .build());
            writeBatch(houseDocs, "房源");
        }

        // 回填物理列：metadata 已含 price_num/area_min/area_max，同步到物理列
        backfillPhysicalColumns();

        log.info("数据同步完成：{} 个楼盘 → {} 条项目 + {} 条房源向量记录",
                projects.size(), projectDocs.size(), houseDocs.size());
        return houseDocs.size();
    }

    /**
     * 从 metadata JSONB 回填物理列，支持增量（只更新物理列为 NULL 的行）。
     */
    private void backfillPhysicalColumns() {
        try {
            int updated = jdbcTemplate.update("""
                    UPDATE vector_store
                    SET type      = metadata->>'type',
                        city      = metadata->>'city',
                        district  = metadata->>'district',
                        price_num = COALESCE((metadata->>'price_num')::int, 0),
                        area_min  = COALESCE((metadata->>'area_min')::int, 0),
                        area_max  = COALESCE((metadata->>'area_max')::int, 0)
                    WHERE type IS NULL
                    """);
            if (updated > 0) {
                log.info("物理列回填完成，更新 {} 行", updated);
            }
        } catch (Exception e) {
            log.warn("物理列回填失败: {}", e.getMessage());
        }
    }


    private void writeBatch(List<Document> docs, String label) {
        int batchSize = 10;
        for (int i = 0; i < docs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, docs.size());
            vectorStore.add(docs.subList(i, end));
            log.info("{}文档批次 {}/{} 写入完成", label, (i / batchSize) + 1,
                    (docs.size() + batchSize - 1) / batchSize);
        }
    }

    private Document toProjectDocument(Project project) {
        String text = buildProjectEmbeddingText(project);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("type", "project");
        putIfNonNull(metadata, "projectId", project.id());
        putIfNonNull(metadata, "projectName", project.projectName());
        putIfNonNull(metadata, "city", project.city());
        putIfNonNull(metadata, "district", project.district());
        putIfNonNull(metadata, "address", project.address());
        putIfNonNull(metadata, "developer", project.developer());
        putIfNonNull(metadata, "propertyCompany", project.propertyCompany());
        putIfNonNull(metadata, "totalHouseholds", project.totalHouseholds());
        if (project.propertyFee() != null) metadata.put("propertyFee", project.propertyFee().toPlainString());
        if (project.plotRatio() != null) metadata.put("plotRatio", project.plotRatio().toPlainString());
        putIfNonNull(metadata, "greeningRate", project.greeningRate());
        putIfNonNull(metadata, "tags", project.tags() != null ? String.join("、", project.tags()) : null);
        putIfNonNull(metadata, "coverUrl", project.coverUrl());

        var houses = project.houses();
        metadata.put("houseCount", houses != null ? houses.size() : 0);

        if (houses != null && !houses.isEmpty()) {
            int priceMin = Integer.MAX_VALUE, priceMax = 0;
            int areaMin = Integer.MAX_VALUE, areaMax = 0;
            Set<String> layoutSet = new LinkedHashSet<>();
            for (var h : houses) {
                int p = parsePriceToNumber(h.priceText());
                if (p > 0) { priceMin = Math.min(priceMin, p); priceMax = Math.max(priceMax, p); }
                var area = parseArea(h.areaText());
                if (area[0] > 0) areaMin = Math.min(areaMin, area[0]);
                if (area[1] > 0) areaMax = Math.max(areaMax, area[1]);
                if (h.roomType() != null) layoutSet.add(h.roomType());
            }
            if (priceMin < Integer.MAX_VALUE) {
                metadata.put("priceMin", priceMin);
                metadata.put("priceMax", priceMax);
            }
            if (areaMin < Integer.MAX_VALUE) {
                metadata.put("areaMin", areaMin);
                metadata.put("areaMax", areaMax);
            }
            if (!layoutSet.isEmpty()) metadata.put("layouts", String.join("、", layoutSet));
        }

        return new Document(text, metadata);
    }

    private String buildProjectEmbeddingText(Project project) {
        var sb = new StringBuilder();
        sb.append("楼盘：").append(project.projectName()).append("；");
        if (project.city() != null) sb.append("城市：").append(project.city()).append("；");
        if (project.district() != null) sb.append("区域：").append(project.district()).append("；");
        if (project.address() != null) sb.append("地址：").append(project.address()).append("；");
        if (project.developer() != null) sb.append("开发商：").append(project.developer()).append("；");
        if (project.propertyCompany() != null) sb.append("物业：").append(project.propertyCompany()).append("；");
        if (project.totalHouseholds() != null) sb.append("总户数：").append(project.totalHouseholds()).append("；");
        if (project.plotRatio() != null) sb.append("容积率：").append(project.plotRatio()).append("；");
        if (project.greeningRate() != null) sb.append("绿化率：").append(project.greeningRate()).append("%；");
        if (project.propertyFee() != null) sb.append("物业费：").append(project.propertyFee()).append("元/㎡；");
        if (project.tags() != null && !project.tags().isEmpty())
            sb.append("标签：").append(String.join("、", project.tags())).append("；");

        var houses = project.houses();
        if (houses != null && !houses.isEmpty()) {
            sb.append("在售").append(houses.size()).append("套，");
            // 户型汇总
            var layouts = houses.stream().map(Project.ProjectHouse::roomType)
                    .filter(Objects::nonNull).distinct().toList();
            if (!layouts.isEmpty()) sb.append("户型包括").append(String.join("、", layouts)).append("，");
            // 价格范围
            int pMin = Integer.MAX_VALUE, pMax = 0;
            for (var h : houses) {
                int p = parsePriceToNumber(h.priceText());
                if (p > 0) { pMin = Math.min(pMin, p); pMax = Math.max(pMax, p); }
            }
            if (pMin < Integer.MAX_VALUE) sb.append("价格").append(pMin).append("万-").append(pMax).append("万，");
            // 面积范围
            int aMin = Integer.MAX_VALUE, aMax = 0;
            for (var h : houses) {
                int[] a = parseArea(h.areaText());
                if (a[0] > 0) aMin = Math.min(aMin, a[0]);
                if (a[1] > 0) aMax = Math.max(aMax, a[1]);
            }
            if (aMin < Integer.MAX_VALUE) sb.append("面积").append(aMin).append("-").append(aMax).append("㎡");
        }
        return sb.toString();
    }

    private int[] parseArea(String areaText) {
        if (areaText == null || areaText.isBlank()) return new int[]{0, 9999};
        try {
            String cleaned = areaText.replace("㎡", "").trim();
            if (cleaned.matches(".*[\\-~].*")) {
                String[] parts = cleaned.split("[\\-~]");
                return new int[]{Integer.parseInt(parts[0].replaceAll("[^\\d]", "")),
                        Integer.parseInt(parts[1].replaceAll("[^\\d]", ""))};
            }
            int v = Integer.parseInt(cleaned.replaceAll("[^\\d]", ""));
            return new int[]{v, v};
        } catch (Exception e) {
            return new int[]{0, 9999};
        }
    }

    private Document toDocument(Project project, Project.ProjectHouse house) {
        String text = buildEmbeddingText(project, house);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("type", "house");
        putIfNonNull(metadata, "houseId", house.id());
        putIfNonNull(metadata, "projectId", project.id());
        putIfNonNull(metadata, "projectName", project.projectName());
        putIfNonNull(metadata, "city", project.city());
        putIfNonNull(metadata, "district", project.district());
        // 封面图：房源自身封面优先，楼盘封面兜底
        String cover = house.coverUrl() != null ? house.coverUrl() : project.coverUrl();
        putIfNonNull(metadata, "coverImage", cover);
        putIfNonNull(metadata, "houseType", house.houseType());
        putIfNonNull(metadata, "roomType", house.roomType());
        putIfNonNull(metadata, "priceText", house.priceText());
        putIfNonNull(metadata, "areaText", house.areaText());
        putIfNonNull(metadata, "orientation", house.orientation());
        putIfNonNull(metadata, "floorInfo", house.floorInfo());
        putIfNonNull(metadata, "tags", house.tags() != null ? String.join("、", house.tags()) : null);
        putIfNonNull(metadata, "description", house.description());

        // 数值化字段 — 供精准查询和 Hybrid 后置过滤使用
        metadata.put("price_num", parsePriceToNumber(house.priceText()));
        parseAndPutArea(house.areaText(), metadata);

        return new Document(text, metadata);
    }

    private static void putIfNonNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    // ==================== 数值化解析 ====================

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("([\\d,]+\\.?\\d*)");

    /**
     * 从价格文本提取数值（万元）。修复 B-02。
     * "1,500万" → 1500, "1500.5万" → 1501, "价格面议" → 0
     */
    private int parsePriceToNumber(String priceText) {
        if (priceText == null || priceText.isBlank()) return 0;
        Matcher matcher = NUMERIC_PATTERN.matcher(priceText);
        if (matcher.find()) {
            String raw = matcher.group(1).replace(",", "");
            try {
                return (int) Math.round(Double.parseDouble(raw));
            } catch (NumberFormatException e) {
                log.warn("价格解析失败: {}", priceText);
                return 0;
            }
        }
        return 0;
    }

    /**
     * 解析面积文本并写入 metadata。独立 try-catch，修复 B-03。
     * "约85-120㎡" → area_min=85, area_max=120
     */
    private void parseAndPutArea(String areaText, Map<String, Object> metadata) {
        try {
            if (areaText == null || areaText.isBlank()) {
                metadata.put("area_min", 0);
                metadata.put("area_max", 9999);
                return;
            }
            String cleaned = areaText.replace("㎡", "").trim();
            if (cleaned.matches(".*[\\-~].*")) {
                String[] parts = cleaned.split("[\\-~]");
                int min = Integer.parseInt(parts[0].replaceAll("[^\\d]", ""));
                int max = Integer.parseInt(parts[1].replaceAll("[^\\d]", ""));
                metadata.put("area_min", min);
                metadata.put("area_max", max);
            } else if (!cleaned.isBlank()) {
                int v = Integer.parseInt(cleaned.replaceAll("[^\\d]", ""));
                metadata.put("area_min", v);
                metadata.put("area_max", v);
            } else {
                metadata.put("area_min", 0);
                metadata.put("area_max", 9999);
            }
        } catch (Exception e) {
            log.warn("面积字段解析失败，降级处理。areaText={}", areaText, e);
            metadata.put("area_min", 0);
            metadata.put("area_max", 9999);
        }
    }

    private String buildEmbeddingText(Project project, Project.ProjectHouse house) {
        StringBuilder sb = new StringBuilder();
        sb.append("楼盘：").append(project.projectName()).append("；");
        if (project.city() != null) sb.append("城市：").append(project.city()).append("；");
        if (project.district() != null) sb.append("区域：").append(project.district()).append("；");
        if (project.address() != null) sb.append("地址：").append(project.address()).append("；");
        if (house.roomType() != null) sb.append("户型：").append(house.roomType()).append("；");
        if (house.areaText() != null) sb.append("面积：").append(house.areaText()).append("；");
        if (house.priceText() != null) sb.append("总价：").append(house.priceText()).append("；");
        if (house.unitPriceText() != null) sb.append("单价：").append(house.unitPriceText()).append("；");
        if (house.floorInfo() != null) sb.append("楼层：").append(house.floorInfo()).append("；");
        if (house.orientation() != null) sb.append("朝向：").append(house.orientation()).append("；");
        if (house.houseType() != null) {
            String typeLabel = switch (house.houseType()) {
                case 1 -> "新房";
                case 2 -> "二手房";
                case 3 -> "租房";
                default -> "其他";
            };
            sb.append("类型：").append(typeLabel).append("；");
        }
        if (project.tags() != null && !project.tags().isEmpty()) {
            sb.append("楼盘标签：").append(String.join("、", project.tags())).append("；");
        }
        if (house.tags() != null && !house.tags().isEmpty()) {
            sb.append("房源标签：").append(String.join("、", house.tags())).append("；");
        }
        if (house.description() != null) {
            sb.append("描述：").append(house.description()).append("；");
        }
        return sb.toString();
    }
}
