package com.guang.integration.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送消息请求 DTO
 */
@Data
@Schema(description = "发送消息请求")
public class SendMessageDTO {

    @NotNull(message = "会话ID不能为空")
    @Schema(description = "目标会话ID")
    private Long sessionId;

    @Schema(description = "回复的消息ID（可选）")
    private Long parentId;

    @NotNull(message = "消息类型不能为空")
    @Schema(description = "消息类型：1=文本，2=图片，3=文件，4=系统提示")
    private Byte msgType;

    @Schema(description = "消息内容（文本消息必填）")
    private String content;

    @Schema(description = "文件/图片地址（非文本消息使用）")
    private String fileUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "原始文件名")
    private String fileName;
}
