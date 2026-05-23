package com.guang.aiassistant.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.guang.aiassistant.client.EstateSystemClient;
import com.guang.aiassistant.core.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 客户画像工具 —— 提供结构化画像和六大因子联动价格重算能力
 */
@Component
public class CustomerProfileTool {

    private static final Logger log = LoggerFactory.getLogger(CustomerProfileTool.class);

    private final EstateSystemClient client;
    private final ThreadLocal<Boolean> lastHasData = new ThreadLocal<>();
    /** 缓存最近一次画像数据（供 UP Engine 联动使用） */
    private final ThreadLocal<JsonNode> cachedProfile = new ThreadLocal<>();

    // ========== UP Engine 常量 ==========
    private static final Map<String, Double> HOUSE_TYPE_FACTORS = Map.of(
            "villa", 2.8,
            "new_house", 1.1,
            "resale", 1.0,
            "apartment", 0.7,
            "rental", 0.5
    );

    public CustomerProfileTool(EstateSystemClient client) {
        this.client = client;
    }

    // 画像数据由 PersonaInjector 静默注入，不再作为 Tool 暴露给 LLM。
    // 方法体保留供程序化调用。
    public String getCustomerProfile() {
        String userId = UserContext.getCurrentUserId();
        if (userId == null || !userId.matches("\\d+")) {
            lastHasData.set(false);
            return "=== 客户画像 ===\n暂无客户数据，无法生成画像。请引导用户多浏览、收藏房源后再获取画像。";
        }

        try {
            JsonNode profileJson = client.getUserProfile(userId);
            if (profileJson == null) {
                lastHasData.set(false);
                return "=== 客户画像 ===\n获取画像失败，请稍后重试。";
            }

            JsonNode data = profileJson.has("data") ? profileJson.get("data") : profileJson;
            cachedProfile.set(data);

            StringBuilder sb = new StringBuilder("=== 客户意向画像 ===\n");

            // 紧迫度
            String urgency = textOrDefault(data, "urgencyLevel", "LOW");
            sb.append("【购房紧迫度】").append(urgency).append("\n");

            // 硬性约束
            JsonNode hard = data.get("hardConstraints");
            if (hard != null) {
                sb.append("【硬性约束】\n");
                sb.append("  城市: ").append(textOrDefault(hard, "city", "未知")).append("\n");
                sb.append("  核心区域: ").append(arrayToString(hard.get("districts"))).append("\n");
                sb.append("  预算区间: ").append(numOrDefault(hard, "minPriceWan", 0))
                        .append(" ~ ").append(numOrDefault(hard, "maxPriceWan", 0)).append(" 万元\n");
                sb.append("  面积区间: ").append(numOrDefault(hard, "minArea", 0))
                        .append(" ~ ").append(numOrDefault(hard, "maxArea", 0)).append(" ㎡\n");
                sb.append("  偏好户型: ").append(arrayToString(hard.get("layoutRooms"))).append(" 室\n");
            }

            // 软偏好
            JsonNode soft = data.get("softPreferences");
            if (soft != null) {
                sb.append("【软偏好】\n");
                sb.append("  房屋类型: ").append(textOrDefault(soft, "preferredHouseType", "未知")).append("\n");
                sb.append("  装修标准: ").append(textOrDefault(soft, "preferredDecoration", "未知")).append("\n");
            }

            // 场景标签
            sb.append("【场景标签】").append(arrayToString(data.get("scenarioTags"))).append("\n");

            // 单价锚点
            double anchor = numOrDefault(data, "unitPriceAnchorWan", 0);
            sb.append("【单价锚点】").append(String.format("%.2f", anchor)).append(" 万元/㎡\n");

            // 区县均价指数
            JsonNode districtIndex = data.get("districtPriceIndex");
            if (districtIndex != null && districtIndex.isObject()) {
                sb.append("【区县均价指数（万元/㎡）】\n");
                districtIndex.fields().forEachRemaining(field ->
                        sb.append("  ").append(field.getKey()).append(": ")
                                .append(String.format("%.2f", field.getValue().asDouble())).append("\n"));
            }

            // Top5 意向房源
            JsonNode topHouses = data.get("topIntentHouses");
            if (topHouses != null && topHouses.isArray() && !topHouses.isEmpty()) {
                sb.append("【意向最强 Top5 房源】\n");
                for (JsonNode h : topHouses) {
                    sb.append("  ").append(textOrDefault(h, "title", ""))
                            .append(" | 得分: ").append(String.format("%.1f", numOrDefault(h, "score", 0)))
                            .append(" | ").append(textOrDefault(h, "district", ""))
                            .append(" | ").append(numOrDefault(h, "priceWan", 0)).append("万\n");
                }
            }

            sb.append("\n提示: 当用户提出新的区域/面积/房屋类型等条件时，请调用 calculateLinkedPrice 工具进行价格联动重算，获取更精准的预算区间。");

            boolean hasAny = anchor > 0 || (topHouses != null && !topHouses.isEmpty());
            lastHasData.set(hasAny);
            return sb.toString();

        } catch (Exception e) {
            log.error("获取用户画像异常", e);
            lastHasData.set(false);
            return "=== 客户画像 ===\n获取画像异常: " + e.getMessage();
        }
    }

