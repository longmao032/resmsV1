package com.guang.aiassistant.service;

import com.guang.aiassistant.config.PromptConfig;
import com.guang.aiassistant.core.router.IntentType;
import com.guang.aiassistant.model.UserIntentForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiIntentExtractor {

    private final ChatClient classifierChatClient;
    private final PromptConfig promptConfig;

    public AiIntentExtractor(
            @Qualifier("classifierChatClient") ChatClient classifierChatClient,
            PromptConfig promptConfig) {
        this.classifierChatClient = classifierChatClient;
        this.promptConfig = promptConfig;
    }

    /**
     * 无状态提取：只根据当前用户消息填表，不携带任何对话历史。
     * 从配置类读取缓存的提示词，确保高性能与解耦。
     */
    public UserIntentForm extract(String message) {
        String systemPrompt = promptConfig.getIntentExtractorPrompt();
        if (systemPrompt == null || systemPrompt.isBlank()) {
            log.warn("提示词内容为空，降级为 SEARCH");
            return fallback(message);
        }

        try {
            UserIntentForm form = classifierChatClient.prompt()
                    .system(systemPrompt)
                    .user(message)
                    .call()
                    .entity(UserIntentForm.class);

            if (form == null) {
                log.warn("结构化提取返回 null，降级为 SEARCH");
                return fallback(message);
            }
            if (form.intentType() == null) {
                log.warn("意图分类为 null，降级为 SEARCH");
                UserIntentForm fallbackForm = new UserIntentForm(IntentType.SEARCH, form.mentionedCity(),
                        form.mentionedDistrict(), form.mentionedSubway(),
                        form.mentionedProject(), form.houseType(),
                        form.minPrice(), form.maxPrice(), form.generalQuery());
                return postProcessHouseType(fallbackForm, message);
            }
            return postProcessHouseType(form, message);
        } catch (Exception e) {
            log.warn("意图提取失败，降级为 SEARCH: {}", e.getMessage());
            return fallback(message);
        }
    }

    /**
     * LLM 提取后处理：若 houseType 未被正确提取，从原始消息中正则兜底。
     */
    private UserIntentForm postProcessHouseType(UserIntentForm form, String message) {
        if (form.houseType() != null || message == null) return form;
        Integer houseType = extractHouseType(message);
        if (houseType == null) {
            String gq = form.generalQuery();
            if (gq != null) houseType = extractHouseType(gq);
        }
        if (houseType != null) {
            // 清掉 generalQuery 中的 houseType 关键词，避免冗余
            String cleanGq = form.generalQuery();
            if (cleanGq != null) {
                cleanGq = cleanGq.replaceAll("新房|新盘|新楼盘|二手房|二手|旧房|租房|出租|租赁", "").trim();
                if (cleanGq.isEmpty()) cleanGq = null;
            }
            return new UserIntentForm(form.intentType(), form.mentionedCity(),
                    form.mentionedDistrict(), form.mentionedSubway(),
                    form.mentionedProject(), houseType,
                    form.minPrice(), form.maxPrice(), cleanGq);
        }
        return form;
    }

    private static Integer extractHouseType(String text) {
        if (text == null) return null;
        if (text.matches(".*(新房|新楼盘|新盘).*")) return 1;
        if (text.matches(".*(二手房|二手|旧房).*")) return 2;
        if (text.matches(".*(租房|出租|租赁).*")) return 3;
        return null;
    }

    private UserIntentForm fallback(String message) {
        IntentType fallbackIntent = IntentType.SEARCH;
        Integer houseType = null;
        if (message != null) {
            String m = message.trim();
            if (m.matches(".*(你好|您好|嗨|hi|hello|谢谢|再见|拜拜|哈哈).*") && m.length() <= 10) {
                fallbackIntent = IntentType.CHITCHAT;
            } else if (m.matches(".*(重置|清空记忆|忘掉|重新开始).*")) {
                fallbackIntent = IntentType.RESET;
            } else if (m.matches(".*(政策|贷款|税费|首付|公积金|购房资格|合同|利率).*")) {
                fallbackIntent = IntentType.POLICY;
            } else {
                houseType = extractHouseType(m);
            }
        }
        return new UserIntentForm(fallbackIntent, null, null, null, null, houseType, null, null, null);
    }
}

