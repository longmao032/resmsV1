package com.guang.portal.controller;

import com.guang.common.result.CommonResult;
import com.guang.common.util.SecurityUtils;
import com.guang.portal.domain.dto.AiChatDTO;
import com.guang.portal.domain.vo.AiChatVO;
import com.guang.portal.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 助手（C端客户）")
@RestController
@RequestMapping("/api/portal/v1/ai")
@RequiredArgsConstructor
public class ClientAiController {

    private final AiAssistantService aiAssistantService;

    @Operation(summary = "向 AI 助手发送消息")
    @PostMapping("/chat")
    public CommonResult<AiChatVO> chat(@RequestBody @Valid AiChatDTO dto) {
        Long appUserId = SecurityUtils.getAppUserId();
        String userId = appUserId != null ? appUserId.toString() : "anonymous";
        AiChatVO vo = aiAssistantService.chat(userId, dto.getMessage(), dto.getSessionId(), dto.getCity());
        return CommonResult.success(vo);
    }
}
