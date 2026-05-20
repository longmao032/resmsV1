package com.guang.aiassistant.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiModelConfig {

    /**
     * 1. 解决 ChatModel 冲突
     * 强行指定 OpenAI (DeepSeek) 为首选，让底层的 ChatClient 默认绑定它
     */
    @Bean
    @Primary
    public ChatModel primaryChatModel(OpenAiChatModel openAiChatModel) {
        return openAiChatModel;
    }

    /**
     * 2. 解决 EmbeddingModel 冲突
     * * 因为容器里同时塞进了 openAiEmbeddingModel 和 dashscopeEmbeddingModel，
     * 我们在参数里使用 @Qualifier("dashscopeEmbeddingModel") 精准抓取通义千问的嵌入模型，
     * 然后打上 @Primary 印章。
     * * 这样 pgvector 寻找嵌入模型时，就会心无旁骛地直接选择 DashScope。
     */
    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(
            @Qualifier("dashscopeEmbeddingModel") EmbeddingModel dashscopeEmbeddingModel) {
        return dashscopeEmbeddingModel;
    }
}