package com.guang.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统用户表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("sys_user")
@Schema(name = "User", description = "系统用户表")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 登录用户名
     */
    @TableField("username")
    @Schema(description = "登录用户名")
    private String username;

    /**
     * 加密密码
     */
    @TableField("password")
    @Schema(description = "加密密码")
    private String password;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    @Schema(description = "真实姓名")
    private String realName;

    /**
     * 昵称
     */
    @TableField("nick_name")
    @Schema(description = "昵称")
    private String nickName;

    /**
     * 手机号
     */
    @TableField("phone")
    @Schema(description = "手机号")
    private String phone;

    /**
     * 邮箱
     */
    @TableField("email")
    @Schema(description = "邮箱")
    private String email;

    /**
     * 性别：0=未知，1=男，2=女
     */
    @TableField("sex")
    @Schema(description = "性别：0=未知，1=男，2=女")
    private Byte sex;

    /**
     * 头像URL
     */
    @TableField("avatar")
    @Schema(description = "头像URL")
    private String avatar;

    /**
     * 所属部门ID
     */
    @TableField("dept_id")
    @Schema(description = "所属部门ID")
    private Integer deptId;

    /**
     * 状态：0=禁用，1=正常
     */
    @TableField("status")
    @Schema(description = "状态：0=禁用，1=正常")
    private Byte status;

    /**
     * 是否删除：0=未删除，1=已删除
     */
    @TableField("is_deleted")
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @TableField("last_login_ip")
    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
