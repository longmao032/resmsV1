package com.guang.aiassistant.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class PromptConfig {

    @Value("classpath:/prompts/chitchat.st")
    private Resource chitchatResource;

    @Value("classpath:/prompts/policy-advisor.st")
    private Resource policyAdvisorResource;

    @Value("classpath:/prompts/warm-response.st")
    private Resource warmResponseResource;

    @Value("classpath:/prompts/intent-extractor.st")
    private Resource intentExtractorResource;

    @Value("classpath:/prompts/system-guide.st")
    private Resource systemGuideResource;

    @Value("classpath:/prompts/house-explanation.st")
    private Resource houseExplanationResource;

    private String chitchatPrompt;
    private String policyAdvisorPrompt;
    private String warmResponsePrompt;
    private String intentExtractorPrompt;
    private String systemGuidePrompt;
    private String houseExplanationPrompt;

    @PostConstruct
    public void init() {
        this.chitchatPrompt = loadResource(chitchatResource, "chitchat.st");
        this.policyAdvisorPrompt = loadResource(policyAdvisorResource, "policy-advisor.st");
        this.warmResponsePrompt = loadResource(warmResponseResource, "warm-response.st");
        this.intentExtractorPrompt = loadResource(intentExtractorResource, "intent-extractor.st");
        this.systemGuidePrompt = loadResource(systemGuideResource, "system-guide.st");
        this.houseExplanationPrompt = loadResource(houseExplanationResource, "house-explanation.st");
        log.info("Prompt 模板文件全部加载并缓存完毕。");
    }

    public String getChitchatPrompt() {
        return chitchatPrompt;
    }

    public String getPolicyAdvisorPrompt() {
        return policyAdvisorPrompt;
    }

    public String getWarmResponsePrompt() {
        return warmResponsePrompt;
    }

    public String getIntentExtractorPrompt() {
        return intentExtractorPrompt;
    }

    public String getSystemGuidePrompt() {
        return systemGuidePrompt;
    }

    public String getHouseExplanationPrompt() {
        return houseExplanationPrompt;
    }

    private String loadResource(Resource resource, String filename) {
        try {
            if (resource == null || !resource.exists()) {
                log.error("Prompt 资源文件不存在: {}", filename);
                return "";
            }
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("加载 Prompt 资源文件 {} 失败", filename, e);
            return "";
        }
    }
}
