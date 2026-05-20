package com.guang.aiassistant.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON 工具 — 将 JsonNode 转为类型安全的 record
 */
final class JsonUtils {

    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule()
                    .addDeserializer(LocalDateTime.class,
                            new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static <T> T parse(JsonNode node, Class<T> type) {
        try {
            // 如果已是完整的 data 包裹，尝试提取 data 字段后再解析
            if (node.has("code") && node.has("data")) {
                JsonNode data = node.get("data");
                // 如果 data 是数组或 records 结构，直接返回节点让调用方处理
                if (data.isArray() || data.has("records")) {
                    return MAPPER.treeToValue(data, type);
                }
            }
            return MAPPER.treeToValue(node, type);
        } catch (Exception e) {
            log.warn("解析 {} 失败: {}", type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private JsonUtils() {}
}