    /**
     * 由 PersonaInjector/PlanningFlow 在 Agent 启动前预装画像 JsonNode 到 ThreadLocal，
     * 供 calculateLinkedPrice 在后续 Tool 调用中使用。
     */
    public void preloadProfile(JsonNode profileJson) {
        if (profileJson != null) {
            this.cachedProfile.set(profileJson);
        }
    }

    public String calculateLinkedPrice(
            String targetDistrict,
            Double targetArea,
            Integer targetRooms,
            String targetHouseType,
            Boolean requireSchool,
            Boolean requireSubway) {

        JsonNode profile = cachedProfile.get();
        if (profile == null) {
            return "错误: 请先调用 getCustomerProfile 获取画像数据后再调用联动计算。";
        }

        JsonNode hard = profile.get("hardConstraints");
        double upAnchor = numOrDefault(profile, "unitPriceAnchorWan", 0);
        if (upAnchor <= 0) {
            return "画像中无有效单价锚点数据，无法进行联动计算。建议直接使用用户口头提出的条件搜索。";
        }

        // 画像基准参数
        String profileDistrict = (hard != null && hard.has("districts") && hard.get("districts").isArray()
                && !hard.get("districts").isEmpty()) ? hard.get("districts").get(0).asText() : null;
        double profileMinArea = hard != null ? numOrDefault(hard, "minArea", 90) : 90;
        double profileMaxArea = hard != null ? numOrDefault(hard, "maxArea", 120) : 120;
        double profileAvgArea = (profileMinArea + profileMaxArea) / 2.0;
        String profileHouseType = profile.has("softPreferences") ?
                textOrDefault(profile.get("softPreferences"), "preferredHouseType", "resale") : "resale";
        int profileRooms = (hard != null && hard.has("layoutRooms") && hard.get("layoutRooms").isArray()
                && !hard.get("layoutRooms").isEmpty()) ? hard.get("layoutRooms").get(0).asInt() : 3;
        boolean profileHasSchool = profile.has("scenarioTags") &&
                profile.get("scenarioTags").toString().contains("学");
        boolean profileNearSubway = profile.has("scenarioTags") &&
                profile.get("scenarioTags").toString().contains("地铁");

        StringBuilder result = new StringBuilder("=== UP Engine 联动价格重算 ===\n");
        double linkedUP = upAnchor;

        // 因子 1: 地段平移
        double fDistrict = 1.0;
        if (targetDistrict != null && profileDistrict != null && !targetDistrict.equals(profileDistrict)) {
            JsonNode districtIndex = profile.get("districtPriceIndex");
            if (districtIndex != null && districtIndex.has(profileDistrict) && districtIndex.has(targetDistrict)) {
                double profileBase = districtIndex.get(profileDistrict).asDouble();
                double targetBase = districtIndex.get(targetDistrict).asDouble();
                if (profileBase > 0) {
                    fDistrict = targetBase / profileBase;
                    result.append(String.format("[地段联动] %s(%.2f) → %s(%.2f), 系数: %.3f\n",
                            profileDistrict, profileBase, targetDistrict, targetBase, fDistrict));
                }
            }
        }

        // 因子 2: 户型密度
        double fLayout = 1.0;
        double effectiveArea = targetArea != null ? targetArea : profileAvgArea;
        if (targetRooms != null && targetRooms > profileRooms && targetArea != null && targetArea < profileAvgArea) {
            fLayout = 1.0 + (targetRooms - profileRooms) * 0.05;
            result.append(String.format("[户型密度联动] %d室→%d室(面积缩小), 溢价系数: %.2f\n", profileRooms, targetRooms, fLayout));
        }

        // 因子 3: 房屋类型跃迁
        double fHouseType = 1.0;
        String effectiveType = targetHouseType != null ? targetHouseType : profileHouseType;
        if (targetHouseType != null && !targetHouseType.equals(profileHouseType)) {
            double mProfile = HOUSE_TYPE_FACTORS.getOrDefault(profileHouseType, 1.0);
            double mTarget = HOUSE_TYPE_FACTORS.getOrDefault(targetHouseType, 1.0);
            if (mProfile > 0) {
                fHouseType = mTarget / mProfile;
                result.append(String.format("[屋型联动] %s(%.1f) → %s(%.1f), 系数: %.3f\n",
                        profileHouseType, mProfile, targetHouseType, mTarget, fHouseType));
            }
        }

        // 因子 4: 学区溢价
        double sSchool = 0.0;
        if (requireSchool != null) {
            if (requireSchool && !profileHasSchool) {
                sSchool = 0.30;
                result.append("[学区联动] 新增学区需求, 溢价加成: +30%\n");
            } else if (!requireSchool && profileHasSchool) {
                sSchool = -0.20;
                result.append("[学区联动] 剔除学区溢价, 下折: -20%\n");
            }
        }

        // 因子 5: 地铁溢价
        double sSubway = 0.0;
        if (requireSubway != null && requireSubway && !profileNearSubway) {
            sSubway = 0.08;
            result.append("[地铁联动] 新增地铁需求, 溢价加成: +8%\n");
        }

        // 综合计算最终联动单价
        linkedUP = upAnchor * fDistrict * fLayout * fHouseType * (1.0 + sSchool + sSubway);
        result.append(String.format("\n单价锚点: %.2f → 联动后单价: %.2f 万元/㎡\n", upAnchor, linkedUP));

        // 因子 6: 面积线性折算
        double linkagePrice = effectiveArea * linkedUP;
        double minPrice = Math.round(linkagePrice * 0.90);
        double maxPrice = Math.round(linkagePrice * 1.10);

        result.append(String.format("目标面积: %.0f ㎡\n", effectiveArea));
        result.append(String.format("联动总价中位数: %.0f 万元\n", linkagePrice));
        result.append(String.format("推荐检索预算区间: %.0f ~ %.0f 万元\n", minPrice, maxPrice));
        result.append(String.format("\n建议搜索参数: 区域=%s, 面积=%.0f㎡, 总价=%.0f~%.0f万, 类型=%s",
                targetDistrict != null ? targetDistrict : profileDistrict,
                effectiveArea, minPrice, maxPrice, effectiveType));

        return result.toString();
    }

