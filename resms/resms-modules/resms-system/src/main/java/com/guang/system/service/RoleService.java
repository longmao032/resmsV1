package com.guang.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guang.system.domain.dto.RoleQueryDTO;
import com.guang.system.domain.dto.RoleSaveDTO;
import com.guang.system.entity.Role;

import java.util.List;

/**
 * <p>
 * 角色表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface RoleService extends IService<Role> {

    /**
     * 分页查询角色
     */
    Page<Role> pageRoles(RoleQueryDTO queryDTO);

    /**
     * 保存角色（含权限关联）
     */
    Boolean saveRole(RoleSaveDTO saveDTO);

    /**
     * 删除角色
     */
    Boolean deleteRole(Integer id);

    /**
     * 修改角色状态
     */
    Boolean updateStatus(Integer id, Byte status);

    /**
     * 获取角色关联的菜单ID
     */
    List<Integer> getRoleMenuIds(Integer roleId);
}
