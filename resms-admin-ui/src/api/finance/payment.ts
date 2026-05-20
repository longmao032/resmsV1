import request from '@/utils/request'

export interface PaymentSubmitForm {
  transactionId: number;
  paymentPlanId: number; // 关联的账单计划
  paymentType: number;   // 1=定金，2=首付款，3=尾款，4=中介费，5=贷款
  flowType: number;      // 1=收款，2=退款
  amount: number;
  paymentMethod: string; // 后端期望字符串描述
  paymentTime: string;   // 修正为 paymentTime
  proofUrl?: string;     // 凭证图片
  remark?: string;
  [key: string]: any;
}

export interface PaymentAuditForm {
  id: number;
  paymentStatus: number; // 1=审核通过，2=作废
  actualAmount: number;  // 财务实收金额
  remark?: string;       // 修正为 remark
}

// 分页查询支付流水
export function listPayment(query: any) {
  return request({
    url: '/finance/v1/payments',
    method: 'get',
    params: query
  })
}

// 提交支付流水 (经纪人)
export function submitPayment(data: PaymentSubmitForm | any) {
  return request({
    url: '/finance/v1/payments',
    method: 'post',
    data: data
  })
}

// 审核支付流水 (财务核销)
export function auditPayment(data: PaymentAuditForm | any) {
  return request({
    url: '/finance/v1/payments/audit',
    method: 'put',
    data: data
  })
}

// 查看流水详情
export function getPayment(id: number | string) {
  return request({
    url: `/finance/v1/payments/${id}`,
    method: 'get'
  })
}

// 作废已通过的流水
export function voidPayment(id: number) {
  return request({
    url: `/finance/v1/payments/${id}/void`,
    method: 'put'
  })
}

// 发起退款申请
export function applyRefund(data: any) {
  return request({
    url: '/finance/v1/payments/refund',
    method: 'post',
    data
  })
}

// 导出支付流水
export function exportPayment(query: any) {
  return request({
    url: '/finance/v1/payments/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}
