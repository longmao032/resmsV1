package com.guang.aiassistant.controller;

import com.guang.aiassistant.core.UserContext;
import com.guang.aiassistant.model.ApiResult;
import com.guang.aiassistant.model.ChatRequest;
import com.guang.aiassistant.model.RecommendationResponse;
import com.guang.aiassistant.model.ChatResult;
import com.guang.aiassistant.service.ChatService;
import com.guang.aiassistant.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@CrossOrigin
@Slf4j
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final DataSyncService dataSyncService;

    public ChatController(ChatService chatService, DataSyncService dataSyncService) {
        this.chatService = chatService;
        this.dataSyncService = dataSyncService;
    }

    @PostMapping("/chat")
    public ApiResult<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            return ApiResult.error(400, "userId 不能为空");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ApiResult.error(400, "message 不能为空");
        }

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        UserContext.setCurrentUserId(request.getUserId());
        try {
            ChatResult result = chatService.chat(
                    request.getMessage(),
                    sessionId,
                    request.getCity(),
                    request.getIsPersonaEnabled()
            );
            RecommendationResponse res = result.response();
            log.info("chat reply — sessionId: {}, reply: {}, recommendations: {}, projects: {}",
                    result.sessionId(), res.reply(), res.recommendations().size(), res.projects().size());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("sessionId", result.sessionId());
            data.put("reply", res.reply());
            data.put("recommendations", res.recommendations());
            data.put("projects", res.projects());
            data.put("followUp", res.followUp());
            return ApiResult.success(data);
        } finally {
            UserContext.clear();
        }
    }

    @GetMapping("/health")
    public ApiResult<String> health() {
        return ApiResult.success("OK");
    }

    /**
     * 手动触发房源数据全量同步至向量库
     */
    @PostMapping("/sync")
    public ApiResult<String> sync() {
        int count = dataSyncService.syncAllHouses();
        return ApiResult.success("同步完成，写入 " + count + " 条房源向量记录");
    }
}
