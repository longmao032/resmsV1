package com.guang.common.util;

import com.guang.common.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全服务工具类
 *
 * @author blackDuck
 */
public class SecurityUtils {

    /**
     * 获取用户ID
     **/
    public static Integer getUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    /**
     * 获取用户账户
     **/
    public static String getUsername() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUsername() : null;
    }

    /**
     * 获取用户昵称
     **/
    public static String getNickName() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getNickName() : null;
    }

    /**
     * 获取展示名称（优先级：昵称 > 真实姓名 > 用户名）
     */
    public static String getShowName() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) return null;
        if (loginUser.getNickName() != null && !loginUser.getNickName().isEmpty()) {
            return loginUser.getNickName();
        }
        if (loginUser.getRealName() != null && !loginUser.getRealName().isEmpty()) {
            return loginUser.getRealName();
        }
        return "用户" + loginUser.getUserId();
    }

    /**
     * 获取C端用户ID（Long类型）
     */
    public static Long getAppUserId() {
        LoginUser loginUser = getLoginUser();
        if (loginUser != null && loginUser.isAppUser()) {
            return loginUser.getUserId().longValue();
        }
        return null;
    }

    /**
     * 当前是否C端用户
     */
    public static boolean isAppUser() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null && loginUser.isAppUser();
    }

    /**
     * 当前是否B端管理用户
     */
    public static boolean isAdminUser() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null && loginUser.isAdmin();
    }

    /**
     * 获取用户
     **/
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }
}
