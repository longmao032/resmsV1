package com.guang.aiassistant.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class TerminateTool {

    @Tool(description = "当任务完成或无法完成时调用，终止执行并返回最终回复")
    public String doTerminate(
            @ToolParam(description = "最终的回复内容，包含推荐结果和分析") String finalResponse) {
        return finalResponse;
    }
}
