import request from '@/utils/request'
import type { Result } from '@/types/api'

/**
 * 菜单对象结构
 */
export interface Menu {
  id?: number;
  parentId: number;
  menuName: string;
  menuCode: string;
  menuType: number; // 1: 目录, 2: 菜单, 3: 按钮
  path?: string;
  component?: string;
  icon?: string;
  sortOrder: number;
  visible?: number;
  status?: number;
  createTime?: string;
  children?: Menu[];
}

/**
 * 获取当前用户的动态菜单树
 */
export function getNav() {
  return request<Result<Menu[]>>({
    url: '/system/v1/menus/nav',
    method: 'get'
  })
}

/**
 * 查询菜单列表
 */
export function listMenu(query?: any) {
  return request<Result<Menu[]>>({
    url: '/system/v1/menus/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取菜单详情
 */
export function getMenu(id: number) {
  return request<Result<Menu>>({
    url: `/system/v1/menus/${id}`,
    method: 'get'
  })
}

/**
 * 新增菜单
 */
export function addMenu(data: Menu) {
  return request<Result<boolean>>({
    url: '/system/v1/menus',
    method: 'post',
    data
  })
}

/**
 * 修改菜单
 */
export function updateMenu(data: Menu) {
  return request<Result<boolean>>({
    url: '/system/v1/menus',
    method: 'put',
    data
  })
}

/**
 * 删除菜单
 */
export function deleteMenu(id: number) {
  return request<Result<boolean>>({
    url: `/system/v1/menus/${id}`,
    method: 'delete'
  })
}
