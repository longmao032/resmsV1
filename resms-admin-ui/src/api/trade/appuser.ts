import request from '@/utils/request'

// 查询C端用户列表
export function listAppUser(query: any) {
  return request({
    url: '/system/trade/v1/app-users',
    method: 'get',
    params: query
  })
}

// 修改C端用户状态
export function changeAppUserStatus(id: number | string, status: number) {
  return request({
    url: `/system/trade/v1/app-users/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// 删除C端用户
export function delAppUser(id: number | string) {
  return request({
    url: `/system/trade/v1/app-users/${id}`,
    method: 'delete'
  })
}

// 导出C端用户
export function exportAppUser(query: any) {
  return request({
    url: '/system/trade/v1/app-users/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}

// 获取C端用户统计数据
export function getAppUserStats() {
  return request({
    url: '/system/trade/v1/app-users/statistics',
    method: 'get'
  })
}
