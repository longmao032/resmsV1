package com.guang.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.system.entity.Menu;

/**
 * <p>
 * 系统菜单表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * <p>
 * 系统菜单表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface MenuMapper extends BaseMapper<Menu> {

    /**
     * 根据用户ID获取菜单列表
     */
    List<Menu> selectMenusByUserId(@Param("userId") Integer userId);
}
