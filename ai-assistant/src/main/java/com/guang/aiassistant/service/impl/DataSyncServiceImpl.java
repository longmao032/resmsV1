package com.guang.aiassistant.service.impl;

import com.guang.aiassistant.client.EstateSystemClient;
import com.guang.aiassistant.model.Project;
import com.guang.aiassistant.model.dto.HouseSyncDTO;
import com.guang.aiassistant.model.dto.ProjectSyncDTO;
import com.guang.aiassistant.repository.VectorStoreRepository;
import com.guang.aiassistant.service.DataSyncService;
import com.guang.aiassistant.service.SearchCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataSyncServiceImpl implements DataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DataSyncServiceImpl.class);

    private final EstateSystemClient client;
    private final VectorStore vectorStore;
    private final VectorStoreRepository vectorStoreRepository;
    private final SearchCacheService searchCacheService;

    public DataSyncServiceImpl(EstateSystemClient client, VectorStore vectorStore,
                               VectorStoreRepository vectorStoreRepository,
                               SearchCacheService searchCacheService) {
        this.client = client;
        this.vectorStore = vectorStore;
        this.vectorStoreRepository = vectorStoreRepository;
        this.searchCacheService = searchCacheService;
    }

    @Override
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

        backfillPhysicalColumns();
        searchCacheService.invalidateAll();

        log.info("数据同步完成：{} 个楼盘 → {} 条项目 + {} 条房源向量记录",
                projects.size(), projectDocs.size(), houseDocs.size());
        return houseDocs.size();
    }

    private void backfillPhysicalColumns() {
        try {
            vectorStoreRepository.backfillPhysicalColumns();
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
            var layouts = houses.stream().map(Project.ProjectHouse::roomType)
                    .filter(Objects::nonNull).distinct().toList();
            if (!layouts.isEmpty()) sb.append("户型包括").append(String.join("、", layouts)).append("，");
            int pMin = Integer.MAX_VALUE, pMax = 0;
            for (var h : houses) {
                int p = parsePriceToNumber(h.priceText());
                if (p > 0) { pMin = Math.min(pMin, p); pMax = Math.max(pMax, p); }
            }
            if (pMin < Integer.MAX_VALUE) sb.append("价格").append(pMin).append("万-").append(pMax).append("万，");
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

        metadata.put("price_num", parsePriceToNumber(house.priceText()));
        parseAndPutArea(house.areaText(), metadata);

        return new Document(text, metadata);
    }

    private static void putIfNonNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("([\\d,]+\\.?\\d*)");

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

    // ==================== 增量同步接口 ====================

    @Override
    public void upsertHouse(HouseSyncDTO dto) {
        log.info("增量同步房源(Upsert): houseId={}", dto.getHouseId());
        Document doc = toDocumentFromDto(dto);
        vectorStore.add(List.of(doc));
        backfillPhysicalColumns();
        searchCacheService.invalidateAll();
    }

    @Override
    public void upsertHouseBatch(List<HouseSyncDTO> dtos) {
        log.info("增量批量同步房源(Upsert): size={}", dtos.size());
        List<Document> docs = dtos.stream().map(this::toDocumentFromDto).toList();
        vectorStore.add(docs);
        backfillPhysicalColumns();
        searchCacheService.invalidateAll();
    }

    @Override
    public void deleteHouse(Integer houseId) {
        log.info("增量同步房源(Delete): houseId={}", houseId);
        vectorStore.delete(List.of("house_" + houseId));
        searchCacheService.invalidateAll();
    }

    @Override
    public void upsertProject(ProjectSyncDTO dto) {
        log.info("增量同步楼盘(Upsert): projectId={}", dto.getProjectId());
        Document doc = toProjectDocumentFromDto(dto);
        vectorStore.add(List.of(doc));
        backfillPhysicalColumns();
        searchCacheService.invalidateAll();
    }

    @Override
    public void deleteProject(Integer projectId) {
        log.info("增量同步楼盘(Delete): projectId={}", projectId);
        vectorStore.delete(List.of("project_" + projectId));
        searchCacheService.invalidateAll();
    }

    private Document toDocumentFromDto(HouseSyncDTO dto) {
        String text = buildEmbeddingTextFromDto(dto);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("type", "house");
        putIfNonNull(metadata, "houseId", dto.getHouseId());
        putIfNonNull(metadata, "projectId", dto.getProjectId());
        putIfNonNull(metadata, "projectName", dto.getProjectName());
        putIfNonNull(metadata, "city", dto.getCity());
        putIfNonNull(metadata, "district", dto.getDistrict());
        putIfNonNull(metadata, "coverImage", dto.getCoverImage());
        putIfNonNull(metadata, "houseType", dto.getHouseType() != null ? dto.getHouseType().intValue() : null);
        putIfNonNull(metadata, "roomType", dto.getRoomType());
        putIfNonNull(metadata, "priceText", dto.getPriceText());
        putIfNonNull(metadata, "areaText", dto.getAreaText());
        putIfNonNull(metadata, "orientation", dto.getOrientation());
        putIfNonNull(metadata, "floorInfo", dto.getFloorInfo());
        putIfNonNull(metadata, "tags", dto.getTags() != null ? String.join("、", dto.getTags()) : null);
        putIfNonNull(metadata, "description", dto.getDescription());

        metadata.put("price_num", parsePriceToNumber(dto.getPriceText()));
        parseAndPutArea(dto.getAreaText(), metadata);

        return new Document("house_" + dto.getHouseId(), text, metadata);
    }

    private String buildEmbeddingTextFromDto(HouseSyncDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("楼盘：").append(dto.getProjectName()).append("；");
        if (dto.getCity() != null) sb.append("城市：").append(dto.getCity()).append("；");
        if (dto.getDistrict() != null) sb.append("区域：").append(dto.getDistrict()).append("；");
        if (dto.getRoomType() != null) sb.append("户型：").append(dto.getRoomType()).append("；");
        if (dto.getAreaText() != null) sb.append("面积：").append(dto.getAreaText()).append("；");
        if (dto.getPriceText() != null) sb.append("总价：").append(dto.getPriceText()).append("；");
        if (dto.getFloorInfo() != null) sb.append("楼层：").append(dto.getFloorInfo()).append("；");
        if (dto.getOrientation() != null) sb.append("朝向：").append(dto.getOrientation()).append("；");
        if (dto.getHouseType() != null) {
            String typeLabel = switch (dto.getHouseType().intValue()) {
                case 1 -> "新房";
                case 2 -> "二手房";
                case 3 -> "租房";
                default -> "其他";
            };
            sb.append("类型：").append(typeLabel).append("；");
        }
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            sb.append("房源标签：").append(String.join("、", dto.getTags())).append("；");
        }
        if (dto.getDescription() != null) {
            sb.append("描述：").append(dto.getDescription()).append("；");
        }
        return sb.toString();
    }

    private Document toProjectDocumentFromDto(ProjectSyncDTO dto) {
        String text = buildProjectEmbeddingTextFromDto(dto);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("type", "project");
        putIfNonNull(metadata, "projectId", dto.getProjectId());
        putIfNonNull(metadata, "projectName", dto.getProjectName());
        putIfNonNull(metadata, "city", dto.getCity());
        putIfNonNull(metadata, "district", dto.getDistrict());
        putIfNonNull(metadata, "address", dto.getAddress());
        putIfNonNull(metadata, "developer", dto.getDeveloper());
        putIfNonNull(metadata, "propertyCompany", dto.getPropertyCompany());
        putIfNonNull(metadata, "totalHouseholds", dto.getTotalHouseholds());
        if (dto.getPropertyFee() != null) metadata.put("propertyFee", dto.getPropertyFee().toPlainString());
        if (dto.getPlotRatio() != null) metadata.put("plotRatio", dto.getPlotRatio().toPlainString());
        putIfNonNull(metadata, "greeningRate", dto.getGreeningRate());
        putIfNonNull(metadata, "tags", dto.getTags() != null ? String.join("、", dto.getTags()) : null);
        putIfNonNull(metadata, "coverUrl", dto.getCoverUrl());

        return new Document("project_" + dto.getProjectId(), text, metadata);
    }

    private String buildProjectEmbeddingTextFromDto(ProjectSyncDTO dto) {
        var sb = new StringBuilder();
        sb.append("楼盘：").append(dto.getProjectName()).append("；");
        if (dto.getCity() != null) sb.append("城市：").append(dto.getCity()).append("；");
        if (dto.getDistrict() != null) sb.append("区域：").append(dto.getDistrict()).append("；");
        if (dto.getAddress() != null) sb.append("地址：").append(dto.getAddress()).append("；");
        if (dto.getDeveloper() != null) sb.append("开发商：").append(dto.getDeveloper()).append("；");
        if (dto.getPropertyCompany() != null) sb.append("物业：").append(dto.getPropertyCompany()).append("；");
        if (dto.getTotalHouseholds() != null) sb.append("总户数：").append(dto.getTotalHouseholds()).append("；");
        if (dto.getPlotRatio() != null) sb.append("容积率：").append(dto.getPlotRatio()).append("；");
        if (dto.getGreeningRate() != null) sb.append("绿化率：").append(dto.getGreeningRate()).append("%；");
        if (dto.getPropertyFee() != null) sb.append("物业费：").append(dto.getPropertyFee()).append("元/㎡；");
        if (dto.getTags() != null && !dto.getTags().isEmpty())
            sb.append("标签：").append(String.join("、", dto.getTags())).append("；");
        return sb.toString();
    }

    @Override
    public List<Integer> getAllHouseIds() {
        return vectorStoreRepository.getAllHouseIds();
    }
}