    // ========== 工具方法 ==========

    private String textOrDefault(JsonNode node, String field, String def) {
        return (node != null && node.has(field) && !node.get(field).isNull()) ? node.get(field).asText() : def;
    }

    private double numOrDefault(JsonNode node, String field, double def) {
        return (node != null && node.has(field) && !node.get(field).isNull()) ? node.get(field).asDouble() : def;
    }

    private String arrayToString(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) return "未知";
        List<String> items = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            items.add(item.asText());
        }
        return String.join("、", items);
    }

    public boolean getLastHasData() {
        Boolean v = lastHasData.get();
        return v != null && v;
    }

    public void clearLast() {
        lastHasData.remove();
        cachedProfile.remove();
    }

    /**
     * 强制清空当前线程的所有画像缓存。供 PlanningFlow finally 块调用，
     * 防止 executor 线程池复用时 ThreadLocal 数据污染。
     */
    public void clearAll() {
        lastHasData.remove();
        cachedProfile.remove();
    }

    /**
     * 将画像中的房屋类型字符串映射为搜索接口的 Integer 值。
     * "new_house" → 1, "resale" → 2, "rental" → 3, 其他 → null
     */
    @Nullable
    public static Integer houseTypeToInt(@Nullable String type) {
        if (type == null) return null;
        return switch (type) {
            case "new_house" -> 1;
            case "resale" -> 2;
            case "rental" -> 3;
            default -> null;
        };
    }
}
