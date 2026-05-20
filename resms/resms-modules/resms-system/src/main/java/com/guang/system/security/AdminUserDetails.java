package com.guang.system.security;

import com.guang.system.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 用户详情封装
 */
public class AdminUserDetails implements UserDetails {

    private final User user;
    private final List<String> permissions;
    private final Byte highestDataScope;

    public AdminUserDetails(User user, List<String> permissions, Byte highestDataScope) {
        this.user = user;
        this.permissions = permissions;
        this.highestDataScope = highestDataScope;
    }

    public Byte getHighestDataScope() {
        return highestDataScope;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 返回当前用户的权限
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
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
        return user.getStatus() == 1;
    }

    public User getUser() {
        return user;
    }
}
