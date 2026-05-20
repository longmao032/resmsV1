package com.guang.aiassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    @Qualifier("classifierChatClient")
    public ChatClient classifierChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Qualifier("fastChatClient")
    public ChatClient fastChatClient(
            ChatModel chatModel,
            ChatMemory chatMemory,
            MyLoggerAdvisor myLoggerAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        myLoggerAdvisor)
                .build();
    }

    @Bean
    public MyLoggerAdvisor myLoggerAdvisor() {
        return new MyLoggerAdvisor();
    }

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(3)
                        .similarityThreshold(0.5)
                        .build())
                .build();
    }
}
