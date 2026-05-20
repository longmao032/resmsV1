package com.guang.resms.controller;

import com.guang.common.result.CommonResult;
import com.guang.resms.websocket.WSConnectionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户在线状态查询接口
 */
@Tag(name = "通用接口", description = "用户在线状态查询")
@RestController
@RequestMapping("/api/v1/common/user")
@RequiredArgsConstructor
public class UserOnlineController {

    private final WSConnectionManager connectionManager;

    @Operation(summary = "查询用户是否在线")
    @GetMapping("/online")
    public CommonResult<Map<String, Object>> isOnline(@RequestParam(defaultValue = "SYS") String userType,
                                                      @RequestParam Long userId) {
        boolean online = connectionManager.isOnline(userType, userId);
        return CommonResult.success(Map.of("userId", userId, "userType", userType, "online", online));
    }
}
