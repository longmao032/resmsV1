package com.guang.aiassistant.core.flow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guang.aiassistant.core.agent.ToolCallAgent;
import com.guang.aiassistant.core.PersonaInjector;
import com.guang.aiassistant.core.UserContext;
import com.guang.aiassistant.service.SearchStateService;
import com.guang.aiassistant.tool.CitySearchTool;
import com.guang.aiassistant.tool.CustomerProfileTool;
import com.guang.aiassistant.tool.HouseSearchTool;
import com.guang.aiassistant.tool.TerminateTool;
import com.guang.aiassistant.model.HouseItem;
import com.guang.aiassistant.model.ProjectItem;
import org.springframework.ai.document.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@Slf4j
@Component
public class PlanningFlow {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ObjectProvider<ToolCallAgent> agentProvider;
    private final SearchStateService searchStateService;
    private final Resource planningPrompt;
    private final Resource stepSummaryPrompt;
    private final Resource systemAgentPrompt;
    private final Resource warmResponsePrompt;
    private final Resource stepToolInstructionPrompt;
    private final Resource stepTerminateInstructionPrompt;
    private final Resource stepInputToolPrompt;
    private final Resource stepInputTerminatePrompt;
    private final Resource chatHistoryContextPrompt;
    private final Resource decomposeSystemPrompt;
    private final TerminateTool terminateTool;
    private final HouseSearchTool houseSearchTool;
    private final CustomerProfileTool customerProfileTool;
    private final CitySearchTool citySearchTool;
    private final PersonaInjector personaInjector;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public PlanningFlow(@Qualifier("openAiChatModel") ChatModel chatModel,
                        ChatMemory chatMemory,
                        ObjectProvider<ToolCallAgent> agentProvider,
                        SearchStateService searchStateService,
                        @Value("classpath:/prompts/planning-task.st") Resource planningPrompt,
                        @Value("classpath:/prompts/step-summary.st") Resource stepSummaryPrompt,
                        @Value("classpath:/prompts/system-agent.st") Resource systemAgentPrompt,
                        @Value("classpath:/prompts/warm-response.st") Resource warmResponsePrompt,
                        @Value("classpath:/prompts/step-tool-instruction.st") Resource stepToolInstructionPrompt,
                        @Value("classpath:/prompts/step-terminate-instruction.st") Resource stepTerminateInstructionPrompt,
                        @Value("classpath:/prompts/step-input-tool.st") Resource stepInputToolPrompt,
                        @Value("classpath:/prompts/step-input-terminate.st") Resource stepInputTerminatePrompt,
                        @Value("classpath:/prompts/chat-history-context.st") Resource chatHistoryContextPrompt,
                        @Value("classpath:/prompts/decompose-system.st") Resource decomposeSystemPrompt,
                        TerminateTool terminateTool,
                        HouseSearchTool houseSearchTool,
                        CustomerProfileTool customerProfileTool,
                        CitySearchTool citySearchTool,
                        PersonaInjector personaInjector) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.chatMemory = chatMemory;
        this.agentProvider = agentProvider;
        this.searchStateService = searchStateService;
        this.planningPrompt = planningPrompt;
        this.stepSummaryPrompt = stepSummaryPrompt;
        this.systemAgentPrompt = systemAgentPrompt;
        this.warmResponsePrompt = warmResponsePrompt;
        this.stepToolInstructionPrompt = stepToolInstructionPrompt;
        this.stepTerminateInstructionPrompt = stepTerminateInstructionPrompt;
        this.stepInputToolPrompt = stepInputToolPrompt;
        this.stepInputTerminatePrompt = stepInputTerminatePrompt;
        this.chatHistoryContextPrompt = chatHistoryContextPrompt;
        this.decomposeSystemPrompt = decomposeSystemPrompt;
        this.terminateTool = terminateTool;
        this.houseSearchTool = houseSearchTool;
        this.customerProfileTool = customerProfileTool;
        this.citySearchTool = citySearchTool;
        this.personaInjector = personaInjector;
    }

    /**
     * PlanningFlow 执行结果：自然语言回复 + 结构化房源推荐列表。
     */
    public record PlanResult(String reply, List<HouseItem> recommendations, List<ProjectItem> projects) {
        public PlanResult(String reply, List<HouseItem> recommendations) {
            this(reply, recommendations, List.of());
        }
    }

    public PlanResult executePlan(String userMessage, String sessionId,
                                  String city, Boolean isPersonaEnabled) {
        String userId = UserContext.getCurrentUserId();
        String searchContext = searchStateService.getSearchContext(sessionId);
        CompletableFuture<PlanResult> future = CompletableFuture.supplyAsync(
                () -> {
                    UserContext.setCurrentUserId(userId);
                    try {
                        return doExecutePlan(userMessage, sessionId, city, isPersonaEnabled, searchContext);
                    } finally {
                        UserContext.clear();
                    }
                },
                executor);

        try {
            return future.get(120, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("执行超时: sessionId={}", sessionId);
            future.cancel(true);
            return new PlanResult("处理超时，请稍后重试或缩小查询范围。", List.of());
        } catch (ExecutionException e) {
            log.error("执行异常: sessionId={}", sessionId, e.getCause());
            return new PlanResult("服务暂时不可用", List.of());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return new PlanResult("服务暂时不可用", List.of());
        }
    }

    private PlanResult doExecutePlan(String userMessage, String sessionId,
                                     String city, Boolean isPersonaEnabled, String searchContext) {
        log.info("=== PlanningFlow 开始 ===");

        // Keyword-based reset guard
        if (userMessage.matches(".*(重置|清空记忆|忘掉之前|重新开始|重来).*")) {
            return new PlanResult("如需重置会话，请输入\"重置\"或\"清空记忆\"。", List.of());
        }

        // 0. 隐藏式注入画像
        String personaText = null;
        if (isPersonaEnabled != null && isPersonaEnabled) {
            String userId = UserContext.getCurrentUserId();
            personaText = personaInjector.preloadAndInject(userId, sessionId, city);
        }

        try {
            // 0.5 从搜索记录中推断城市（请求未传 city 时）
            if (city == null || city.isBlank()) {
                city = extractCityFromHistory(sessionId);
            }

            // 1. Decompose -- include city and search context
            StringBuilder planningInput = new StringBuilder();
            if (city != null && !city.isBlank()) {
                planningInput.append("用户所在城市：").append(city).append("。无需再询问城市！\n");
            } else {
                planningInput.append("注意：若历史对话中已出现城市名，直接沿用，不要再询问城市！\n");
            }
            if (searchContext != null && !searchContext.isBlank()) {
                planningInput.append(searchContext).append("\n");
            }
            planningInput.append("用户目标：").append(userMessage);

            List<Step> steps;
            try {
                steps = decompose(planningInput.toString(), sessionId);
            } catch (Exception e) {
                log.warn("任务拆解失败，降级为单步Agent: {}", e.getMessage());
                String reply = runSingleStepAgent(userMessage, city, isPersonaEnabled, sessionId, searchContext, personaText);
                return new PlanResult(reply, extractHouseItems(Map.of()));
            }

            if (steps.isEmpty()) {
                String reply = runSingleStepAgent(userMessage, city, isPersonaEnabled, sessionId, searchContext, personaText);
                return new PlanResult(reply, extractHouseItems(Map.of()));
            }

            // 2. Execute steps sequentially with context passing
            String previousSummary = null;
            boolean anySearchPerformed = false;
            for (int i = 0; i < steps.size(); i++) {
                Step step = steps.get(i);
                log.info("步骤 {}/{}: {} (工具: {})", i + 1, steps.size(), step.description, step.tool);

                try {
                    ToolCallAgent agent = buildAgent(step.tool, isPersonaEnabled, sessionId, personaText);
                    String stepInput = buildStepInput(step.description, step.tool, previousSummary, searchContext);
                    String stepResult = agent.run(stepInput);

                    // 闭环校验：doTerminate 步骤若前面没做过搜索，强制拦截编造数据
                    if ("doTerminate".equals(step.tool) && !anySearchPerformed) {
                        String cleaned = enforceNoFabrication(stepResult);
                        if (!cleaned.equals(stepResult)) {
                            log.warn("闭环拦截：doTerminate 步骤在无搜索的情况下编造了房源数据，已替换");
                            stepResult = cleaned;
                        }
                    }

                    step.result = stepResult;
                    step.status = StepStatus.COMPLETED;
                    previousSummary = summarize(stepResult);

                    // 追踪是否已执行过搜索
                    if ("queryHouses".equals(step.tool) || "searchProjects".equals(step.tool)) {
                        anySearchPerformed = true;
                    }

                    // 检测城市/区县校验是否失败或有歧义，仅对校验工具的结果做检查
                    if (stepResult != null && isCityValidationTool(step.tool) && (
                            stepResult.contains("有歧义") ||
                            stepResult.contains("未匹配") ||
                            stepResult.contains("未找到匹配的城市") ||
                            stepResult.contains("请输入城市") ||
                            stepResult.contains("多个同名")
                    )) {
                        log.info("检测到城市/区县校验不通过或有同名区县歧义，提前结束工作流");
                        for (int j = i + 1; j < steps.size(); j++) {
                            steps.get(j).status = StepStatus.FAILED;
                            steps.get(j).result = "由于前置城市校验未通过或存在歧义，跳过该步骤";
                        }
                        break;
                    }

                    // Persist structured results from tool calls
                    try {
                        persistStepResult(step.tool, sessionId);
                    } catch (Exception e) {
                        log.warn("持久化步骤结果失败: {}", e.getMessage());
                    }
                } catch (Exception e) {
                    log.error("步骤 {} 执行失败: {}", i + 1, e.getMessage());
                    step.status = StepStatus.FAILED;
                    step.result = e.getMessage();
                    previousSummary = "步骤" + (i + 1) + "(" + step.description + ")执行失败，跳过";
                }
            }

            // 3. All failed -- static degradation
            if (steps.stream().allMatch(s -> s.status == StepStatus.FAILED)) {
                return new PlanResult("服务暂时不可用", List.of());
            }

            // 4. Build internal workflow details for logs
            String workflowReport = buildFinalReport(userMessage, steps);
            log.info("=== PlanningFlow 执行链路日志 ===\n{}", workflowReport);

            // 5. Synthesize warm user-facing response
            String rawResponse = synthesizeWarmResponse(userMessage, steps, sessionId);

            // 6. 解析 AI 生成的推荐理由，分离出纯文本回复
            Map<String, String> sellingPoints = new LinkedHashMap<>();
            String cleanReply = parseSellingPoints(rawResponse, sellingPoints);

            // 7. 从向量库提取结构化数据
            List<HouseItem> recommendations = extractHouseItems(sellingPoints);
            List<ProjectItem> projects = extractProjectItems(sellingPoints);

            // 8. Persist clean reply to memory
            chatMemory.add(sessionId, List.of(new UserMessage(userMessage), new AssistantMessage(cleanReply)));

            log.info("=== PlanningFlow 完成 ===");
            return new PlanResult(cleanReply, recommendations, projects);
        } finally {
            // 强制清理 ThreadLocal，防止 executor 线程池复用时数据污染
            customerProfileTool.clearAll();
            houseSearchTool.clearLastQuery();
            houseSearchTool.clearLastSearchDocs();
        }
    }

    /**
     * 硬拦截：检测无搜索步骤时 Agent 是否编造了具体房源数据。
     * 匹配模式：面积（XX㎡）、价格（XX万/万元）、户型（X室X厅）等真实房源特征。
     * 命中则返回降级安全文本，未命中返回原文。
     */
    private String enforceNoFabrication(String response) {
        if (response == null || response.isBlank()) return response;

        long score = 0;
        // 面积模式：数字 + ㎡/平方米（如 95㎡、128 ㎡）
        if (java.util.regex.Pattern.compile("\\d{2,4}\\s*㎡").matcher(response).find()) score++;
        if (java.util.regex.Pattern.compile("\\d{2,4}\\s*平方米").matcher(response).find()) score++;
        // 价格模式：数字 + 万/万元（如 712.5万、480万）
        if (java.util.regex.Pattern.compile("\\d{2,5}(?:\\.\\d)?\\s*万").matcher(response).find()) score++;
        // 户型模式：X室X厅（如 3室2厅）
        if (java.util.regex.Pattern.compile("\\d+室\\d+厅").matcher(response).find()) score++;
        // 面积-价格组合（如 95㎡、712.5万元）
        if (java.util.regex.Pattern.compile("\\d{2,4}\\s*㎡.*\\d{2,5}(?:\\.\\d)?\\s*万").matcher(response).find()) score += 2;

        // 命中 2 项及以上特征 = 高度疑似编造房源数据
        if (score >= 2) {
            return "抱歉，我暂时无法为您查询具体房源信息。请告诉我您想了解哪个城市或区域的房源，我马上帮您搜索。";
        }
        return response;
    }

    /** 从会话搜索历史中提取最近一次使用的城市，用于短消息的场景补全 */
    private String extractCityFromHistory(String sessionId) {
        try {
            return searchStateService.getLastCity(sessionId);
        } catch (Exception e) {
            log.debug("从搜索历史提取城市失败: {}", e.getMessage());
            return null;
        }
    }

    private void persistStepResult(String tool, String sessionId) {
        if (tool == null) return;
        switch (tool) {
            case "getCustomerProfile" -> {
                boolean hasData = customerProfileTool.getLastHasData();
                customerProfileTool.clearLast();
                searchStateService.markProfileFetched(sessionId, hasData, null, null);
            }
            case "queryHouses", "searchProjects" -> {
                int count = houseSearchTool.getLastQueryCount();
                var params = houseSearchTool.getLastQueryParams();
                houseSearchTool.clearLastQuery();
                searchStateService.recordSearch(sessionId, tool,
                        params != null ? new java.util.HashMap<>(params) : null, count);
            }
        }
    }

    // ---- Chat History ----

    private String buildChatHistoryContext(String sessionId) {
        try {
            var all = chatMemory.get(sessionId);
            if (all == null || all.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            int exchanges = 0;
            int maxExchanges = 6;
            for (int i = all.size() - 1; i >= 0 && exchanges < maxExchanges; i--) {
                Message m = all.get(i);
                if (m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT) {
                    String text = m.getText();
                    if (text != null && text.length() > 300) {
                        text = text.substring(0, 300) + "...";
                    }
                    String role = m.getMessageType() == MessageType.USER ? "用户" : "助手";
                    sb.insert(0, role + "：" + (text != null ? text : "") + "\n\n");
                    if (m.getMessageType() == MessageType.USER) exchanges++;
                }
            }
            if (sb.isEmpty()) return "";
            String template = readResource(chatHistoryContextPrompt);
            return template.replace("{history}", sb.toString());
        } catch (Exception e) {
            log.warn("读取对话历史失败: {}", e.getMessage());
            return "";
        }
    }

    // ---- Decompose ----

    private List<Step> decompose(String objective, String sessionId) {
        String format = """
                {
                  "steps": [
                    { "description": "步骤描述", "tool": "工具名或空字符串" }
                  ]
                }
                """;

        // Manually fetch recent ChatMemory — avoids advisor writing internal calls
        String chatHistory = buildChatHistoryContext(sessionId);

        String resourceContent = readResource(planningPrompt);
        PromptTemplate template = new PromptTemplate(resourceContent);
        template.add("task", objective);
        template.add("format", format);

        String sysTemplate = readResource(decomposeSystemPrompt);
        String planText = chatClient.prompt()
                .system(s -> s.text(sysTemplate.replace("{chat_history}", chatHistory.isEmpty() ? "（无）" : chatHistory)))
                .user(template.create().getContents())
                .call()
                .content();

        if (planText == null || planText.isBlank()) {
            return List.of();
        }

        // Extract JSON from response (may be wrapped in ```json ... ```)
        String json = planText.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
        }

        List<Step> steps = parseSteps(json);
        if (steps != null) return steps;

        // 解析失败时尝试从末尾提取最后一个 JSON 对象（LLM 可能在前面写了推理文本）
        int lastBrace = json.lastIndexOf('{');
        if (lastBrace >= 0) {
            String tailJson = json.substring(lastBrace);
            steps = parseSteps(tailJson);
            if (steps != null) {
                log.warn("从 LLM 输出末尾提取 JSON 成功");
                return steps;
            }
        }

        log.error("JSON 解析失败，LLM 输出: {}", planText.length() > 200 ? planText.substring(0, 200) + "..." : planText);
        return List.of();
    }

    // ---- Agent Building ----

    private ToolCallAgent buildAgent(String stepTool, Boolean isPersonaEnabled, String sessionId, String personaText) {
        ToolCallAgent agent = agentProvider.getObject();

        List<ToolCallback> tools = new ArrayList<>();
        String requiredToolName = null;

        if (stepTool != null && !stepTool.isBlank()) {
            requiredToolName = stepTool;
            switch (stepTool) {
                case "queryHouses", "searchProjects" -> {
                    tools.addAll(List.of(ToolCallbacks.from(houseSearchTool)));
                    tools.addAll(List.of(ToolCallbacks.from(citySearchTool)));
                }
                case "validateCityDistrict", "searchCities", "listDistricts" ->
                        tools.addAll(List.of(ToolCallbacks.from(citySearchTool)));
                case "doTerminate" -> {} // analysis step — terminate only
                default -> {
                    tools.addAll(List.of(ToolCallbacks.from(houseSearchTool)));
                    tools.addAll(List.of(ToolCallbacks.from(citySearchTool)));
                    tools.addAll(List.of(ToolCallbacks.from(customerProfileTool)));
                }
            }
        } else {
            requiredToolName = "doTerminate";
        }
        tools.addAll(List.of(ToolCallbacks.from(terminateTool)));

        // Build dynamic system prompt with the required tool instruction
        String basePrompt = readResource(systemAgentPrompt);
        String toolInstruction;
        if (requiredToolName == null) {
            toolInstruction = "";
        } else if ("doTerminate".equals(requiredToolName)) {
            toolInstruction = readResource(stepTerminateInstructionPrompt);
        } else {
            toolInstruction = readResource(stepToolInstructionPrompt)
                    .replace("{tool_name}", requiredToolName);
        }
        String dynamicPrompt = basePrompt.replace("{required_tool}", toolInstruction);

        // 追加画像到 System Prompt 尾部
        String fullPrompt = dynamicPrompt;
        if (personaText != null && !personaText.isBlank()) {
            fullPrompt = dynamicPrompt + "\n\n" + personaText;
        }

        String history = buildChatHistoryContext(sessionId);

        agent.configure("StepAgent", chatClient,
                fullPrompt,
                tools.toArray(new ToolCallback[0]),
                requiredToolName, history);
        return agent;
    }

    // ---- Step Input / Summary ----

    private String buildStepInput(String stepDescription, String tool, String previousSummary,
                                  String searchContext) {
        String prev = (previousSummary != null && !previousSummary.isBlank())
                ? "\n\n【前序步骤结果摘要】\n" + previousSummary : "";

        if (tool == null || tool.isBlank()) {
            return "请执行以下任务步骤：\n" + stepDescription + prev;
        }

        if ("doTerminate".equals(tool)) {
            String ctx = (searchContext != null && !searchContext.isBlank())
                    ? "\n\n【本次会话搜索记录】\n" + searchContext
                      + "\n以上标记为「0条」的区域/类型组合请勿推荐。"
                    : "";
            String template = readResource(stepInputTerminatePrompt);
            return template.replace("{step_description}", stepDescription)
                    .replace("{search_context}", ctx)
                    .replace("{prev_summary}", prev);
        } else {
            String template = readResource(stepInputToolPrompt);
            return template.replace("{step_description}", stepDescription)
                    .replace("{tool_name}", tool)
                    .replace("{prev_summary}", prev);
        }
    }

    private String summarize(String stepResult) {
        if (stepResult == null || stepResult.isBlank()) {
            return "步骤无结果";
        }
        try {
            PromptTemplate template = new PromptTemplate(stepSummaryPrompt);
            template.add("stepResult", stepResult);
            String summary = chatClient.prompt(template.create()).call().content();
            return summary != null && !summary.isBlank() ? summary
                    : stepResult.length() > 150 ? stepResult.substring(0, 150) + "..." : stepResult;
        } catch (Exception e) {
            log.warn("摘要生成失败: {}", e.getMessage());
            return stepResult.length() > 150 ? stepResult.substring(0, 150) + "..." : stepResult;
        }
    }

    private String buildFinalReport(String objective, List<Step> steps) {
        StringBuilder report = new StringBuilder();
        report.append("## ").append(objective).append("\n\n");

        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            String icon = s.status == StepStatus.COMPLETED ? "成功" : "失败";
            report.append(String.format("**步骤 %d: %s** %s\n", i + 1, s.description, icon));
            if (s.status == StepStatus.COMPLETED && s.result != null) {
                report.append(s.result).append("\n\n");
            } else if (s.status == StepStatus.FAILED) {
                report.append("> 失败: ").append(s.result).append("\n\n");
            }
        }

        long completed = steps.stream().filter(s -> s.status == StepStatus.COMPLETED).count();
        report.append(String.format("---\n**执行完毕：%d/%d 步骤成功**", completed, steps.size()));
        return report.toString();
    }

    private String synthesizeWarmResponse(String userMessage, List<Step> steps, String sessionId) {
        StringBuilder stepsData = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            if (s.status == StepStatus.COMPLETED && s.result != null) {
                stepsData.append(String.format("【步骤 %d：%s】执行结果：\n%s\n\n", i + 1, s.description, s.result));
            }
        }

        String prompt = readResource(warmResponsePrompt);

        try {
            return chatClient.prompt()
                    .user(u -> u.text(prompt)
                            .param("user_message", userMessage)
                            .param("steps_data", stepsData.toString()))
                    .advisors(a -> a.param(org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("合成温暖导购话术失败，使用兜底逻辑: {}", e.getMessage());
            return steps.stream()
                    .filter(s -> s.status == StepStatus.COMPLETED && s.result != null)
                    .reduce((first, second) -> second)
                    .map(s -> s.result)
                    .orElse("服务暂时不可用");
        }
    }

    /**
     * 从 LLM 原始响应中解析 AI 生成的推荐理由。
     * 响应格式：纯文本回复 + "---SELLING_POINTS---" + 每行一条 "房源ID: 推荐理由"。
     * 返回干净的纯文本回复（不含分隔符及之后的任何内容），sellingPoints 被原地填充。
     */
    private String parseSellingPoints(String rawResponse, Map<String, String> sellingPoints) {
        if (rawResponse == null || rawResponse.isBlank()) return rawResponse;

        String delimiter = "---SELLING_POINTS---";
        int idx = rawResponse.indexOf(delimiter);
        if (idx < 0) {
            // 分隔符可选 — 未输出则使用房源标签作为推荐理由
            return rawResponse;
        }

        String reply = rawResponse.substring(0, idx).trim();
        String pointsBlock = rawResponse.substring(idx + delimiter.length()).trim();

        // 清除可能包裹的代码块标记
        pointsBlock = pointsBlock.replaceAll("^```[\\s\\S]*?\\n?", "").replaceAll("\\n?```$", "").trim();

        for (String line : pointsBlock.split("\\n")) {
            line = line.trim();
            if (line.isBlank()) continue;
            int colon = line.indexOf(':');
            if (colon < 0) colon = line.indexOf('：');  // 兼容中文冒号
            if (colon > 0 && colon < line.length() - 1) {
                String rawKey = line.substring(0, colon).trim();
                String point = line.substring(colon + 1).trim();
                // 规范化 key：去掉 LLM 可能添加的前缀（#、房源、房源ID 等），只保留数字/字母
                String key = rawKey.replaceAll("^[#\\s]*房源(?:ID)?\\s*", "").trim();
                if (!key.isBlank() && !point.isBlank()) {
                    sellingPoints.putIfAbsent(key, point);
                }
            }
        }

        if (!sellingPoints.isEmpty()) {
            log.info("解析到 {} 条 AI 推荐理由", sellingPoints.size());
        }
        return reply;
    }

    /**
     * 从 HouseSearchTool 缓存的搜索 Document 中提取结构化 HouseItem 列表。
     * 推荐理由优先级：AI 生成 > 房源标签 > null。
     */
    private List<HouseItem> extractHouseItems(Map<String, String> sellingPoints) {
        List<Document> docs = houseSearchTool.getLastSearchDocs();
        if (docs == null || docs.isEmpty()) return List.of();

        return docs.stream()
                .map(d -> {
                    String houseId = stringMeta(d, "houseId");
                    String projectName = stringMeta(d, "projectName");
                    String district = stringMeta(d, "district");
                    String price = stringMeta(d, "priceText");
                    String layout = coalesce(stringMeta(d, "roomType"), stringMeta(d, "layout"));
                    String area = stringMeta(d, "areaText");
                    String coverUrl = stringMeta(d, "coverImage");
                    String tags = stringMeta(d, "tags");

                    // 推荐理由：AI 生成优先 → 房源标签兜底
                    String sellingPoint = resolveSellingPoint(houseId, sellingPoints);
                    if (sellingPoint == null) {
                        sellingPoint = tags;
                    }
                    if (projectName == null) return null;
                    return new HouseItem(houseId, projectName, district, price, layout, area, coverUrl, sellingPoint);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** 从 sellingPoints 映射中匹配推荐理由：精确 → 模糊 → 兜底 null */
    private String resolveSellingPoint(String houseId, Map<String, String> sellingPoints) {
        if (houseId == null || sellingPoints.isEmpty()) return null;
        String point = sellingPoints.get(houseId);
        if (point != null) return point;
        return sellingPoints.entrySet().stream()
                .filter(e -> e.getKey().contains(houseId) || houseId.contains(e.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    /** 从缓存的搜索文档中提取项目级结构化数据（仅 type=project 的文档） */
    private List<ProjectItem> extractProjectItems(Map<String, String> sellingPoints) {
        List<Document> docs = houseSearchTool.getLastSearchDocs();
        if (docs == null || docs.isEmpty()) return List.of();

        return docs.stream()
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

                    // 价格/面积区间
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

                    String sellingPoint = resolveSellingPoint(projectId, sellingPoints);
                    if (sellingPoint == null) sellingPoint = tags;

                    if (projectName == null) return null;
                    return new ProjectItem(projectId, projectName, district, address, developer,
                            houseCount, priceRange, areaRange, layouts, tags, coverUrl, sellingPoint);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static String stringMeta(Document doc, String key) {
        Object val = doc.getMetadata().get(key);
        return val != null ? val.toString() : null;
    }

    private static String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    // ---- Degradation ----

    private String runSingleStepAgent(String userMessage, String city,
                                      Boolean isPersonaEnabled, String sessionId,
                                      String searchContext, String personaText) {
        try {
            ToolCallAgent agent = buildAgent(null, isPersonaEnabled, sessionId, personaText);
            String stepInput = buildStepInput("综合分析用户需求", null, searchContext, searchContext);
            return agent.run(stepInput);
        } catch (Exception e) {
            log.error("单步Agent降级也失败: {}", e.getMessage());
            return "服务暂时不可用";
        }
    }

    // ---- Utility ----

    /**
     * 尝试将字符串解析为 Step 列表，失败返回 null。
     */
    private List<Step> parseSteps(String json) {
        try {
            Map<String, List<Map<String, String>>> parsed =
                    MAPPER.readValue(json, new TypeReference<Map<String, List<Map<String, String>>>>() {});
            List<Map<String, String>> rawSteps = parsed.get("steps");
            if (rawSteps == null || rawSteps.isEmpty()) return List.of();

            List<Step> steps = new ArrayList<>();
            for (Map<String, String> s : rawSteps) {
                Step step = new Step();
                step.description = s.getOrDefault("description", "").trim();
                step.tool = s.getOrDefault("tool", "").trim();
                if (step.tool.isEmpty()) step.tool = null;
                step.status = StepStatus.NOT_STARTED;
                if (!step.description.isEmpty()) steps.add(step);
            }
            return steps;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isCityValidationTool(String tool) {
        return "validateCityDistrict".equals(tool)
                || "searchCities".equals(tool)
                || "listDistricts".equals(tool);
    }

    private String readResource(Resource resource) {
        try {
            return new String(resource.getInputStream().readAllBytes());
        } catch (Exception e) {
            log.warn("无法读取提示词资源: {}", e.getMessage());
            return "";
        }
    }

    // ---- Lifecycle ----

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ---- Inner Model ----

    private static class Step {
        String description;
        String tool;
        String result;
        StepStatus status;
    }
}
