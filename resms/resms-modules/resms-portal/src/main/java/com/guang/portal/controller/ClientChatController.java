package com.guang.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.common.security.LoginUser;
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
 * C端（移动端客户）聊天接口
 * 路径：/api/portal/v1/chat
 * 调用者身份通过 C 端 Token 解析，从 SecurityContext 中获取
 *
 * @author blackDuck
 * @since 2026-05-11
 */
@Tag(name = "聊天模块（C端客户）")
@RestController
@RequestMapping("/api/portal/v1/chat")
@RequiredArgsConstructor
public class ClientChatController {

    private final ChatService chatService;

    // C端调用者固定类型
    private static final Byte C_USER_TYPE = 2;

    // -------------------------
    // 会话管理
    // -------------------------

    @Operation(summary = "发起与销售的会话（C端客户主动发起）")
    @PostMapping("/sessions")
    @Log(title = "聊天管理", businessType = "CHAT", operatorType = "SAVE")
    public CommonResult<SessionVO> createSession(
            @RequestBody @Valid CreateSessionDTO dto) {
        Long appUserId = SecurityUtils.getAppUserId();
        return CommonResult.success(chatService.createOrGetSession(dto, C_USER_TYPE, appUserId));
    }

    @Operation(summary = "获取我的会话列表")
    @GetMapping("/sessions")
    public CommonResult<List<SessionVO>> listMySessions() {
        Long appUserId = SecurityUtils.getAppUserId();
        return CommonResult.success(chatService.listMySessions(C_USER_TYPE, appUserId));
    }

    @Operation(summary = "进入会话，清零未读数")
    @PutMapping("/sessions/{sessionId}/read")
    @Log(title = "聊天管理", businessType = "CHAT", operatorType = "UPDATE")
    public CommonResult<Void> readSession(@PathVariable Long sessionId) {
        Long appUserId = SecurityUtils.getAppUserId();
        chatService.readSession(sessionId, C_USER_TYPE, appUserId);
        return CommonResult.success(null);
    }

    @Operation(summary = "删除会话（隐藏会话列表）")
    @DeleteMapping("/sessions/{sessionId}")
    @Log(title = "聊天管理", businessType = "CHAT", operatorType = "DELETE")
    public CommonResult<Void> deleteSession(@PathVariable Long sessionId) {
        Long appUserId = SecurityUtils.getAppUserId();
        chatService.deleteSession(sessionId, C_USER_TYPE, appUserId);
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
    public CommonResult<ChatMessage> sendMessage(
            @RequestBody @Valid SendMessageDTO dto) {
        Long appUserId = SecurityUtils.getAppUserId();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        String appUserName = loginUser != null ? loginUser.getNickName() : null;
        String appUserAvatar = loginUser != null ? loginUser.getAvatar() : null;
        ChatMessage msg = chatService.sendMessage(dto, C_USER_TYPE, appUserId, appUserName, appUserAvatar);
        return CommonResult.success(msg);
    }

    @Operation(summary = "撤回消息（2 分钟内）")
    @DeleteMapping("/messages/{messageId}")
    @Log(title = "聊天管理", businessType = "CHAT", operatorType = "DELETE")
    public CommonResult<Void> recallMessage(@PathVariable Long messageId) {
        Long appUserId = SecurityUtils.getAppUserId();
        chatService.recallMessage(messageId, C_USER_TYPE, appUserId);
        return CommonResult.success(null);
    }

    // -------------------------
    // 在线状态
    // -------------------------

    @Operation(summary = "查询用户是否在线")
    @GetMapping("/users/{userType}/{userId}/online")
    public CommonResult<Boolean> isOnline(@PathVariable Byte userType, @PathVariable Long userId) {
        return CommonResult.success(chatService.isOnline(userType, userId));
    }
}
