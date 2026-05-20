import request from '@/utils/request'
import type { Result } from '@/types/api'

/**
 * 用户接口定义
 */
export interface User {
  id?: number;
  username: string;
  password?: string;
  realName: string;
  phone: string;
  email?: string;
  sex?: number;
  avatar?: string;
  deptId?: number;
  deptName?: string;
  roleIds?: number[];
  roles?: any[];
  status?: number;
  remark?: string;
  lastLoginTime?: string;
  lastLoginIp?: string;
  createTime?: string;
}

/**
 * 获取销售人选列表（下拉框用，无需管理员权限，可选姓名筛选）
 */
export function getSalesOptions(realName?: string) {
  return request<Result<{ id: number; realName: string }[]>>({
    url: '/system/v1/users/sales-options',
    method: 'get',
    params: realName ? { realName } : undefined
  })
}

/**
 * 分页查询用户
 */
export function listUser(query: any) {
  return request<Result<any>>({
    url: '/system/v1/users/page',
    method: 'get',
    params: query
  })
}

/**
 * 新增用户
 */
export function addUser(data: User) {
  return request<Result<boolean>>({
    url: '/system/v1/users',
    method: 'post',
    data
  })
}

/**
 * 修改用户
 */
export function updateUser(data: User) {
  return request<Result<boolean>>({
    url: '/system/v1/users',
    method: 'put',
    data
  })
}

/**
 * 删除用户
 */
export function delUser(id: number) {
  return request<Result<boolean>>({
    url: `/system/v1/users/${id}`,
    method: 'delete'
  })
}

/**
 * 修改状态
 */
export function changeUserStatus(id: number, status: number) {
  return request<Result<boolean>>({
    url: `/system/v1/users/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 重置密码
 */
export function resetUserPwd(id: number, password: string) {
  return request<Result<boolean>>({
    url: `/system/v1/users/${id}/password`,
    method: 'put',
    params: { password }
  })
}

/**
 * 获取个人信息
 */
export function getProfile() {
  return request<Result<User>>({
    url: '/system/v1/users/profile',
    method: 'get'
  })
}

/**
 * 更新个人信息
 */
export function updateProfile(data: {
  realName?: string
  phone?: string
  email?: string
  sex?: number
  avatar?: string
}) {
  return request<Result<boolean>>({
    url: '/system/v1/users/profile',
    method: 'put',
    data
  })
}

/**
 * 获取指定部门下的用户列表（下拉框用）
 */
export function getUsersByDept(deptId: number) {
  return request<Result<{ id: number; realName: string }[]>>({
    url: '/system/v1/users/by-dept',
    method: 'get',
    params: { deptId }
  })
}

/**
 * 修改密码
 */
export function changePassword(data: {
  oldPassword: string
  newPassword: string
}) {
  return request<Result<boolean>>({
    url: '/system/v1/users/profile/password',
    method: 'put',
    data
  })
}
