package com.guang.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表返回对象
 */
@Data
@Schema(description = "用户列表返回对象")
public class UserVO {

    @Schema(description = "用户ID")
    private Integer id;

    @Schema(description = "用户名")
    private String username;

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

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "部门ID")
    private Integer deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "数据隔离范围：1=全部，2=本部门，3=本部门及下属，4=仅本人")
    private Byte dataScope;

    @Schema(description = "角色列表")
    private List<UserRoleInfo> roles;

    @Schema(description = "状态：0=禁用，1=正常")
    private Byte status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Data
    public static class UserRoleInfo {
        private Integer id;
        private String name;
        private String roleCode;
    }
}
