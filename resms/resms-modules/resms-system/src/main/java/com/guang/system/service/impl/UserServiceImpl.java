package com.guang.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.common.exception.ApiException;
import com.guang.common.security.LoginUser;
import com.guang.common.util.SecurityUtils;
import com.guang.system.domain.dto.PasswordChangeDTO;
import com.guang.system.domain.dto.ProfileEditDTO;
import com.guang.system.domain.dto.UserQueryDTO;
import com.guang.system.domain.dto.UserSaveDTO;
import com.guang.system.domain.vo.UserVO;
import com.guang.system.entity.Dept;
import com.guang.system.entity.Role;
import com.guang.system.entity.User;
import com.guang.system.entity.UserRole;
import com.guang.system.mapper.UserMapper;
import com.guang.system.service.DeptService;
import com.guang.system.service.RoleService;
import com.guang.system.service.UserRoleService;
import com.guang.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 系统用户表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;
    private final DeptService deptService;
    private final RoleService roleService;

    @Override
    public Page<UserVO> pageUsers(UserQueryDTO queryDTO) {
        // 非管理员强制限定查询部门，防止通过篡改 deptId 越权查看其他部门成员
        List<Integer> allowedDeptIds = null;
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null && !loginUser.isAppUser()) {
            Byte scope = loginUser.getDataScope();
            Integer userDeptId = loginUser.getDeptId();
            if (scope != null && scope != 1 && userDeptId != null) {
                if (scope == 3) {
                    // scope=3 本部门及子部门：收集子部门 ID
                    allowedDeptIds = new java.util.ArrayList<>();
                    allowedDeptIds.add(userDeptId);
                    // 子部门：ancestors 包含 当前用户deptId
                    List<Dept> subs = deptService.list(new LambdaQueryWrapper<Dept>()
                            .apply("FIND_IN_SET({0}, ancestors)", userDeptId)
                            .eq(Dept::getIsDeleted, 0));
                    for (Dept sub : subs) {
                        allowedDeptIds.add(sub.getId());
                    }
                } else {
                    // scope=2/4：强制仅本部门
                    allowedDeptIds = List.of(userDeptId);
                }
            }
        }

        Page<User> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getUsername()), User::getUsername, queryDTO.getUsername())
               .like(StringUtils.hasText(queryDTO.getPhone()), User::getPhone, queryDTO.getPhone())
               .like(StringUtils.hasText(queryDTO.getRealName()), User::getRealName, queryDTO.getRealName())
               .eq(queryDTO.getStatus() != null, User::getStatus, queryDTO.getStatus());
        if (allowedDeptIds != null) {
            wrapper.in(User::getDeptId, allowedDeptIds);
        } else if (queryDTO.getDeptId() != null) {
            wrapper.eq(User::getDeptId, queryDTO.getDeptId());
        }
        wrapper.eq(User::getIsDeleted, 0)
               .orderByDesc(User::getCreateTime);
        
        Page<User> userPage = this.page(page, wrapper);
        Page<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        
        if (CollectionUtils.isEmpty(userPage.getRecords())) {
            return voPage;
        }

        // 1. 获取所有涉及的部门信息
        List<Integer> deptIds = userPage.getRecords().stream()
                .map(User::getDeptId)
                .filter(id -> id != null && id != 0)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, String> deptMap = deptIds.isEmpty() ? Map.of() :
                deptService.listByIds(deptIds).stream().collect(Collectors.toMap(Dept::getId, Dept::getDeptName));

        // 2. 获取所有涉及的用户角色信息
        List<Integer> userIds = userPage.getRecords().stream().map(User::getId).collect(Collectors.toList());
        List<UserRole> userRoles = userRoleService.list(new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds));
        
        Map<Integer, List<Integer>> userRoleIdsMap = userRoles.stream()
                .collect(Collectors.groupingBy(UserRole::getUserId, 
                        Collectors.mapping(UserRole::getRoleId, Collectors.toList())));

        List<Integer> allRoleIds = userRoles.stream().map(UserRole::getRoleId).distinct().collect(Collectors.toList());
        Map<Integer, Role> roleMap = allRoleIds.isEmpty() ? Map.of() :
                roleService.listByIds(allRoleIds).stream().collect(Collectors.toMap(Role::getId, r -> r));

        // 3. 组装 VO
        List<UserVO> voList = userPage.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            vo.setDeptName(deptMap.get(user.getDeptId()));
            
            List<Integer> roleIds = userRoleIdsMap.getOrDefault(user.getId(), List.of());
            List<UserVO.UserRoleInfo> roleInfos = roleIds.stream()
                    .map(roleMap::get)
                    .filter(r -> r != null)
                    .map(r -> {
                        UserVO.UserRoleInfo info = new UserVO.UserRoleInfo();
                        info.setId(r.getId());
                        info.setName(r.getRoleName());
                        info.setRoleCode(r.getRoleCode());
                        return info;
                    }).collect(Collectors.toList());
            vo.setRoles(roleInfos);
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveUser(UserSaveDTO saveDTO) {
        User user = new User();
        BeanUtils.copyProperties(saveDTO, user);

        boolean isUpdate = saveDTO.getId() != null;
        if (isUpdate) {
            user.setPassword(null);
        } else {
            user.setPassword(passwordEncoder.encode(saveDTO.getPassword()));
            user.setIsDeleted((byte) 0);
            if (user.getStatus() == null) {
                user.setStatus((byte) 1);
            }
        }

        boolean success = this.saveOrUpdate(user);

        if (success) {
            List<Integer> roleIds = saveDTO.getRoleIds();
            if (roleIds == null) {
                roleIds = new ArrayList<>();
            } else {
                roleIds = new ArrayList<>(roleIds); // 转换为可变列表
            }
            
            // 新增用户强制添加默认角色 ID=6
            if (!isUpdate && !roleIds.contains(6)) {
                roleIds.add(6);
            }

            if (isUpdate) {
                userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
            }
            
            if (!roleIds.isEmpty()) {
                List<UserRole> userRoles = roleIds.stream().map(roleId -> {
                    UserRole ur = new UserRole();
                    ur.setUserId(user.getId());
                    ur.setRoleId(roleId);
                    return ur;
                }).collect(Collectors.toList());
                userRoleService.saveBatch(userRoles);
            }
        }

        return success;
    }

    @Override
    public boolean deleteUser(Integer id) {
        User user = new User();
        user.setId(id);
        user.setIsDeleted((byte) 1);
        return this.updateById(user);
    }

    @Override
    public boolean updateStatus(Integer id, Byte status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return this.updateById(user);
    }

    @Override
    public boolean resetPassword(Integer id, String password) {
        User user = new User();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(password));
        return this.updateById(user);
    }

    @Override
    public UserVO getProfile(Integer userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new ApiException("用户不存在");
        }

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);

        // 部门
        if (user.getDeptId() != null && user.getDeptId() > 0) {
            Dept dept = deptService.getById(user.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }

        // 角色
        List<UserRole> userRoles = userRoleService.list(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        List<Integer> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (!roleIds.isEmpty()) {
            List<Role> roles = roleService.listByIds(roleIds);
            List<UserVO.UserRoleInfo> roleInfos = roles.stream().map(r -> {
                UserVO.UserRoleInfo info = new UserVO.UserRoleInfo();
                info.setId(r.getId());
                info.setName(r.getRoleName());
                info.setRoleCode(r.getRoleCode());
                return info;
            }).collect(Collectors.toList());
            vo.setRoles(roleInfos);
        }

        return vo;
    }

    @Override
    public boolean updateProfile(Integer userId, ProfileEditDTO editDTO) {
        User user = new User();
        user.setId(userId);
        user.setRealName(editDTO.getRealName());
        user.setNickName(editDTO.getNickName());
        user.setPhone(editDTO.getPhone());
        user.setEmail(editDTO.getEmail());
        user.setSex(editDTO.getSex());
        user.setAvatar(editDTO.getAvatar());
        return this.updateById(user);
    }

    @Override
    public boolean changePassword(Integer userId, PasswordChangeDTO dto) {
        User user = this.getById(userId);
        if (user == null) {
            throw new ApiException("用户不存在");
        }
        // 校验原密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new ApiException("原密码不正确");
        }
        // 更新新密码
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        return this.updateById(update);
    }

    @Override
    public java.util.List<com.guang.system.domain.vo.SalesOptionVO> listSalesOptions(String realName) {
        return baseMapper.selectSalesOptions(realName);
    }

    @Override
    public java.util.List<com.guang.system.domain.vo.SalesOptionVO> listByDept(Integer deptId) {
        return baseMapper.selectByDept(deptId);
    }
}
