package com.guang.aiassistant.service;

import com.guang.aiassistant.core.flow.PlanningFlow;
import com.guang.aiassistant.core.flow.PlanningFlow.PlanResult;
import com.guang.aiassistant.core.router.IntentRouter;
import com.guang.aiassistant.core.router.IntentType;
import com.guang.aiassistant.model.RecommendationResponse;
import com.guang.aiassistant.service.SearchStateService;
import org.springframework.ai.chat.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ChatServiceImpl {

    private final IntentRouter intentRouter;
    private final ChatClient fastChatClient;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final PlanningFlow planningFlow;
    private final SearchStateService searchStateService;
    private final JdbcTemplate jdbcTemplate;

    private final Resource chitchatPrompt;
    private final Resource policyAdvisorPrompt;

    public ChatServiceImpl(
            IntentRouter intentRouter,
            @Qualifier("fastChatClient") ChatClient fastChatClient,
            QuestionAnswerAdvisor questionAnswerAdvisor,
            PlanningFlow planningFlow,
            SearchStateService searchStateService,
            JdbcTemplate jdbcTemplate,
            @Value("classpath:/prompts/chitchat.st") Resource chitchatPrompt,
            @Value("classpath:/prompts/policy-advisor.st") Resource policyAdvisorPrompt) {
        this.intentRouter = intentRouter;
        this.fastChatClient = fastChatClient;
        this.questionAnswerAdvisor = questionAnswerAdvisor;
        this.planningFlow = planningFlow;
        this.searchStateService = searchStateService;
        this.jdbcTemplate = jdbcTemplate;
        this.chitchatPrompt = chitchatPrompt;
        this.policyAdvisorPrompt = policyAdvisorPrompt;
    }

    public ChatResult chat(String message, String sessionId, String city, Boolean isPersonaEnabled) {
        IntentType intent = intentRouter.classify(message);
        log.info("Intent: {} for sessionId: {}", intent, sessionId);

        return switch (intent) {
            case CHITCHAT -> new ChatResult(sessionId, RecommendationResponse.of(handleChitchat(message, sessionId), List.of()));
            case POLICY -> new ChatResult(sessionId, RecommendationResponse.of(handlePolicy(message, sessionId), List.of()));
            case SEARCH -> handleSearch(message, sessionId, city, isPersonaEnabled);
            case RESET -> new ChatResult(sessionId, RecommendationResponse.of(handleReset(sessionId), List.of()));
        };
    }

    private String handleChitchat(String message, String sessionId) {
        try {
            String system = readResource(chitchatPrompt);
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
            String system = readResource(policyAdvisorPrompt);
            String reply = fastChatClient.prompt()
                    .system(system)
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .advisors(questionAnswerAdvisor)
                    .call()
                    .content();
            if (reply == null || reply.isBlank()) {
                return "抱歉，暂时无法查询到相关政策信息，请稍后再试。";
            }
            return reply;
        } catch (Exception e) {
            log.error("Policy handler failed", e);
            return "抱歉，政策咨询服务暂时不可用，请稍后再试。";
        }
    }

    private ChatResult handleSearch(String message, String sessionId,
                                    String city, Boolean isPersonaEnabled) {
        PlanResult result = planningFlow.executePlan(message, sessionId, city, isPersonaEnabled);
        if (!result.projects().isEmpty()) {
            return new ChatResult(sessionId, RecommendationResponse.withProjects(
                    result.reply(), result.recommendations(), result.projects()));
        }
        return new ChatResult(sessionId, RecommendationResponse.of(result.reply(), result.recommendations()));
    }

    private String handleReset(String sessionId) {
        searchStateService.clearSession(sessionId);
        try {
            jdbcTemplate.update("DELETE FROM chat_memory WHERE conversation_id = ?", sessionId);
        } catch (Exception e) {
            log.warn("清空 ChatMemory 失败: {}", e.getMessage());
        }
        log.info("会话 {} 已重置", sessionId);
        return "已清空当前会话的搜索记忆和对话历史，您可以重新开始。";
    }

    private String readResource(Resource resource) {
        try {
            return new String(resource.getInputStream().readAllBytes());
        } catch (Exception e) {
            log.warn("Failed to read resource: {}", resource.toString(), e);
            return "";
        }
    }
}
