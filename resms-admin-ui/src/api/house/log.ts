import request from '@/utils/request'
import type { PageResult } from '@/types/api'

/**
 * 房源状态日志定义
 */
export interface HouseStatusLog {
  id: number;
  houseId: number;
  houseNo: string;
  projectName: string;
  fromStatus: number | null;
  toStatus: number;
  changeReason: string;
  operatorId: number;
  operatorName: string;
  ipAddress: string;
  createTime: string;
}

/**
 * 分页查询房源状态日志
 */
export function listStatusLog(query: any) {
  return request<PageResult<HouseStatusLog>>({
    url: '/system/house/v1/status-logs/page',
    method: 'get',
    params: query
  })
}

/**
 * 项目变更日志定义
 */
export interface ProjectChangeLog {
  id: number;
  projectId: number;
  fieldLabel: string;
  oldValue: string | null;
  newValue: string;
  operatorName: string;
  ipAddress: string;
  createTime: string;
}

/**
 * 分页查询项目变更日志
 */
export function listProjectLog(query: any) {
  return request<PageResult<ProjectChangeLog>>({
    url: '/system/house/v1/projects/logs/page',
    method: 'get',
    params: query
  })
}
