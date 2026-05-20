import request from '@/utils/request'

// 查询客户列表
export function listCustomer(query: any) {
  return request({
    url: '/system/trade/v1/customers',
    method: 'get',
    params: query
  })
}

// 获取客户详情
export function getCustomer(id: number | string) {
  return request({
    url: `/system/trade/v1/customers/${id}`,
    method: 'get'
  })
}

// 保存客户
export function saveCustomer(data: any) {
  return request({
    url: '/system/trade/v1/customers',
    method: 'post',
    data: data
  })
}

// 删除客户
export function delCustomer(id: number | string) {
  return request({
    url: `/system/trade/v1/customers/${id}`,
    method: 'delete'
  })
}

// 领取客户（公海池）
export function claimCustomer(id: number | string) {
  return request({
    url: `/system/trade/v1/customers/${id}/claim`,
    method: 'post'
  })
}

// 获取明文手机号
export function getCustomerPhone(id: number | string) {
  return request({
    url: `/system/trade/v1/customers/${id}/phone`,
    method: 'get'
  })
}

// 获取明文身份证号
export function getCustomerIdCard(id: number | string) {
  return request({
    url: `/system/trade/v1/customers/${id}/id-card`,
    method: 'get'
  })
}

// 查询客户跟进记录列表
export function listFollowUp(customerId: number | string) {
  return request({
    url: `/system/trade/v1/customers/${customerId}/follow-ups`,
    method: 'get'
  })
}

// 保存跟进记录
export function saveFollowUp(customerId: number | string, data: any) {
  return request({
    url: `/system/trade/v1/customers/${customerId}/follow-ups`,
    method: 'post',
    data: data
  })
}

// 更新跟进记录
export function updateFollowUp(id: number | string, data: any) {
  return request({
    url: `/system/trade/v1/follow-ups/${id}`,
    method: 'put',
    data
  })
}

// ========== 预约带看 ==========

// 创建预约
export function createAppointment(customerId: number | string, data: any) {
  return request({
    url: `/system/trade/v1/customers/${customerId}/appointments`,
    method: 'post',
    data
  })
}

// 确认完成预约
export function completeAppointment(id: number | string, data: any) {
  return request({
    url: `/system/trade/v1/appointments/${id}/complete`,
    method: 'put',
    data
  })
}

// 取消预约
export function cancelAppointment(id: number | string, data: any) {
  return request({
    url: `/system/trade/v1/appointments/${id}/cancel`,
    method: 'put',
    data
  })
}

// 查询客户预约列表
export function listAppointments(customerId: number | string) {
  return request({
    url: `/system/trade/v1/customers/${customerId}/appointments`,
    method: 'get'
  })
}

// ========== 客户统计 ==========

export function getCustomerStats(customerId: number | string) {
  return request({
    url: `/system/trade/v1/customers/${customerId}/stats`,
    method: 'get'
  })
}
