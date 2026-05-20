import request from '@/utils/request'
import type { Result } from '@/types/api'

/**
 * 角色接口定义
 */
export interface Role {
  id?: number;
  roleName: string;
  roleCode: string;
  description?: string;
  sortOrder?: number;
  dataScope?: number;
  status?: number;
  menuIds?: number[];
  createTime?: string;
}

/**
 * 获取所有角色 (用于下拉框)
 */
export function listAllRoles() {
  return request<Result<any[]>>({
    url: '/system/v1/roles/all',
    method: 'get'
  })
}

/**
 * 分页查询角色
 */
export function listRole(query: any) {
  return request<Result<any>>({
    url: '/system/v1/roles/page',
    method: 'get',
    params: query
  })
}

/**
 * 获取角色关联的菜单ID
 */
export function getRoleMenuIds(roleId: number) {
  return request<Result<number[]>>({
    url: `/system/v1/roles/${roleId}/menuIds`,
    method: 'get'
  })
}

/**
 * 新增角色
 */
export function addRole(data: Role) {
  return request<Result<boolean>>({
    url: '/system/v1/roles',
    method: 'post',
    data
  })
}

/**
 * 修改角色
 */
export function updateRole(data: Role) {
  return request<Result<boolean>>({
    url: '/system/v1/roles',
    method: 'put',
    data
  })
}

/**
 * 删除角色
 */
export function delRole(id: number) {
  return request<Result<boolean>>({
    url: `/system/v1/roles/${id}`,
    method: 'delete'
  })
}

/**
 * 修改角色状态
 */
export function updateRoleStatus(id: number, status: number) {
  return request<Result<boolean>>({
    url: `/system/v1/roles/${id}/status`,
    method: 'put',
    params: { status }
  })
}
