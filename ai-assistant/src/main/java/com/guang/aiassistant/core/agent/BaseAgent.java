package com.guang.aiassistant.core.agent;


import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象基础代理类，用于管理代理和执行流程
 * 提供状态转换、内存管理和基础步骤的执行循环的基础功能
 * 子类必须实现step方法
 */
@Data
@Slf4j
public abstract class BaseAgent {
    //核心属性
    private String name;

    //提示词
    private String SystemPrompt;
    private String nextStepPrompt;

    //代理状态
    private AgentConstant state = AgentConstant.IDLE;

    //执行步骤控制
    private int currentStep = 0;
    private int maxSteps = 6;

    //LLM 大模型
    private ChatClient chatClient;

    //Memory 记忆 (需要自主维护会话上下文）
    private List<Message> messagesList = new ArrayList<>();

    // 预格式化的对话历史，由 PlanningFlow 在 configure 时传入，run() 时注入到 messagesList 头部
    protected String formattedHistory = null;

    /**
     * 运行智能体
     *
     * @param userPrompt 用户提示词
     * @return 调用结果
     */
    public String run(String userPrompt) {
        // 基础校验
        if (this.state != AgentConstant.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }

        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }

        // 执行，更改状态
        this.state = AgentConstant.RUNNING;

        // 记录消息上下文 — 历史在前，当前提示词在后
        if (formattedHistory != null && !formattedHistory.isBlank()) {
            messagesList.add(new UserMessage(formattedHistory));
        }
        messagesList.add(new UserMessage(userPrompt));

        String lastOutput = "";
        try {
            for (int i = 0; i < maxSteps && state != AgentConstant.FINISHED; i++) {
                currentStep = i + 1;
                log.info("Executing step {}/{}", currentStep, maxSteps);
                String stepResult = step();
                if (state == AgentConstant.FINISHED) {
                    lastOutput = stepResult;
                } else if (!stepResult.isBlank()) {
                    lastOutput = stepResult;
                }
            }
            if (currentStep >= maxSteps && state != AgentConstant.FINISHED) {
                state = AgentConstant.FINISHED;
                log.warn("Agent reached max steps ({}) without calling doTerminate", maxSteps);
            }
            if (lastOutput.isBlank()) {
                return "抱歉，我暂时无法处理您的请求，请换一种方式描述您的问题。";
            }
            return lastOutput;
        } catch (Exception e) {
            state = AgentConstant.ERROR;
            log.error("Error executing agent", e);
            return "抱歉，服务暂时不可用，请稍后重试。";
        } finally {
            //清理资源
            this.clean();
        }
    }

    /**
     * 定义单个步骤
     *
     * @return
     */
    public abstract String step();

    protected void clean() {
        formattedHistory = null;
    }


}
