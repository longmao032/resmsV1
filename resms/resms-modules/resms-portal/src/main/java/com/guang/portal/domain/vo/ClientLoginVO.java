package com.guang.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "C端登录成功返回结果")
public class ClientLoginVO {
    @Schema(description = "令牌")
    private String token;
    @Schema(description = "令牌前缀")
    private String tokenHead;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "头像地址")
    private String avatarUrl;









}
