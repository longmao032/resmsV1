import request from '@/utils/request'

// 查询客户足迹列表
export function listHistory(query: any) {
  return request({
    url: '/system/trade/v1/histories',
    method: 'get',
    params: query
  })
}

// 记录交互轨迹 (手动录入)
export function addHistory(data: any) {
  return request({
    url: '/system/trade/v1/histories',
    method: 'post',
    data: data
  })
}
