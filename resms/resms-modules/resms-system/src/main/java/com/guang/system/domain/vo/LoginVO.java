package com.guang.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录成功返回结果
 */
@Data
@Builder
@Schema(description = "登录成功返回结果")
public class LoginVO {
    @Schema(description = "令牌")
    private String token;
    @Schema(description = "令牌前缀")
    private String tokenHead;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "用户信息")
    private UserVO user;
}
