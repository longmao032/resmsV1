package com.guang.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 个人资料编辑参数
 */
@Data
@Schema(description = "个人资料编辑参数")
public class ProfileEditDTO {

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别：0=未知，1=男，2=女")
    private Byte sex;

    @Schema(description = "头像URL")
    private String avatar;
}
