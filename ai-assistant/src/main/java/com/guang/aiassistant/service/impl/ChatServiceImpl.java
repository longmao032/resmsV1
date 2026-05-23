package com.guang.aiassistant.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.guang.aiassistant.config.PromptConfig;
import com.guang.aiassistant.core.PersonaInjector;
import com.guang.aiassistant.core.UserContext;
import com.guang.aiassistant.core.router.IntentType;
import com.guang.aiassistant.model.*;
import com.guang.aiassistant.repository.AssistantChatMemoryRepository;
import com.guang.aiassistant.service.AiIntentExtractor;
import com.guang.aiassistant.service.ChatService;
import com.guang.aiassistant.service.ExplanationService;
import com.guang.aiassistant.service.SearchPipeline;
import com.guang.aiassistant.service.SearchStateService;
import com.guang.aiassistant.tool.CitySearchTool;
import com.guang.aiassistant.tool.CustomerProfileTool;
import com.guang.aiassistant.tool.HouseSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final AiIntentExtractor intentExtractor;
    private final ChatClient fastChatClient;
    private final SearchStateService searchStateService;
    private final AssistantChatMemoryRepository assistantChatMemoryRepository;
    private final HouseSearchTool houseSearchTool;
    private final CitySearchTool citySearchTool;
    private final PersonaInjector personaInjector;
    private final CustomerProfileTool customerProfileTool;
    private final PromptConfig promptConfig;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;
    private final SearchPipeline searchPipeline;
    private final ExplanationService explanationService;

    public ChatServiceImpl(
            AiIntentExtractor intentExtractor,
            @Qualifier("fastChatClient") ChatClient fastChatClient,
            SearchStateService searchStateService,
            AssistantChatMemoryRepository assistantChatMemoryRepository,
            HouseSearchTool houseSearchTool,
            CitySearchTool citySearchTool,
            PersonaInjector personaInjector,
            CustomerProfileTool customerProfileTool,
            PromptConfig promptConfig,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            VectorStore vectorStore,
            SearchPipeline searchPipeline,
            ExplanationService explanationService) {
        this.intentExtractor = intentExtractor;
        this.fastChatClient = fastChatClient;
        this.searchStateService = searchStateService;
        this.assistantChatMemoryRepository = assistantChatMemoryRepository;
        this.houseSearchTool = houseSearchTool;
        this.citySearchTool = citySearchTool;
        this.personaInjector = personaInjector;
        this.customerProfileTool = customerProfileTool;
        this.promptConfig = promptConfig;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
        this.searchPipeline = searchPipeline;
        this.explanationService = explanationService;
    }

    @Override
    public ChatResult chat(String message, String sessionId, String city, Boolean isPersonaEnabled) {
        UserIntentForm intentForm = intentExtractor.extract(message);
        IntentType intent = intentForm.intentType();
        log.info("Intent: {} city:{} district:{} project:{} houseType:{} price:{}-{} for sessionId: {}",
                 intent, intentForm.mentionedCity(), intentForm.mentionedDistrict(),
                 intentForm.mentionedProject(), intentForm.houseType(),
                 intentForm.minPrice(), intentForm.maxPrice(), sessionId);

        return switch (intent) {
            case CHITCHAT -> new ChatResult(sessionId,
                                           RecommendationResponse.of(handleChitchat(message, sessionId), List.of()));
            case POLICY -> new ChatResult(sessionId,
                                         RecommendationResponse.of(handlePolicy(message, sessionId), List.of()));
            case SEARCH -> handleSearch(message, sessionId, city, isPersonaEnabled, intentForm);
            case RESET -> new ChatResult(sessionId,
                                        RecommendationResponse.of(handleReset(sessionId), List.of()));
        };
    }

    // ==================== Chitchat / Policy / Reset ====================

    private String handleChitchat(String message, String sessionId) {
        try {
            String system = promptConfig.getChitchatPrompt();
            String reply = fastChatClient.prompt()
                    .system(system)
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content();
            if (reply == null || reply.isBlank()) {
                return "您好！我是您的专属房产顾问，有什么可以帮您的吗？";
            }
            return reply;
        } catch (Exception e) {
            log.error("Chitchat handler failed", e);
            return "您好！我是您的专属房产顾问，有什么可以帮您的吗？";
        }
    }

    private String handlePolicy(String message, String sessionId) {
        try {
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(message)
                            .topK(3)
                            .similarityThreshold(0.5D)
                            .build()
            );

            if (documents == null || documents.isEmpty()) {
                return "抱歉，暂未查到相关政策，建议咨询专业顾问。";
            }

            StringBuilder sb = new StringBuilder("根据政策知识库为您检索到以下相关条款：\n\n");
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                sb.append(String.format("【条款 %d】\n", i + 1));
                sb.append(doc.getText().trim()).append("\n");

                String source = (String) doc.getMetadata().get("title");
                if (source == null) {
                    source = (String) doc.getMetadata().get("source");
                }
                if (source != null && !source.isBlank()) {
                    sb.append("—— 来源：《").append(source).append("》\n");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("Policy handler failed", e);
            return "抱歉，政策咨询服务暂时不可用，请稍后再试。";
        }
    }

    private String handleReset(String sessionId) {
        searchStateService.clearSession(sessionId);
        try {
            assistantChatMemoryRepository.deleteByConversationId(sessionId);
        } catch (Exception e) {
            log.warn("清空 ChatMemory 失败: {}", e.getMessage());
        }
        log.info("会话 {} 已重置", sessionId);
        return "已清空当前会话的搜索记忆和对话历史，您可以重新开始。";
    }

    // ==================== SEARCH — Java 强管控 ====================

    private ChatResult handleSearch(String message, String sessionId, String city, Boolean isPersonaEnabled, UserIntentForm form) {
        try {
            // --- 1. 装载多轮持久上下文 ---
            HouseSearchContext ctx = loadContext(sessionId);
            boolean contextLoaded = ctx != null;
            if (!contextLoaded) {
                ctx = new HouseSearchContext();
            }

            // --- 2. 城市继承与覆盖 ---
            String historyCity = searchStateService.getLastCity(sessionId);
            String resolvedCity = resolveCity(form, sessionId, city, ctx);

            // --- 3. 模糊意图检测：用户表达了购房意向但条件不足 → LLM 引导话术 ---
            boolean vague = isVagueSearch(form, resolvedCity, ctx);
            log.info("模糊判定结果: vague={}", vague);
            if (vague) {
                // 保存当前轮次已知信息到 context，确保下一轮能继承
                if (resolvedCity != null && !resolvedCity.isBlank()) {
                    ctx.setCity(resolvedCity);
                }
                if (form.mentionedDistrict() != null && !form.mentionedDistrict().isBlank()) {
                    ctx.setDistrict(form.mentionedDistrict());
                }
                if (form.mentionedSubway() != null && !form.mentionedSubway().isBlank()) {
                    ctx.setSubwayLine(form.mentionedSubway());
                }
                if (form.houseType() != null) {
                    ctx.setHouseType(form.houseType());
                }
                saveContext(sessionId, ctx);
                String guideReply = generateGuideResponse(message, sessionId, form);
                return new ChatResult(sessionId, RecommendationResponse.of(guideReply, List.of()));
            }

            // --- 4. 画像注入：用户没说的条件用画像默认值 ---
            JsonNode personaData = null;
            boolean personaOk = false;
            String userId = UserContext.getCurrentUserId();
            boolean personaEnabled = isPersonaEnabled == null || isPersonaEnabled;
            if (personaEnabled && userId != null && userId.matches("\\d+") && resolvedCity != null) {
                // 标准化城市名称，对齐画像存储格式（"深圳" → "深圳市"），避免跨城误判
                String normalizedCity = resolvedCity.endsWith("市") ? resolvedCity : resolvedCity + "市";
                try {
                    personaData = personaInjector.preloadAndInject(userId, sessionId, normalizedCity);
                    personaOk = personaData != null;
                } catch (Exception e) {
                    log.warn("画像注入异常，降级无画像运行: {}", e.getMessage());
                }
            }

            // 提取画像默认值
            String personaDistrict = null;
            Integer personaMinPrice = null, personaMaxPrice = null;
            Integer personaMinArea = null, personaMaxArea = null;
            Integer personaHouseType = null;
            String personaLayout = null;
            if (personaOk) {
                JsonNode hard = personaData.get("hardConstraints");
                if (hard != null) {
                    JsonNode districts = hard.get("districts");
                    if (districts != null && districts.size() > 0) personaDistrict = districts.get(0).asText();
                    if (hard.has("minPriceWan") && !hard.get("minPriceWan").isNull())
                            personaMinPrice = hard.get("minPriceWan").asInt();
                    if (hard.has("maxPriceWan") && !hard.get("maxPriceWan").isNull())
                            personaMaxPrice = hard.get("maxPriceWan").asInt();
                    if (hard.has("minArea") && !hard.get("minArea").isNull())
                            personaMinArea = hard.get("minArea").asInt() * 100; // ㎡ → 对齐 metadata 的 ×100 存储格式
                    if (hard.has("maxArea") && !hard.get("maxArea").isNull())
                            personaMaxArea = hard.get("maxArea").asInt() * 100;
                    JsonNode rooms = hard.get("layoutRooms");
                    if (rooms != null && rooms.size() > 0) personaLayout = rooms.get(0).asInt() + "室";
                }
                JsonNode soft = personaData.get("softPreferences");
                if (soft != null && soft.has("preferredHouseType") && !soft.get("preferredHouseType").isNull())
                        personaHouseType = CustomerProfileTool.houseTypeToInt(soft.get("preferredHouseType").asText());
            }

            // 合并：用户本次输入 > 会话历史(ctx) > 画像默认值
            String finalDistrict = form.mentionedDistrict() != null ? form.mentionedDistrict()
                    : ctx.getDistrict() != null ? ctx.getDistrict()
                    : personaDistrict;
            // 价格：用户说了任意一端，就只用用户数据，不用画像补充（避免两端不一致导致区间反转）
            boolean userHasPrice = form.minPrice() != null || form.maxPrice() != null;
            Integer finalMinPrice = userHasPrice ? form.minPrice() : personaMinPrice;
            Integer finalMaxPrice = userHasPrice ? form.maxPrice() : personaMaxPrice;
            Integer finalHouseType = form.houseType() != null ? form.houseType()
                    : ctx.getHouseType() != null ? ctx.getHouseType()
                    : personaHouseType;
            Integer finalMinArea = personaMinArea;
            Integer finalMaxArea = personaMaxArea;
            String finalLayout = personaLayout;

            // 日志：生产可观测性汇总行
            if (personaOk) {
                StringBuilder mergeLog = new StringBuilder("Persona merged: OK");
                mergeLog.append(", district=").append(personaDistrict);
                if (form.mentionedDistrict() != null) mergeLog.append("(user→override)");
                mergeLog.append(", price=").append(personaMinPrice).append("-").append(personaMaxPrice);
                if (form.minPrice() != null) mergeLog.append("(user minPrice)");
                if (form.maxPrice() != null) mergeLog.append("(user maxPrice)");
                mergeLog.append(", houseType=").append(personaHouseType);
                if (form.houseType() != null) mergeLog.append("(user→override)");
                if (personaLayout != null) mergeLog.append(", layout=").append(personaLayout);
                if (personaMinArea != null) mergeLog.append(", area=").append(personaMinArea / 100).append("-").append(personaMaxArea / 100).append("㎡");
                log.info(mergeLog.toString());
            } else {
                log.info("Persona merged: null");
            }

            // 城市变更检测 → 清洗跨城污染
            boolean isCityChanged = false;
            if (form.mentionedCity() != null && !form.mentionedCity().isBlank()
                    && historyCity != null && !historyCity.isBlank()
                    && !historyCity.equals(form.mentionedCity())) {
                isCityChanged = true;
                ctx.changeCity(form.mentionedCity());
            }
            ctx.setCity(resolvedCity);

            // --- 3. 区县合法性校验 ---
            String resolvedDistrict = form.mentionedDistrict();
            if (resolvedDistrict != null && !resolvedDistrict.isBlank()) {
                String validation = citySearchTool.validateCityDistrict(resolvedCity, resolvedDistrict);
                log.info("区县校验结果: {}", validation);
                if (validation.contains("未匹配") || validation.contains("有歧义") || validation.contains("未找到匹配的城市")) {
                    return new ChatResult(sessionId,
                                          RecommendationResponse.of(
                                                  "抱歉，「" + resolvedDistrict + "」在「" + resolvedCity + "」未找到匹配的区县，请确认区县名称是否正确。",
                                                  List.of()));
                }
                ctx.setDistrict(resolvedDistrict);
            }

            // --- 4. 楼盘更新（聚焦楼盘时清洗地铁） ---
            if (form.mentionedProject() != null && !form.mentionedProject().isBlank()) {
                ctx.updateProject(form.mentionedProject());
            }

            // --- 5. 地铁偏好更新 ---
            if (form.mentionedSubway() != null && !form.mentionedSubway().isBlank()) {
                ctx.setSubwayLine(form.mentionedSubway());
            }

            // --- 6. houseType 覆盖 ---
            if (form.houseType() != null) {
                ctx.setHouseType(form.houseType());
            }

            // --- 7. 统一组装语义查询（地铁偏好注入） ---
            String effectiveSemanticQuery = form.generalQuery();
            if (ctx.getSubwayLine() != null && !ctx.getSubwayLine().isBlank()) {
                String subwayHint = ctx.getSubwayLine().equals("地铁") ? "地铁沿线" : ctx.getSubwayLine();
                effectiveSemanticQuery = effectiveSemanticQuery == null || effectiveSemanticQuery.isBlank()
                        ? subwayHint : effectiveSemanticQuery + " " + subwayHint;
            }

            // --- 8. 提取用户偏好标签（从 generalQuery 中匹配已知标签） ---
            List<String> userTagPreferences = extractTagPreferences(effectiveSemanticQuery);

            // --- 9. 执行搜索管道（召回 → 二次排序 → 缓存 → 降级） ---
            log.info("搜索参数: city={}, district={}, houseType={}, layout={}, area={}-{}, price={}-{}, query={}, subway={}",
                    resolvedCity, finalDistrict, finalHouseType, finalLayout,
                    finalMinArea != null ? finalMinArea / 100 : null, finalMaxArea != null ? finalMaxArea / 100 : null,
                    finalMinPrice, finalMaxPrice, effectiveSemanticQuery, ctx.getSubwayLine());
            SearchResult searchResult = searchPipeline.execute(
                    resolvedCity, finalDistrict, finalHouseType,
                    finalLayout, finalMinArea, finalMaxArea,
                    finalMinPrice, finalMaxPrice,
                    effectiveSemanticQuery, personaData,
                    userTagPreferences, form.mentionedDistrict());

            // --- 10. 记录搜索状态 ---
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("city", resolvedCity);
            if (finalDistrict != null) params.put("district", finalDistrict);
            if (ctx.getProject() != null) params.put("project", ctx.getProject());
            if (finalHouseType != null) params.put("houseType", finalHouseType);
            if (finalMinPrice != null) params.put("minPrice", finalMinPrice);
            if (finalMaxPrice != null) params.put("maxPrice", finalMaxPrice);
            int count = searchResult.rankedHouses().size();
            searchStateService.recordSearch(sessionId, "queryHouses", params, count);

            // --- 11. 提取结构化数据（从排序后的结果中取 top 5） ---
            List<HouseItem> houseItems = extractHouseItemsFromRanked(searchResult.rankedHouses());
            List<ProjectItem> projectItems = extractProjectItemsFromRanked(searchResult.rankedHouses());

            // --- 12. LLM 推荐理由生成（非阻塞，失败时跳过） ---
            if (!houseItems.isEmpty()) {
                String preferencesSummary = buildPreferencesSummary(form, personaData);
                Map<String, String> explanations = explanationService.generateExplanations(
                        message, preferencesSummary, houseItems, sessionId);
                houseItems = houseItems.stream()
                        .map(h -> new HouseItem(h.houseId(), h.projectName(), h.district(), h.price(),
                                h.layout(), h.area(), h.coverUrl(), h.sellingPoint(),
                                explanations.getOrDefault(h.houseId(), null)))
                        .toList();
            }

            // --- 13. 持久化上下文 ---
            if (contextLoaded) {
                saveContext(sessionId, ctx);
            } else {
                log.warn("跳过上下文回写：Redis 不可用时的空上下文不应覆写, sessionId={}", sessionId);
            }

            // --- 14. LLM 话术合成 ---
            String warmDistrict = form.mentionedDistrict() != null ? finalDistrict : null;
            String degradationCtx = searchResult.degradationReason() != null ? searchResult.degradationReason() : "";
            String reply = renderWarmResponse(resolvedCity, warmDistrict, count, isCityChanged,
                    degradationCtx, message, sessionId);

            if (!projectItems.isEmpty()) {
                return new ChatResult(sessionId,
                                      RecommendationResponse.withProjects(reply, houseItems, projectItems));
            }
            return new ChatResult(sessionId,
                                 RecommendationResponse.of(reply, houseItems));

        } finally {
            houseSearchTool.clearLastQuery();
            houseSearchTool.clearLastSearchDocs();
            customerProfileTool.clearAll();
        }
    }

    /**
     * 城市解析：本轮优先 → 上下文继承 → 历史继承 → 请求参数兜底
     */
    private String resolveCity(UserIntentForm form, String sessionId, String requestCity, HouseSearchContext ctx) {
        if (form.mentionedCity() != null && !form.mentionedCity().isBlank()) {
            log.info("城市覆盖：本轮提到「{}」", form.mentionedCity());
            return form.mentionedCity();
        }
        if (ctx != null && ctx.getCity() != null && !ctx.getCity().isBlank()) {
            log.info("城市继承：从上下文继承「{}」", ctx.getCity());
            return ctx.getCity();
        }
        String historyCity = searchStateService.getLastCity(sessionId);
        if (historyCity != null && !historyCity.isBlank()) {
            log.info("城市继承：从历史记录继承「{}」", historyCity);
            return historyCity;
        }
        if (requestCity != null && !requestCity.isBlank()) {
            log.info("城市兜底：使用请求参数「{}」", requestCity);
            return requestCity;
        }
        return null;
    }

    // ==================== 模糊意图引导 ====================

    /**
     * 判断本轮搜索意图是否过于模糊（无城市，或城市+无任何条件+无历史上下文）。
     * 模糊时应由 LLM 生成引导话术，而非强行执行搜索。
     */
    private boolean isVagueSearch(UserIntentForm form, String resolvedCity, HouseSearchContext ctx) {
        log.info("条件汇总: city={}, district={}, project={}, houseType={}, price={}-{}, subway={}, generalQuery={}",
                resolvedCity, form.mentionedDistrict(), form.mentionedProject(), form.houseType(),
                form.minPrice(), form.maxPrice(), form.mentionedSubway(), form.generalQuery());
        log.info("上下文状态: city={}, district={}, project={}, houseType={}, subwayLine={}, isEmpty={}",
                ctx != null ? ctx.getCity() : "null",
                ctx != null ? ctx.getDistrict() : "null",
                ctx != null ? ctx.getProject() : "null",
                ctx != null ? ctx.getHouseType() : "null",
                ctx != null ? ctx.getSubwayLine() : "null",
                ctx == null || ctx.isEmpty());
        if (resolvedCity == null || resolvedCity.isBlank()) return true;
        // 结构化字段 + 带城市的一般语义查询算是有效条件。
        // generalQuery 单独（无城市、无历史上下文）不算——如"王子"、"推荐"等孤立词走引导话术。
        boolean hasCriteria = form.mentionedDistrict() != null
                || form.mentionedProject() != null
                || form.houseType() != null
                || form.minPrice() != null
                || form.maxPrice() != null
                || form.mentionedSubway() != null
                || (form.generalQuery() != null && form.mentionedCity() != null);
        if (hasCriteria) return false;
        // 本轮无结构化字段 → 检查历史上下文是否有足够的搜索条件
        if (ctx == null || ctx.isEmpty()) return true;
        // ctx 中有区县或楼盘 → 条件足够，可以搜索
        boolean ctxHasCriteria = ctx.getDistrict() != null
                || ctx.getProject() != null
                || ctx.getHouseType() != null;
        return !ctxHasCriteria;
    }

    /**
     * 模糊场景下，使用 LLM 生成自然引导话术，引导用户给出具体需求。
     * 将已提取的区县/楼盘等信息注入系统提示词，让 LLM 能精准追问。
     * 对话历史由 MessageChatMemoryAdvisor 自动注入（fastChatClient 默认启用），
     * 提示词已指引 LLM 阅读历史记录来回溯之前提到过的城市/小区等信息。
     */
    private String generateGuideResponse(String userMessage, String sessionId, UserIntentForm form) {
        try {
            String system = promptConfig.getSystemGuidePrompt();
            StringBuilder enriched = new StringBuilder(system);

            // 注入已提取的结构化信息（辅助 LLM 精准理解本轮意图）
            if (form.mentionedDistrict() != null) {
                enriched.append("\n客户本轮提到了区县：").append(form.mentionedDistrict());
                enriched.append("（未指明城市）。可在对话历史中查找对应城市。\n");
            }
            if (form.mentionedProject() != null) {
                enriched.append("\n客户本轮提到了楼盘：").append(form.mentionedProject());
                enriched.append("（未指明城市）。可在对话历史中查找对应城市。\n");
            }
            if (form.houseType() != null) {
                String type = switch (form.houseType()) {
                    case 1 -> "新房";
                    case 2 -> "二手房";
                    case 3 -> "租房";
                    default -> "";
                };
                enriched.append("\n客户意向类型：").append(type).append("\n");
            }

            String reply = fastChatClient.prompt()
                    .system(enriched.toString())
                    .user(userMessage)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content();
            if (reply != null && !reply.isBlank()) return reply;
        } catch (Exception e) {
            log.warn("引导话术合成失败，使用兜底文本: {}", e.getMessage());
        }
        return "您好！我是您的房产顾问，请问您想看哪个城市的房子呢？";
    }

    // ==================== 持久上下文读写 ====================

    /**
     * 从 Redis 装载多轮上下文。返回 null 表示 Redis 不可用或无数据，调用方应使用空上下文且不应回写。
     */
    private HouseSearchContext loadContext(String sessionId) {
        String key = "estate:session:" + sessionId;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                return objectMapper.readValue(json, HouseSearchContext.class);
            }
            // Redis 可用但无数据 → 返回空上下文（允许回写）
            return new HouseSearchContext();
        } catch (Exception e) {
            log.error("从 Redis 读取 HouseSearchContext 异常，sessionId: {}", sessionId, e);
            return null;
        }
    }

    private void saveContext(String sessionId, HouseSearchContext ctx) {
        if (ctx == null) return;
        String key = "estate:session:" + sessionId;
        try {
            String json = objectMapper.writeValueAsString(ctx);
            redisTemplate.opsForValue().set(key, json, 30, TimeUnit.MINUTES);
            log.debug("成功持久化上下文至 Redis, sessionId={}: {}", sessionId, json);
        } catch (Exception e) {
            log.error("持久化 HouseSearchContext 至 Redis 异常，sessionId: {}", sessionId, e);
        }
    }

    // ==================== 结构化数据提取 ====================

    /** 从排序后的结果中提取 top 5 房源 */
    private List<HouseItem> extractHouseItemsFromRanked(List<RankedHouse> ranked) {
        return ranked.stream()
                .map(RankedHouse::document)
                .filter(d -> !"project".equals(stringMeta(d, "type")))
                .map(d -> {
                    String houseId = stringMeta(d, "houseId");
                    String projectName = stringMeta(d, "projectName");
                    String district = stringMeta(d, "district");
                    String price = stringMeta(d, "priceText");
                    String layout = coalesce(stringMeta(d, "roomType"), stringMeta(d, "layout"));
                    String area = stringMeta(d, "areaText");
                    String coverUrl = stringMeta(d, "coverImage");
                    String tags = stringMeta(d, "tags");
                    if (projectName == null) return null;
                    return new HouseItem(houseId, projectName, district, price, layout, area, coverUrl, tags);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /** 从排序后的结果中提取楼盘 */
    private List<ProjectItem> extractProjectItemsFromRanked(List<RankedHouse> ranked) {
        return ranked.stream()
                .map(RankedHouse::document)
                .filter(d -> "project".equals(stringMeta(d, "type")))
                .map(d -> {
                    String projectId = stringMeta(d, "projectId");
                    String projectName = stringMeta(d, "projectName");
                    String district = stringMeta(d, "district");
                    String address = stringMeta(d, "address");
                    String developer = stringMeta(d, "developer");
                    String houseCount = stringMeta(d, "houseCount");
                    String layouts = stringMeta(d, "layouts");
                    String tags = stringMeta(d, "tags");
                    String coverUrl = stringMeta(d, "coverUrl");

                    String priceMin = stringMeta(d, "priceMin");
                    String priceMax = stringMeta(d, "priceMax");
                    String priceRange = null;
                    if (priceMin != null && priceMax != null && !priceMin.equals(priceMax))
                        priceRange = priceMin + "万-" + priceMax + "万";
                    else if (priceMin != null) priceRange = priceMin + "万";

                    String areaMin = stringMeta(d, "areaMin");
                    String areaMax = stringMeta(d, "areaMax");
                    String areaRange = null;
                    if (areaMin != null && areaMax != null && !areaMin.equals(areaMax))
                        areaRange = areaMin + "-" + areaMax + "㎡";
                    else if (areaMin != null) areaRange = areaMin + "㎡";

                    if (projectName == null) return null;
                    return new ProjectItem(projectId, projectName, district, address, developer,
                                           houseCount, priceRange, areaRange, layouts, tags, coverUrl, tags);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /** 从 generalQuery 中提取匹配已知标签的偏好词 */
    private List<String> extractTagPreferences(String query) {
        if (query == null || query.isBlank()) return List.of();
        return HouseSearchTool.KNOWN_TAGS.stream().filter(query::contains).toList();
    }

    /** 构建偏好摘要（供 LLM 生成推荐理由） */
    private String buildPreferencesSummary(UserIntentForm form, JsonNode personaData) {
        StringBuilder sb = new StringBuilder();
        if (form.mentionedDistrict() != null) sb.append("区域:").append(form.mentionedDistrict()).append("; ");
        if (form.minPrice() != null || form.maxPrice() != null)
            sb.append("预算:").append(form.minPrice()).append("-").append(form.maxPrice()).append("万; ");
        if (form.houseType() != null) sb.append("房型:").append(form.houseType()).append("; ");
        if (form.generalQuery() != null) sb.append("偏好:").append(form.generalQuery()).append("; ");
        if (personaData != null) {
            JsonNode hard = personaData.get("hardConstraints");
            if (hard != null && hard.has("layoutRooms")) sb.append("画像户型:").append(hard.get("layoutRooms")).append("; ");
        }
        return sb.length() > 0 ? sb.toString() : "无特殊偏好";
    }

    /**
     * LLM 话术合成 — 只投喂状态信标，严禁传入具体房源数据。
     * 失败时触发硬编码兜底文本，确保高可用。
     */
    private String renderWarmResponse(String city, String district, int count,
                                       boolean isCityChanged, String degradation,
                                       String userMessage, String sessionId) {
        String location = district != null ? city + district : city;

        // count=0 时不调 LLM，避免模型忽略约束生成"选择不少"等幻觉回复
        if (count <= 0) {
            return "在「" + location + "」暂未找到完全匹配的房源。建议您：试试放宽价格范围、换一个区域、或告诉我您的其他偏好，我帮您重新筛选。";
        }

        try {
            String template = promptConfig.getWarmResponsePrompt();
            String system = template
                    .replace("{city}", city != null ? city : "未知")
                    .replace("{district}", district != null ? district : "不限")
                    .replace("{count}", String.valueOf(count))
                    .replace("{isCityChanged}", String.valueOf(isCityChanged))
                    .replace("{degradation}", degradation != null ? degradation : "");

            String reply = fastChatClient.prompt()
                    .system(system)
                    .user(userMessage)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content();

            if (reply != null && !reply.isBlank()) {
                return reply;
            }
        } catch (Exception e) {
            log.warn("话术合成失败，使用兜底文本: {}", e.getMessage());
        }

        if (count == 0) {
            return "在「" + location + "」暂未找到完全匹配的房源。建议您：试试放宽价格范围、换一个区域、或告诉我您的其他偏好，我帮您重新筛选。";
        }
        return "为您在「" + location + "」找到 " + count + " 条结果，请查看下方推荐。";
    }

    // ==================== 搜索降级 ====================

    // ==================== 工具方法 ====================

    private static String stringMeta(Document doc, String key) {
        Object val = doc.getMetadata().get(key);
        return val != null ? val.toString() : null;
    }

    private static String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}

