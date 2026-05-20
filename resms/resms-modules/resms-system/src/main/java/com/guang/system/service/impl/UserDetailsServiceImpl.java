package com.guang.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guang.system.entity.User;
import com.guang.system.mapper.MenuMapper;
import com.guang.system.mapper.RoleMapper;
import com.guang.system.mapper.UserMapper;
import com.guang.system.security.AdminUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

/**
 * Spring Security 用户加载服务实现
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final MenuMapper menuMapper;
    private final RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查询用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0));
        
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        // 2. 查询用户权限编码 (用于按钮级权限控制)
        List<String> permissions = menuMapper.selectMenusByUserId(user.getId())
                .stream()
                .map(menu -> menu.getMenuCode())
                .filter(code -> code != null && !code.isEmpty())
                .toList();

        List<com.guang.system.entity.Role> userRoles = roleMapper.selectRolesByUserId(user.getId());

        // 3. 查询用户角色编码 (用于 hasRole 控制)
        List<String> roleCodes = userRoles.stream()
                .map(role -> "ROLE_" + role.getRoleCode()) // Spring Security 默认角色前缀
                .toList();

        // 合并权限和角色
        List<String> authorities = Stream.concat(permissions.stream(), roleCodes.stream()).toList();

        // 计算最高数据权限：1=全部，2=本部门，3=本部门及子部门，4=仅本人
        Byte highestDataScope = 4; // 默认为本人
        if (userRoles != null && !userRoles.isEmpty()) {
            boolean hasAll = false;
            boolean hasDeptAndSub = false;
            boolean hasDept = false;
            for (com.guang.system.entity.Role r : userRoles) {
                if (r.getDataScope() != null) {
                    if (r.getDataScope() == 1) {
                        hasAll = true;
                    } else if (r.getDataScope() == 3) {
                        hasDeptAndSub = true;
                    } else if (r.getDataScope() == 2) {
                        hasDept = true;
                    }
                }
            }
            if (hasAll) {
                highestDataScope = 1;
            } else if (hasDeptAndSub) {
                highestDataScope = 3;
            } else if (hasDept) {
                highestDataScope = 2;
            }
        }
        
        return new AdminUserDetails(user, authorities, highestDataScope);
    }
}
