package com.guang.aiassistant.service;

import com.guang.aiassistant.config.PromptConfig;
import com.guang.aiassistant.model.ExplanationResult;
import com.guang.aiassistant.model.HouseItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * LLM 推荐理由生成 — 为每个命中的房源生成个性化推荐话术。
 * 使用 classifierChatClient（无记忆），5 秒超时，失败时返回空 map。
 */
@Slf4j
@Service
public class ExplanationService {

    private final ChatClient classifierChatClient;
    private final PromptConfig promptConfig;

    public ExplanationService(@Qualifier("classifierChatClient") ChatClient classifierChatClient,
                              PromptConfig promptConfig) {
        this.classifierChatClient = classifierChatClient;
        this.promptConfig = promptConfig;
    }

    /**
     * 为房源列表生成推荐理由。
     *
     * @return houseId → reason 的映射，失败时返回空 map
     */
    public Map<String, String> generateExplanations(String userQuery, String preferences,
                                                     List<HouseItem> houses, String sessionId) {
        if (houses == null || houses.isEmpty()) return Map.of();

        try {
            return CompletableFuture.supplyAsync(() -> doGenerate(userQuery, preferences, houses))
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("推荐理由生成超时或失败，降级无解释: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, String> doGenerate(String userQuery, String preferences, List<HouseItem> houses) {
        try {
            String housesText = houses.stream()
                    .map(h -> String.format("- ID:%s 项目:%s 区域:%s 卖点:%s",
                            h.houseId(), h.projectName(), h.district(),
                            h.sellingPoint() != null ? h.sellingPoint() : "无"))
                    .collect(Collectors.joining("\n"));

            String template = promptConfig.getHouseExplanationPrompt();
            String system = template
                    .replace("\\{userQuery\\}", userQuery != null ? userQuery : "")
                    .replace("\\{preferences\\}", preferences != null ? preferences : "无特殊偏好")
                    .replace("\\{houses\\}", housesText);

            ExplanationResult result = classifierChatClient.prompt()
                    .system(system)
                    .user("请为以上房源生成推荐理由")
                    .call()
                    .entity(ExplanationResult.class);

            if (result == null || result.explanations() == null) return Map.of();

            return result.explanations().stream()
                    .filter(e -> e.houseId() != null && e.reason() != null)
                    .collect(Collectors.toMap(
                            ExplanationResult.HouseExplanation::houseId,
                            ExplanationResult.HouseExplanation::reason,
                            (a, b) -> a));
        } catch (Exception e) {
            log.warn("推荐理由生成异常: {}", e.getMessage());
            return Map.of();
        }
    }
}
