package com.guang.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户保存参数
 */
@Data
@Schema(description = "用户保存参数")
public class UserSaveDTO {

    @Schema(description = "用户ID (更新时必填)")
    private Integer id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码 (新增时必填)")
    private String password;

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

    @Schema(description = "所属部门ID")
    private Integer deptId;

    @Schema(description = "状态：0=禁用，1=正常")
    private Byte status;

    @Schema(description = "角色ID列表")
    private List<Integer> roleIds;

    @Schema(description = "备注")
    private String remark;
}
