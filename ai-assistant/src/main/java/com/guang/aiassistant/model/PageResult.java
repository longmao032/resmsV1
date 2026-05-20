package com.guang.aiassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通用分页响应封装
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageResult<T>(
        List<T> records,
        long total,
        long size,
        long current,
        long pages
) {
    @SuppressWarnings("unchecked")
    public static <T> PageResult<T> fromJson(JsonNode root, java.util.function.Function<JsonNode, T> mapper) {
        // 兼容 data / data.records 嵌套
        JsonNode data = root.has("data") ? root.get("data") : root;
        JsonNode recordsNode = data.has("records") ? data.get("records") : data;

        List<T> list = Collections.emptyList();
        if (recordsNode != null && recordsNode.isArray()) {
            list = new ArrayList<>();
            for (JsonNode item : recordsNode) {
                T obj = mapper.apply(item);
                if (obj != null) list.add(obj);
            }
        }

        return new PageResult<>(
                list,
                optLong(data, "total", list.size()),
                optLong(data, "size", 10),
                optLong(data, "current", 1),
                optLong(data, "pages", 1)
        );
    }

    private static long optLong(JsonNode node, String field, long fallback) {
        JsonNode f = node.get(field);
        return f != null ? f.asLong(fallback) : fallback;
    }
}
