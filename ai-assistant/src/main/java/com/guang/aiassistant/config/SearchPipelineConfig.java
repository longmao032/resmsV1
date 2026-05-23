package com.guang.aiassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 搜索管道配置 — 召回池大小、结果数、缓存 TTL、排序权重。
 */
@Configuration
@ConfigurationProperties(prefix = "search")
@Getter
@Setter
public class SearchPipelineConfig {

    private Pipeline pipeline = new Pipeline();
    private Reranker reranker = new Reranker();

    @Getter
    @Setter
    public static class Pipeline {
        private int minResults = 3;
        private int cacheTtlMinutes = 8;
        private int recallPoolSize = 150;
        private int resultLimit = 5;
    }

    @Getter
    @Setter
    public static class Reranker {
        private double weightTag = 0.25;
        private double weightPersona = 0.20;
        private double weightSemantic = 0.25;
        private double weightPrice = 0.15;
        private double weightLocation = 0.15;
    }
}
