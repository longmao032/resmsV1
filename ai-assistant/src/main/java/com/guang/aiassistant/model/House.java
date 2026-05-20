package com.guang.aiassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 房源 / 楼盘数据模型
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record House(
        @JsonProperty("id") String id,
        @JsonProperty("projectName") String projectName,
        @JsonProperty("projectNameEn") String projectNameEn,
        @JsonProperty("address") String address,
        @JsonProperty("district") String district,
        @JsonProperty("city") String city,
        @JsonProperty("area") BigDecimal area,
        @JsonProperty("totalPrice") BigDecimal totalPrice,
        @JsonProperty("unitPrice") BigDecimal unitPrice,
        @JsonProperty("layout") String layout,
        @JsonProperty("decoration") String decoration,
        @JsonProperty("status") String status,
        @JsonProperty("description") String description,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("imageUrl") String imageUrl,
        @JsonProperty("coverUrl") String coverUrl,
        @JsonProperty("houseNo") String houseNo,
        @JsonProperty("houseType") Integer houseType
) {

    /**
     * 从 JsonNode 解析房源列表（兼容多种 JSON 嵌套格式）
     */
    public static List<House> fromJsonNode(JsonNode root) {
        List<House> houses = new ArrayList<>();
        if (root == null) return houses;

        // 尝试从常见嵌套路径提取列表
        JsonNode list = root;
        for (String path : List.of("data.records", "data.list", "data.items", "data", "records", "list", "items")) {
            JsonNode resolved = root.at("/" + path.replace(".", "/"));
            if (resolved != null && resolved.isArray()) {
                list = resolved;
                break;
            }
        }

        if (list.isArray()) {
            for (JsonNode item : list) {
                House house = fromSingle(item);
                if (house != null) houses.add(house);
            }
        } else if (list.isObject()) {
            House house = fromSingle(list);
            if (house != null) houses.add(house);
        }
        return houses;
    }

    private static House fromSingle(JsonNode node) {
        try {
            // 尝试 JSON 字段映射，如果字段名不同则手动提取
            if (node.has("id") || node.has("projectId")) {
                return new House(
                        textField(node, "id", "projectId"),
                        textField(node, "projectName", "name", "title"),
                        textField(node, "projectNameEn", "nameEn"),
                        textField(node, "address", "location"),
                        textField(node, "district", "area", "region"),
                        textField(node, "city"),
                        decimalField(node, "area", "buildArea"),
                        decimalField(node, "totalPrice", "price", "totalPrice"),
                        decimalField(node, "unitPrice", "avgPrice"),
                        textField(node, "layout", "roomType", "houseType"),
                        textField(node, "decoration", "fitment"),
                        textField(node, "status", "saleStatus"),
                        textField(node, "description", "desc", "intro"),
                        tagsField(node),
                        textField(node, "imageUrl", "image", "pic"),
                        textField(node, "coverUrl", "coverImage"),
                        textField(node, "houseNo", "houseNumber"),
                        intField(node, "houseType")
                );
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String textField(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode field = node.get(key);
            if (field != null && field.isTextual() && !field.asText().isBlank()) {
                return field.asText();
            }
        }
        return null;
    }

    private static Integer intField(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode field = node.get(key);
            if (field != null && !field.asText().isBlank()) {
                try {
                    return Integer.parseInt(field.asText());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private static BigDecimal decimalField(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode field = node.get(key);
            if (field != null && !field.asText().isBlank()) {
                try {
                    return new BigDecimal(field.asText());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private static List<String> tagsField(JsonNode node) {
        JsonNode tags = node.get("tags");
        if (tags != null && tags.isArray()) {
            List<String> result = new ArrayList<>();
            for (JsonNode t : tags) {
                if (t.isTextual()) result.add(t.asText());
            }
            return result;
        }
        return List.of();
    }

    /**
     * 生成用于 embedding 的文本描述
     */
    public String toEmbeddingText() {
        StringBuilder sb = new StringBuilder();
        if (projectName != null) sb.append("楼盘：").append(projectName).append("；");
        if (city != null) sb.append("城市：").append(city).append("；");
        if (district != null) sb.append("区域：").append(district).append("；");
        if (address != null) sb.append("地址：").append(address).append("；");
        if (layout != null) sb.append("户型：").append(layout).append("；");
        if (area != null) sb.append("面积：").append(area).append("㎡；");
        if (totalPrice != null) sb.append("总价：").append(totalPrice).append("万；");
        if (unitPrice != null) sb.append("单价：").append(unitPrice).append("元/㎡；");
        if (decoration != null) sb.append("装修：").append(decoration).append("；");
        if (status != null) sb.append("状态：").append(status).append("；");
        if (description != null) sb.append("描述：").append(description).append("；");
        if (houseType != null) {
            String typeLabel = switch (houseType) {
                case 1 -> "新房";
                case 2 -> "二手房";
                case 3 -> "租房";
                default -> "其他";
            };
            sb.append("类型：").append(typeLabel).append("；");
        }
        if (tags != null && !tags.isEmpty()) sb.append("标签：").append(String.join("、", tags)).append("；");
        return sb.toString();
    }
}
