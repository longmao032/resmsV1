package com.guang.common.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录用户封装，用于缓存到 Redis
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginUser implements UserDetails {

    private Integer userId;
    private String username;
    private String nickName;
    private String realName;
    private String avatar;
    private String password;
    private List<String> permissions;
    /** 用户类型: "SYS" = B端管理用户, "APP" = C端App用户 */
    private String userType;
    /** 部门ID */
    private Integer deptId;
    /** 最高数据权限级别（1=全部，2=本部门，3=本部门及子部门，4=仅本人） */
    private Byte dataScope;

    public boolean isAppUser() {
        return "APP".equals(userType);
    }

    public boolean isAdmin() {
        return "SYS".equals(userType) || userType == null;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null) return List.of();
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
