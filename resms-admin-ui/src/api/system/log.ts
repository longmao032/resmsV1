import request from '@/utils/request'
import type { Result } from '@/types/api'

/**
 * 操作日志对象
 */
export interface OperationLog {
  id: number;
  module: string;
  businessType: string;
  operationType: string;
  operationDesc: string;
  requestMethod: string;
  requestUrl: string;
  requestParams: string;
  responseResult: string;
  userName: string;
  ipAddress: string;
  userAgent: string;
  executeTime: number;
  status: number;
  errorMsg?: string;
  riskLevel: number;
  operationTime: string;
}

/**
 * 分页查询操作日志
 */
export function listLog(query: any) {
  return request<Result<any>>({
    url: '/system/v1/logs/page',
    method: 'get',
    params: query
  })
}
