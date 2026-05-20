import request from '@/utils/request'
import type { Result } from '@/types/api'

/** 佣金记录 */
export interface Commission {
  id: number
  transactionId?: number
  salesId?: number
  salesName?: string
  financeName?: string
  transactionNo?: string
  commissionRate: number
  amount: number
  status: number
  calculateTime?: string
  issueTime?: string
  financeId?: number
  bankCardNo?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 分页查询佣金记录 */
export function listCommissions(params: {
  pageNum: number
  pageSize: number
  userId?: number
  status?: number
}) {
  return request<Result<{
    records: Commission[]
    total: number
    size: number
    current: number
  }>>({
    url: '/finance/v1/commissions',
    method: 'get',
    params
  })
}

/** 查询佣金详情 */
export function getCommission(id: number) {
  return request<Result<Commission>>({
    url: `/finance/v1/commissions/${id}`,
    method: 'get'
  })
}

/** 确认核算佣金（0→1） */
export function calculateCommission(id: number) {
  return request<Result<boolean>>({
    url: `/finance/v1/commissions/${id}/calculate`,
    method: 'put'
  })
}

/** 确认发放佣金（1→2） */
export function issueCommission(id: number) {
  return request<Result<boolean>>({
    url: `/finance/v1/commissions/${id}/issue`,
    method: 'put'
  })
}
