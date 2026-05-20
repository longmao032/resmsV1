package com.guang.portal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@Schema(description = "AI 聊天请求参数")
public class AiChatDTO {
    @NotEmpty(message = "消息内容不能为空")
    @Schema(description = "用户消息", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "会话ID，首次为空则自动创建新会话")
    private String sessionId;

    @Schema(description = "城市，用于限定房源搜索范围")
    private String city;
}
