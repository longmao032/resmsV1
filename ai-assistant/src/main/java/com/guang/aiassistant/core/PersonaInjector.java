package com.guang.aiassistant.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guang.aiassistant.client.EstateSystemClient;
import com.guang.aiassistant.model.SearchState;
import com.guang.aiassistant.service.SearchStateService;
import com.guang.aiassistant.tool.CustomerProfileTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * 画像注入器 —— 在请求层静默拉取客户画像，缓存到 PG，
 * 并预装 ThreadLocal 供计算引擎使用。
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
     * 拉取画像写入缓存和 ThreadLocal。返回拆包后的 data 节点，
     * 调用方直接提取字段用于搜索参数补全，无需再解析或读 ThreadLocal。
     *
     * @param userId    当前用户 ID
     * @param sessionId 会话 ID
     * @param queryCity 用户本轮查询的目标城市（用于跨城判断），可为 null
     * @return 拆包后的 data 节点，或 null（降级无画像运行）
     */
    @Nullable
    public JsonNode preloadAndInject(String userId, String sessionId, @Nullable String queryCity) {
        SearchState state = stateService.getState(sessionId);

        // ---- 分支 A：已成功注入，复用 PG 缓存 ----
        if (state.profileFetched() && state.personaText() != null) {
            log.info("PersonaInjector 复用缓存: sessionId={}", sessionId);
            JsonNode cached = restoreFromCache(state);
            if (cached != null) return cached;
            // 缓存解析失败，降级为重新拉取
        }

        // ---- 分支 B：首次尝试 或 之前被跳过（跨城/无数据），重新拉取 ----

        // B1. 调远程 API
        JsonNode profileJson;
        long start = System.currentTimeMillis();
        try {
            profileJson = client.getUserProfile(userId);
        } catch (Exception e) {
            log.warn("PersonaInjector 画像拉取失败，降级无画像运行（保留旧缓存）: {}", e.getMessage());
            // 不写入缓存：保留已有的有效缓存，下次请求重试
            return null;
        }
        long elapsed = System.currentTimeMillis() - start;

        // B2. 空数据检查
        if (profileJson == null || isEffectivelyEmpty(profileJson)) {
            log.info("PersonaInjector 画像无有效数据: sessionId={}", sessionId);
            stateService.markProfileFetched(sessionId, false, null, null);
            return null;
        }

        // B3. 跨城检查 — 不注入，不覆写缓存（保留原有画像供回城时复用）
        String profileCity = extractCity(profileJson);
        if (queryCity != null && !queryCity.isBlank()
                && profileCity != null && !profileCity.equals(queryCity)) {
            log.info("PersonaInjector 跨城跳过: 画像城市={}, 查询城市={}", profileCity, queryCity);
            // 不写入缓存：避免摧毁原城市的画像缓存
            return null;
        }

        // B4. 拆包：取 data 节点，外部 API 返回 { "data": { ... } }
        JsonNode data = profileJson.has("data") ? profileJson.get("data") : profileJson;

        // B5. 预装 ThreadLocal（供 calculateLinkedPrice 使用）
        profileTool.preloadProfile(data);

        // B6. 检查是否有有效数据
        boolean hasData = hasValidData(data);

        // B7. 持久化 text + json（personaText 存 JSON 字符串保证缓存可用）
        stateService.markProfileFetched(sessionId, hasData, profileJson.toString(),
                profileJson.toString());

        log.info("PersonaInjector 注入成功: sessionId={}, city={}, hasData={}, cost={}ms",
                sessionId, profileCity, hasData, elapsed);
        return data;
    }

    // ---- 内部方法 ----

    /**
     * 从 PG 缓存还原 ThreadLocal 并返回 data 节点，供 calculateLinkedPrice 使用。
     */
    @Nullable
    private JsonNode restoreFromCache(SearchState state) {
        String cachedJson = state.personaJson();
        if (cachedJson != null && !cachedJson.isBlank()) {
            try {
                JsonNode root = MAPPER.readTree(cachedJson);
                JsonNode data = root.has("data") ? root.get("data") : root;
                profileTool.preloadProfile(data);
                return data;
            } catch (Exception e) {
                log.warn("PersonaInjector 还原画像 JSON 失败: {}", e.getMessage());
            }
        }
        return null;
    }

    private boolean hasValidData(JsonNode data) {
        double anchor = numOrDefault(data, "unitPriceAnchorWan", 0);
        JsonNode topHouses = data.get("topIntentHouses");
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

    private static double numOrDefault(JsonNode node, String field, double def) {
        return (node != null && node.has(field) && !node.get(field).isNull())
                ? node.get(field).asDouble() : def;
    }
}
