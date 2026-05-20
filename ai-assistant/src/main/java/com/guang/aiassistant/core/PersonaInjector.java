package com.guang.aiassistant.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guang.aiassistant.client.EstateSystemClient;
import com.guang.aiassistant.service.SearchStateService;
import com.guang.aiassistant.tool.CustomerProfileTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * 画像隐藏式注入器 —— 在 PlanningFlow 编排层静默拉取客户画像，
 * 注入到 Agent System Prompt 尾部，省去 LLM 显式调用 getCustomerProfile 的 ReAct 往返。
 *
 * <p>两级缓存：
 * <ul>
 *   <li>personaText != null → 已成功注入，复用 PG 缓存</li>
 *   <li>personaText == null → 未成功注入（首次/跨城跳过/无数据），重新拉取</li>
 * </ul>
 */
@Component
public class PersonaInjector {

    private static final Logger log = LoggerFactory.getLogger(PersonaInjector.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final EstateSystemClient client;
    private final CustomerProfileTool profileTool;
    private final SearchStateService stateService;

    public PersonaInjector(EstateSystemClient client,
                           CustomerProfileTool profileTool,
                           SearchStateService stateService) {
        this.client = client;
        this.profileTool = profileTool;
        this.stateService = stateService;
    }

    /**
     * 静默拉取画像，预装 ThreadLocal，持久化 personaText + personaJson。
     *
     * @param userId    当前用户 ID
     * @param sessionId 会话 ID
     * @param queryCity 用户本轮查询的目标城市（用于跨城判断），可为 null
     * @return 格式化画像文本（用于注入 System Prompt），或 null（降级无画像运行）
     */
    @Nullable
    public String preloadAndInject(String userId, String sessionId, @Nullable String queryCity) {
        SearchStateService.State state = stateService.getState(sessionId);

        // ---- 分支 A：已成功注入，复用 PG 缓存 ----
        if (state.profileFetched() && state.personaText() != null) {
            log.info("PersonaInjector 复用缓存: sessionId={}", sessionId);
            restoreThreadLocalFromCache(state);
            return state.personaText();
        }

        // ---- 分支 B：首次尝试 或 之前被跳过（跨城/无数据），重新拉取 ----

        // B1. 调远程 API
        JsonNode profileJson;
        try {
            profileJson = client.getUserProfile(userId);
        } catch (Exception e) {
            log.warn("PersonaInjector 画像拉取失败，降级无画像运行: {}", e.getMessage());
            stateService.markProfileFetched(sessionId, false, null, null);
            return null;
        }

        // B2. 空数据检查
        if (profileJson == null || isEffectivelyEmpty(profileJson)) {
            log.info("PersonaInjector 画像无有效数据: sessionId={}", sessionId);
            stateService.markProfileFetched(sessionId, false, null, null);
            return null;
        }

        // B3. 跨城检查 — 不注入，记录 personaText=null 标记为"跳过"
        String profileCity = extractCity(profileJson);
        if (queryCity != null && !queryCity.isBlank()
                && profileCity != null && !profileCity.equals(queryCity)) {
            log.info("PersonaInjector 跨城跳过: 画像城市={}, 查询城市={}", profileCity, queryCity);
            stateService.markProfileFetched(sessionId, false, null, null);
            return null;
        }

        // B4. 预装 ThreadLocal（供 calculateLinkedPrice 使用）
        profileTool.preloadProfile(profileJson);

        // B5. 格式化文本
        String personaText = formatPersona(profileJson);
        boolean hasData = hasValidData(profileJson);

        // B6. 持久化 text + json
        stateService.markProfileFetched(sessionId, hasData, personaText,
                profileJson.toString());

        log.info("PersonaInjector 注入成功: sessionId={}, city={}, hasData={}",
                sessionId, profileCity, hasData);
        return personaText;
    }

    // ---- 内部方法 ----

    /**
     * 从 PG 缓存还原 ThreadLocal，供多轮对话中 calculateLinkedPrice 使用。
     */
    private void restoreThreadLocalFromCache(SearchStateService.State state) {
        String cachedJson = state.personaJson();
        if (cachedJson != null && !cachedJson.isBlank()) {
            try {
                JsonNode node = MAPPER.readTree(cachedJson);
                profileTool.preloadProfile(node);
            } catch (Exception e) {
                log.warn("PersonaInjector 还原画像 JSON 失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 将画像 JSON 格式化为注入 System Prompt 的文本块。
     */
    private String formatPersona(JsonNode data) {
        JsonNode d = data.has("data") ? data.get("data") : data;

        StringBuilder sb = new StringBuilder("## 当前用户画像（系统自动获取）\n\n");

        // 紧迫度
        sb.append("- 购房紧迫度: ").append(textOrDefault(d, "urgencyLevel", "LOW")).append("\n");

        // 硬性约束
        JsonNode hard = d.get("hardConstraints");
        if (hard != null) {
            sb.append("- 意向城市: ").append(textOrDefault(hard, "city", "未知")).append("\n");
            sb.append("- 核心区域: ").append(arrayToStr(hard.get("districts"))).append("\n");
            sb.append("- 预算区间: ").append(numOrDefault(hard, "minPriceWan", 0))
                    .append(" ~ ").append(numOrDefault(hard, "maxPriceWan", 0)).append(" 万元\n");
            sb.append("- 面积区间: ").append(numOrDefault(hard, "minArea", 0))
                    .append(" ~ ").append(numOrDefault(hard, "maxArea", 0)).append(" ㎡\n");
            sb.append("- 偏好户型: ").append(arrayToStr(hard.get("layoutRooms"))).append(" 室\n");
        }

        // 软偏好
        JsonNode soft = d.get("softPreferences");
        if (soft != null) {
            sb.append("- 房屋类型: ").append(textOrDefault(soft, "preferredHouseType", "未知")).append("\n");
            sb.append("- 装修标准: ").append(textOrDefault(soft, "preferredDecoration", "未知")).append("\n");
        }

        // 场景标签
        sb.append("- 场景标签: ").append(arrayToStr(d.get("scenarioTags"))).append("\n");

        // 单价锚点
        double anchor = numOrDefault(d, "unitPriceAnchorWan", 0);
        sb.append("- 价格锚点: ").append(String.format("%.2f", anchor)).append(" 万元/㎡\n");

        // 区县均价指数
        JsonNode districtIndex = d.get("districtPriceIndex");
        if (districtIndex != null && districtIndex.isObject()) {
            sb.append("- 区县均价指数: ");
            var it = districtIndex.fields();
            boolean first = true;
            while (it.hasNext()) {
                var field = it.next();
                if (!first) sb.append(", ");
                sb.append(field.getKey()).append(" ")
                        .append(String.format("%.2f", field.getValue().asDouble()));
                first = false;
            }
            sb.append(" 万元/㎡\n");
        }

        // Top5 意向房源（精简）
        JsonNode topHouses = d.get("topIntentHouses");
        if (topHouses != null && topHouses.isArray() && !topHouses.isEmpty()) {
            sb.append("- 意向房源: ");
            int count = Math.min(topHouses.size(), 3);
            for (int i = 0; i < count; i++) {
                JsonNode h = topHouses.get(i);
                if (i > 0) sb.append("；");
                sb.append(textOrDefault(h, "title", ""))
                        .append("(").append(textOrDefault(h, "district", ""))
                        .append(", ").append(numOrDefault(h, "priceWan", 0)).append("万)");
            }
            sb.append("\n");
        }

        sb.append("\n> 以上画像数据已自动获取，你无需也无法调用 getCustomerProfile。\n");
        sb.append(">\n");
        sb.append("> **calculateLinkedPrice 调用规则：**\n");
        sb.append("> - 如果用户明确提出的预算/面积/区域等条件与以上画像**冲突**"
                + "（如预算超出画像范围、区域不在画像核心区域），优先遵循用户意愿，"
                + "并应调用 calculateLinkedPrice 重新计算联动价格。\n");
        sb.append("> - 如果用户的条件与画像**不冲突**（如仅问询，未提出偏离画像的新约束），"
                + "直接用画像回答即可，不需要调用 calculateLinkedPrice。\n");

        return sb.toString();
    }

    private boolean hasValidData(JsonNode data) {
        JsonNode d = data.has("data") ? data.get("data") : data;
        double anchor = numOrDefault(d, "unitPriceAnchorWan", 0);
        JsonNode topHouses = d.get("topIntentHouses");
        boolean hasHouses = topHouses != null && topHouses.isArray() && !topHouses.isEmpty();
        return anchor > 0 || hasHouses;
    }

    private boolean isEffectivelyEmpty(JsonNode profileJson) {
        JsonNode d = profileJson.has("data") ? profileJson.get("data") : profileJson;
        if (d == null || d.isNull()) return true;
        // data 对象存在但没有任何有意义字段
        return d.get("hardConstraints") == null
                && d.get("softPreferences") == null
                && d.get("scenarioTags") == null
                && d.get("unitPriceAnchorWan") == null;
    }

    private String extractCity(JsonNode profileJson) {
        JsonNode d = profileJson.has("data") ? profileJson.get("data") : profileJson;
        JsonNode hard = d.get("hardConstraints");
        if (hard != null && hard.has("city") && !hard.get("city").isNull()) {
            return hard.get("city").asText();
        }
        return null;
    }

    // ---- 工具方法 ----

    private static String textOrDefault(JsonNode node, String field, String def) {
        return (node != null && node.has(field) && !node.get(field).isNull())
                ? node.get(field).asText() : def;
    }

    private static double numOrDefault(JsonNode node, String field, double def) {
        return (node != null && node.has(field) && !node.get(field).isNull())
                ? node.get(field).asDouble() : def;
    }

    private static String arrayToStr(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) return "未知";
        StringBuilder sb = new StringBuilder();
        for (JsonNode item : arrayNode) {
            if (!sb.isEmpty()) sb.append("、");
            sb.append(item.asText());
        }
        return sb.toString();
    }
}
