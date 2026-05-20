package com.guang.integration.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建会话请求 DTO
 */
@Data
@Schema(description = "创建或获取会话请求")
public class CreateSessionDTO {

    @NotNull(message = "会话类型不能为空")
    @Schema(description = "会话类型：1=员工私聊，2=员工群聊，3=销售与C端客户")
    private Byte sessionType;

    @Schema(description = "会话名称（群聊时必填）")
    private String sessionName;

    @NotEmpty(message = "会话成员不能为空")
    @Schema(description = "初始成员列表")
    private List<MemberDTO> members;

    @Data
    @Schema(description = "成员信息")
    public static class MemberDTO {

        @NotNull(message = "用户类型不能为空")
        @Schema(description = "用户类型：1=员工，2=C端客户")
        private Byte userType;

        @NotNull(message = "用户ID不能为空")
        @Schema(description = "用户ID")
        private Long userId;
    }
}
