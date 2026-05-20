import request from '@/utils/request'
import type { Result, PageResult } from '@/types/api'

/**
 * 项目接口定义
 */
export interface Project {
  id?: number;
  projectNo?: string;
  projectName: string;
  projectType: number;
  developer: string;
  propertyCompany: string;
  province?: string;
  city: string;
  district: string;
  address: string;
  totalHouseholds: number;
  propertyFee: number;
  plotRatio: number;
  greeningRate: number;
  tags?: string[];
  coverUrl?: string;
  longitude?: number;
  latitude?: number;
  status: number;
  commissionRate: number;
}

/**
 * 分页查询项目
 */
export function listProject(query: any) {
  return request<PageResult<Project>>({
    url: '/system/house/v1/projects/page',
    method: 'get',
    params: query
  })
}

/**
 * 新增/修改项目
 */
export function saveProject(data: Project) {
  return request<Result<boolean>>({
    url: '/system/house/v1/projects',
    method: 'post',
    data
  })
}

/**
 * 删除项目
 */
export function delProject(id: number) {
  return request<Result<boolean>>({
    url: `/system/house/v1/projects/${id}`,
    method: 'delete'
  })
}

/**
 * 获取项目详情
 */
export function getProject(id: number) {
  return request<Result<Project>>({
    url: `/system/house/v1/projects/${id}`,
    method: 'get'
  })
}

/**
 * 导出项目列表
 */
export function exportProject(query: any) {
  return request({
    url: '/system/house/v1/projects/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}

/**
 * 项目统计概览（顶部看板）
 */
export interface ProjectStats {
  totalCount: number
  totalTrend: number
  onSaleCount: number
  onSaleTrend: number
  pendingCount: number
  pendingTrend: number
  avgCommissionRate: number
  avgCommissionTrend: number
}

export function getProjectStats() {
  return request<Result<ProjectStats>>({
    url: '/system/house/v1/projects/statistics',
    method: 'get'
  })
}
