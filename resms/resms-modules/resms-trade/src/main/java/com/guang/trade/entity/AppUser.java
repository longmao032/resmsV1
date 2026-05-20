package com.guang.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * C端移动端用户账号表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-10
 */
@Getter
@Setter
@ToString
@TableName("tb_app_user")
@Schema(name = "AppUser", description = "C端移动端用户账号表")
public class AppUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * C端用户ID
     */
    @Schema(description = "C端用户ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 手机号（核心登录凭证）
     */
    @TableField("phone")
    @Schema(description = "手机号")
    private String phone;

    /**
     * 密码
     */
    @TableField("password")
    @Schema(description = "密码")
    private String password;

    /**
     * 微信OpenID
     */
    @TableField("wechat_openid")
    @Schema(description = "微信OpenID")
    private String wechatOpenid;

    /**
     * 微信UnionID
     */
    @TableField("union_id")
    @Schema(description = "微信UnionID")
    private String unionId;

    /**
     * 用户昵称
     */
    @TableField("nickname")
    @Schema(description = "用户昵称")
    private String nickname;

    @TableField("gender")
    @Schema(description = "性别：0=未知，1=男，2=女")
    private Byte gender;

    @TableField("email")
    @Schema(description = "邮箱")
    private String email;
    /**
     * 头像地址
     */
    @TableField("avatar_url")
    @Schema(description = "头像地址")
    private String avatarUrl;

    /**
     * 状态：0=封禁，1=正常
     */
    @TableField("status")
    @Schema(description = "状态：0=封禁，1=正常")
    private Byte status;

    /**
     * 是否删除：0=未删除，1=已删除
     */
    @TableField("is_deleted")
    @TableLogic
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
