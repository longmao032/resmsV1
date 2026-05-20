package com.guang.aiassistant.core.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Component
@Scope("prototype")
public class ToolCallAgent extends ReActAgent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolCallback[] availableTools;
    private ChatResponse toolCallChatResponse;
    private final ToolCallingManager toolCallingManager;
    private final ChatOptions chatOptions;

    private int noToolCallCount = 0;
    private boolean toolEverCalled = false;
    private String requiredToolName = null;

    public ToolCallAgent() {
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = OpenAiChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();
        setMaxSteps(6);
    }

    /**
     * Called by PlanningFlow to configure this agent instance before run().
     */
    public void configure(String name, ChatClient chatClient, String systemPrompt,
                          ToolCallback[] tools, String requiredToolName, String formattedHistory) {
        setName(name);
        setChatClient(chatClient);
        setSystemPrompt(systemPrompt);
        this.availableTools = tools;
        this.requiredToolName = requiredToolName;
        this.formattedHistory = formattedHistory;
    }

    @SuppressWarnings("varargs")
    @Override
    public boolean think() {
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            getMessagesList().add(new UserMessage(getNextStepPrompt()));
        }

        List<Message> messageList = getMessagesList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);

        ChatResponse chatResponse = getChatClient().prompt(prompt)
                .system(getSystemPrompt())
                .toolCallbacks(availableTools)
                .call()
                .chatResponse();

        this.toolCallChatResponse = chatResponse;
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();

        log.info("{} 的思考：{}", getName(), assistantMessage.getText());
        log.info("{} 选择了 {} 个工具来使用", getName(), toolCallList.size());

        if (!toolCallList.isEmpty()) {
            log.info("工具调用: {}", toolCallList.stream()
                    .map(tc -> String.format("%s(%s)", tc.name(), tc.arguments()))
                    .collect(Collectors.joining(", ")));
        }

        if (toolCallList.isEmpty()) {
            getMessagesList().add(assistantMessage);
            if (!toolEverCalled && noToolCallCount < 2) {
                noToolCallCount++;
                log.warn("Agent 未调用任何工具 (第{}次)，注入提醒重试", noToolCallCount);
                String nag = requiredToolName != null
                        ? "你还没有调用 " + requiredToolName + "。请立即调用它获取真实数据，不要凭记忆回答。调用工具后再用 doTerminate 返回结果。"
                        : "你还没有调用任何工具。请立即调用工具获取真实数据，不要凭记忆回答。调用工具后再用 doTerminate 返回结果。";
                getMessagesList().add(new UserMessage(nag));
                return true;
            }
            return false;
        }
        noToolCallCount = 0;
        return true;
    }

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }

        toolEverCalled = true;
        noToolCallCount = 0;

        Prompt prompt = new Prompt(getMessagesList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult;
        try {
            toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        } catch (Exception e) {
            // LLM 有时生成 "minArea": , 等畸形 JSON，尝试修复后重试一次
            log.warn("工具调用 JSON 解析失败，尝试修复重试: {}", e.getMessage());
            try {
                toolCallChatResponse = sanitizeToolCallArguments(toolCallChatResponse);
                toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
            } catch (Exception retryException) {
                log.error("修复重试仍失败", retryException);
                return "工具调用参数格式错误，请用更简洁的条件重新查询。";
            }
        }

        setMessagesList(toolExecutionResult.conversationHistory());

        ToolResponseMessage toolResponseMessage =
                (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());

        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            setState(AgentConstant.FINISHED);
        }

        if (terminateToolCalled) {
            String finalResponse = toolResponseMessage.getResponses().stream()
                    .filter(r -> r.name().equals("doTerminate"))
                    .map(r -> parseResponseData(r.responseData()))
                    .collect(Collectors.joining("\n"));
            log.info("Agent finished via doTerminate, response length: {}", finalResponse.length());
            return finalResponse;
        }

        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 返回的结果：" + parseResponseData(response.responseData()))
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }

    /**
     * 修复 LLM 常见的 JSON 参数错误：
     * - "key": , (空值) → 移除该键
     * - "key": "" (空字符串) → 移除该键
     */
    ChatResponse sanitizeToolCallArguments(ChatResponse response) {
        AssistantMessage original = response.getResult().getOutput();
        List<AssistantMessage.ToolCall> fixedCalls = new ArrayList<>();
        boolean changed = false;

        for (AssistantMessage.ToolCall tc : original.getToolCalls()) {
            String raw = tc.arguments();
            String fixed = fixJson(raw);
            if (!fixed.equals(raw)) {
                changed = true;
                fixedCalls.add(new AssistantMessage.ToolCall(
                        tc.id(), tc.type(), tc.name(), fixed));
            } else {
                fixedCalls.add(tc);
            }
        }

        if (!changed) return response;

        AssistantMessage fixedMsg = AssistantMessage.builder()
                .content(original.getText())
                .properties(original.getMetadata())
                .toolCalls(fixedCalls)
                .build();
        return ChatResponse.builder()
                .from(response)
                .generations(List.of(new Generation(fixedMsg)))
                .build();
    }

    /**
     * 修复畸形 JSON：移除值为空白的键（"key": , 或 "key": ""），
     * 移除尾部多余的逗号。
     */
    static String fixJson(String json) {
        if (json == null || json.isBlank()) return json;
        // 1. 移除 "key": ,  这类空值（key 后紧跟逗号）
        String fixed = json.replaceAll("\"[^\"]+\":\\s*,", "");
        // 2. 移除 , "key": } 末尾空值（key 前有逗号，后跟右括号）
        fixed = fixed.replaceAll(",\\s*\"[^\"]+\":\\s*}", "}");
        // 3. 移除 {"key": } 唯一的空值
        fixed = fixed.replaceAll("\\{\\s*\"[^\"]+\":\\s*}", "{}");
        // 4. 移除 "key": "" 这类空字符串值
        fixed = fixed.replaceAll("\"[^\"]+\":\\s*\"\"", "");
        // 5. 修复留下的连续逗号 "{ ," 或 ", ," 或 ", }"
        fixed = fixed.replaceAll(",\\s*,+", ",");
        fixed = fixed.replaceAll("\\{\\s*,", "{");
        fixed = fixed.replaceAll(",\\s*}", "}");
        return fixed;
    }

    private String parseResponseData(String responseData) {
        if (responseData == null || responseData.isBlank()) {
            return "";
        }
        if (responseData.startsWith("\"") && responseData.endsWith("\"")) {
            try {
                return MAPPER.readValue(responseData, String.class);
            } catch (Exception e) {
                log.warn("反序列化工具响应数据失败: {}", e.getMessage());
            }
        }
        return responseData;
    }

    @Override
    protected void clean() {
        getMessagesList().clear();
        noToolCallCount = 0;
        toolEverCalled = false;
        requiredToolName = null;
    }
}
