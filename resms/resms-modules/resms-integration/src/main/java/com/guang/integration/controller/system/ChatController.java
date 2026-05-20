package com.guang.integration.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.common.util.SecurityUtils;
import com.guang.integration.domain.dto.CreateSessionDTO;
import com.guang.integration.domain.dto.SendMessageDTO;
import com.guang.integration.domain.vo.SessionVO;
import com.guang.integration.entity.ChatMessage;
import com.guang.integration.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * B端（员工）聊天接口
 * 路径：/api/system/v1/chat
 * 调用者身份固定为 userType=1（员工），ID 从 JWT 上下文中取
 *
 * @author blackDuck
 * @since 2026-05-11
 */
@Tag(name = "聊天模块（员工端）")
@RestController
@RequestMapping("/api/system/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // B端调用者固定类型
    private static final Byte B_USER_TYPE = 1;

    // -------------------------
    // 会话管理
    // -------------------------

    @Operation(summary = "创建或获取会话（私聊去重）")
    @PostMapping("/sessions")
    @Log(title = "聊天管理", businessType = "CHAT", operatorType = "SAVE")
    public CommonResult<SessionVO> createOrGetSession(@RequestBody @Valid CreateSessionDTO dto) {
        Long userId = SecurityUtils.getUserId().longValue();
        String username = SecurityUtils.getUsername();
        return CommonResult.success(chatService.createOrGetSession(dto, B_USER_TYPE, userId));
    }

    @Operation(summary = "获取我的会话列表")
    @GetMapping("/sessions")
    public CommonResult<List<SessionVO>> listMySessions() {
        Long userId = SecurityUtils.getUserId().longValue();
        return CommonResult.success(chatService.listMySessions(B_USER_TYPE, userId));
    }

    @Operation(summary = "进入会话，清零未读数")
    @PutMapping("/sessions/{sessionId}/read")
    @Log(title = "聊天管理", businessType = "CHAT", operatorType = "UPDATE")
    public CommonResult<Void> readSession(@PathVariable Long sessionId) {
        Long userId = SecurityUtils.getUserId().longValue();
        chatService.readSession(sessionId, B_USER_TYPE, userId);
        return CommonResult.success(null);
    }

    @Operation(summary = "删除会话（当前用户）")
    @DeleteMapping("/sessions/{sessionId}")
    @Log(title = "聊天管理", businessType = "CHAT", operatorType = "DELETE")
    public CommonResult<Void> deleteSession(@PathVariable Long sessionId) {
        Long userId = SecurityUtils.getUserId().longValue();
        chatService.deleteSession(sessionId, B_USER_TYPE, userId);
        return CommonResult.success(null);
    }

    // -------------------------
    // 消息管理
    // -------------------------

    @Operation(summary = "分页拉取历史消息")
    @GetMapping("/sessions/{sessionId}/messages")
    public CommonResult<Page<ChatMessage>> pageMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "30") int pageSize) {
        return CommonResult.success(chatService.pageMessages(sessionId, pageNum, pageSize));
    }

    @Operation(summary = "发送消息")
    @PostMapping("/messages")
    @Log(title = "聊天管理", businessType = "CHAT", operatorType = "SAVE")
    public CommonResult<ChatMessage> sendMessage(@RequestBody @Valid SendMessageDTO dto) {
        Long userId = SecurityUtils.getUserId().longValue();
        String showName = SecurityUtils.getShowName();
        // senderName 直接取 JWT 中的展示名称（快照写入 DB）
        ChatMessage msg = chatService.sendMessage(dto, B_USER_TYPE, userId, showName, null);
        return CommonResult.success(msg);
    }

    @Operation(summary = "撤回消息（2 分钟内）")
    @DeleteMapping("/messages/{messageId}")
    @Log(title = "聊天管理", businessType = "CHAT", operatorType = "DELETE")
    public CommonResult<Void> recallMessage(@PathVariable Long messageId) {
        Long userId = SecurityUtils.getUserId().longValue();
        chatService.recallMessage(messageId, B_USER_TYPE, userId);
        return CommonResult.success(null);
    }
}
