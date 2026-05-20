package com.guang.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.system.entity.Role;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * <p>
 * 角色表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据用户ID获取角色列表
     */
    List<Role> selectRolesByUserId(@Param("userId") Integer userId);
}
