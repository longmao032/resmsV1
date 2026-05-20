package com.guang.aiassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 楼盘列表 API 响应（list-all 接口返回非分页的扁平项目数组）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectListResponse(
        @JsonProperty("code") int code,
        @JsonProperty("message") String message,
        @JsonProperty("data") List<Project> data
) {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 从 JsonNode 解析楼盘列表
     */
    public static List<Project> fromJson(JsonNode root) {
        if (root == null) return Collections.emptyList();

        try {
            // 尝试标准 data 字段
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (data != null && data.isArray()) {
                List<Project> result = new ArrayList<>();
                for (JsonNode item : data) {
                    Project p = MAPPER.treeToValue(item, Project.class);
                    if (p != null) result.add(p);
                }
                return result;
            }
        } catch (Exception e) {
            // fallback to raw data
        }
        return Collections.emptyList();
    }
}
