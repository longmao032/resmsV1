package com.guang.portal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@Schema(description = "C端用户注册参数")
public class ClientRegisterDTO {
    @NotEmpty(message = "手机号不能为空")
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotEmpty(message = "密码不能为空")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatarUrl;
}
