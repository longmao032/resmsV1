package com.guang.aiassistant.core.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IntentRouter {

    private final ChatClient classifierChatClient;
    private final Resource intentRouterPrompt;

    public IntentRouter(
            @Qualifier("classifierChatClient") ChatClient classifierChatClient,
            @Value("classpath:/prompts/intent-router.st") Resource intentRouterPrompt) {
        this.classifierChatClient = classifierChatClient;
        this.intentRouterPrompt = intentRouterPrompt;
    }

    public IntentType classify(String message) {
        try {
            String template = new String(intentRouterPrompt.getInputStream().readAllBytes());
            PromptTemplate pt = new PromptTemplate(template);
            pt.add("message", message);

            String response = classifierChatClient.prompt(pt.create()).call().content();
            if (response == null || response.isBlank()) {
                log.warn("Intent classifier returned empty response, defaulting to SEARCH");
                return IntentType.SEARCH;
            }

            String text = response.trim().toLowerCase();
            log.info("Intent classifier raw response: {}", text);

            if (text.contains("reset")) {
                return IntentType.RESET;
            }
            if (text.contains("policy")) {
                return IntentType.POLICY;
            }
            if (text.contains("chitchat")) {
                return IntentType.CHITCHAT;
            }
            if (text.contains("search")) {
                return IntentType.SEARCH;
            }

            log.warn("Intent classifier returned unrecognized response: {}, defaulting to SEARCH", text);
            return IntentType.SEARCH;
        } catch (Exception e) {
            log.warn("Intent classification failed, defaulting to SEARCH: {}", e.getMessage());
            return IntentType.SEARCH;
        }
    }
}
