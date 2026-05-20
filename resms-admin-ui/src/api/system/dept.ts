import request from '@/utils/request'
import type { Result } from '@/types/api'

/**
 * 部门接口定义
 */
export interface Dept {
  id?: number;
  parentId: number;
  deptName: string;
  deptCode: string;
  leaderId?: number;
  sortOrder?: number;
  status?: number;
  deptType?: number;
  email?: string;
  phone?: string;
  createTime?: string;
  children?: Dept[];
}

/**
 * 获取部门树
 */
export function listDeptTree(query?: any) {
  return request<Result<Dept[]>>({
    url: '/system/v1/depts/tree',
    method: 'get',
    params: query
  })
}

/**
 * 新增部门
 */
export function addDept(data: Dept) {
  return request<Result<boolean>>({
    url: '/system/v1/depts',
    method: 'post',
    data
  })
}

/**
 * 修改部门
 */
export function updateDept(data: Dept) {
  return request<Result<boolean>>({
    url: '/system/v1/depts',
    method: 'put',
    data
  })
}

/**
 * 删除部门
 */
export function delDept(id: number) {
  return request<Result<boolean>>({
    url: `/system/v1/depts/${id}`,
    method: 'delete'
  })
}
