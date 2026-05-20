import request from '@/utils/request'

// 查询交易订单列表
export function listOrder(query: any) {
  return request({
    url: '/system/trade/v1/transactions',
    method: 'get',
    params: query
  })
}

// 查询交易详情
export function getOrder(id: number | string) {
  return request({
    url: `/system/trade/v1/transactions/${id}`,
    method: 'get'
  })
}

// 交易表单接口定义
export interface PaymentPlanDTO {
  payName: string;
  receivableAmount: number;
  dueDate: string; // YYYY-MM-DD HH:mm:ss
}

export interface OrderForm {
  houseId: number;
  customerId: number;
  salesId: number;
  dealPrice: number;
  deposit: number;
  paymentType: number; // 1=一次性付款，2=分期付款，3=按揭贷款，4=租房
  paymentPlanList?: PaymentPlanDTO[];
  downPayment?: number;
  loanAmount?: number;
  nextPaymentTime?: string;
  [key: string]: any;
}

// 创建交易订单
export function addOrder(data: OrderForm | any) {
  return request({
    url: '/system/trade/v1/transactions',
    method: 'post',
    data: data
  })
}

// 获取某笔交易下所有账单计划
export function reqGetPaymentPlansByTransId(transactionId: number | string) {
  return request({
    url: `/system/trade/v1/transactions/${transactionId}/payment-plans`,
    method: 'get'
  })
}

// 更新交易状态
export function updateOrderStatus(data: any) {
  return request({
    url: '/system/trade/v1/transactions/status',
    method: 'put',
    data: data
  })
}

// 导出交易订单
export function exportOrder(query: any) {
  return request({
    url: '/system/trade/v1/transactions/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}

// 取消交易并退款
export function cancelOrderWithRefund(data: { id: number, reason: string }) {
  return request({
    url: '/system/trade/v1/transactions/cancel-with-refund',
    method: 'put',
    data
  })
}
