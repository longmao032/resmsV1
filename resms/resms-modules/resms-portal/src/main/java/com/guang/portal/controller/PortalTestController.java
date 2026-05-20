package com.guang.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 门户模块测试控制器
 *
 * @author antigravity
 */
@Tag(name = "门户基础接口", description = "用于门户网站的基础接口")
@RestController
@RequestMapping("/api/portal/v1")
public class PortalTestController {

    @Operation(summary = "模块连接测试")
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "resms-portal 模块运行正常");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
