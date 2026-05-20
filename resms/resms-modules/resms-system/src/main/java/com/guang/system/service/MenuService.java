package com.guang.system.service;

import com.guang.system.entity.Menu;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 系统菜单表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
import com.guang.system.domain.vo.MenuVO;
import java.util.List;

/**
 * <p>
 * 系统菜单表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface MenuService extends IService<Menu> {

    /**
     * 根据用户ID获取菜单树
     */
    List<MenuVO> getMenuTreeByUserId(Integer userId);

    /**
     * 获取所有菜单列表（管理页面用）
     */
    List<MenuVO> getMenuList(Menu query);
}
