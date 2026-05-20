import request from '@/utils/request'
import type { Result } from '@/types/api'

/** 交易信息 (列表VO) */
export interface TransactionVO {
  id: number
  transactionNo: string
  dealPrice: number
  deposit: number
  downPayment: number
  loanAmount: number
  paymentType: number
  actualPaidAmount: number
  status: number
  createTime: string
  house: {
    id: number
    houseNo: string
    projectName: string
    layout: string
    area: number
    coverImage: string
  } | null
  customer: {
    id: number
    realName: string
    phone: string
  } | null
  sales: {
    id: number
    realName: string
  } | null
}

/** 分页查询交易列表 */
export function listTransactions(params: {
  pageNum?: number
  pageSize?: number
  houseId?: number
  transactionNo?: string
  customerId?: number
  salesId?: number
  status?: number
}) {
  return request<Result<{
    records: TransactionVO[]
    total: number
    size: number
    current: number
  }>>({
    url: '/system/trade/v1/transactions',
    method: 'get',
    params
  })
}
