package com.guang.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.system.domain.vo.MenuVO;
import com.guang.system.entity.Menu;
import com.guang.system.mapper.MenuMapper;
import com.guang.system.service.MenuService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 系统菜单表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Override
    public List<MenuVO> getMenuTreeByUserId(Integer userId) {
        // 1. 获取用户所有的菜单列表
        List<Menu> allMenus;
        if (userId == 1) { // 假设 ID=1 是超级管理员
            allMenus = this.list(new LambdaQueryWrapper<Menu>()
                    .eq(Menu::getStatus, 1)
                    .eq(Menu::getIsDeleted, 0)
                    .orderByAsc(Menu::getSortOrder));
        } else {
            allMenus = baseMapper.selectMenusByUserId(userId);
        }

        // 2. 转换为 VO
        List<MenuVO> voList = allMenus.stream().map(menu -> {
            MenuVO vo = new MenuVO();
            BeanUtils.copyProperties(menu, vo);
            return vo;
        }).collect(Collectors.toList());

        // 3. 构建树形结构
        return buildMenuTree(voList, 0);
    }

    @Override
    public List<MenuVO> getMenuList(Menu query) {
        // 1. 查询所有菜单数据
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        if (query.getMenuName() != null && !query.getMenuName().isEmpty()) {
            wrapper.like(Menu::getMenuName, query.getMenuName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Menu::getStatus, query.getStatus());
        }
        wrapper.eq(Menu::getIsDeleted, 0);
        wrapper.orderByAsc(Menu::getSortOrder);
        
        List<Menu> list = this.list(wrapper);

        // 2. 转换为 VO
        List<MenuVO> voList = list.stream().map(menu -> {
            MenuVO vo = new MenuVO();
            BeanUtils.copyProperties(menu, vo);
            return vo;
        }).collect(Collectors.toList());

        // 3. 构建树形结构 (对于列表管理，如果是带条件的查询，可能不需要构建完整的树，或者根据父子关系重组)
        // 这里简单处理：如果没带搜索条件，构建完整树；如果带了搜索条件，返回扁平列表
        if ((query.getMenuName() == null || query.getMenuName().isEmpty()) && query.getStatus() == null) {
            return buildMenuTree(voList, 0);
        }
        return voList;
    }

    private List<MenuVO> buildMenuTree(List<MenuVO> menus, Integer parentId) {
        return menus.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .peek(menu -> menu.setChildren(buildMenuTree(menus, menu.getId())))
                .sorted((a, b) -> a.getSortOrder() - b.getSortOrder())
                .collect(Collectors.toList());
    }
}
