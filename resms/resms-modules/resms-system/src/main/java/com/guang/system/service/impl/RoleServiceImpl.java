package com.guang.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.exception.ApiException;
import com.guang.system.domain.dto.RoleQueryDTO;
import com.guang.system.domain.dto.RoleSaveDTO;
import com.guang.system.entity.Role;
import com.guang.system.entity.RoleMenu;
import com.guang.system.mapper.RoleMapper;
import com.guang.system.service.RoleMenuService;
import com.guang.system.service.RoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMenuService roleMenuService;


    @Override
    public Page<Role> pageRoles(RoleQueryDTO queryDTO) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getRoleName()), Role::getRoleName, queryDTO.getRoleName())
                .like(StrUtil.isNotBlank(queryDTO.getRoleCode()), Role::getRoleCode, queryDTO.getRoleCode())
                .eq(queryDTO.getStatus() != null, Role::getStatus, queryDTO.getStatus())
                .eq(Role::getIsDeleted, 0)
                .orderByAsc(Role::getId);
        return this.page(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveRole(RoleSaveDTO saveDTO) {
        Role role = BeanUtil.copyProperties(saveDTO, Role.class);
        boolean isUpdate = role.getId() != null;
        
        // 1. 保存角色基本信息
        if (!this.saveOrUpdate(role)) {
            throw new ApiException("保存角色失败");
        }

        Integer roleId = role.getId();

        // 2. 处理关联菜单
        if (saveDTO.getMenuIds() != null) {
            if (isUpdate) {
                roleMenuService.remove(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
            }
            if (CollUtil.isNotEmpty(saveDTO.getMenuIds())) {
                List<RoleMenu> roleMenus = saveDTO.getMenuIds().stream().map(menuId -> {
                    RoleMenu rm = new RoleMenu();
                    rm.setRoleId(roleId);
                    rm.setMenuId(menuId);
                    return rm;
                }).collect(Collectors.toList());
                roleMenuService.saveBatch(roleMenus);
            }
        }


        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRole(Integer id) {
        Role role = new Role();
        role.setId(id);
        role.setIsDeleted((byte) 1);
        if (this.updateById(role)) {
            // 级联删除关联关系（物理删除关联，逻辑删除主体）
            roleMenuService.remove(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
            return true;
        }
        return false;
    }

    @Override
    public Boolean updateStatus(Integer id, Byte status) {
        Role role = new Role();
        role.setId(id);
        role.setStatus(status);
        return this.updateById(role);
    }

    @Override
    public List<Integer> getRoleMenuIds(Integer roleId) {
        return roleMenuService.list(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId))
                .stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
    }
}
