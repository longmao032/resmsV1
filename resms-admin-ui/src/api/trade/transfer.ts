import request from '@/utils/request'

// 查询过户信息
export function getTransfer(transactionId: number) {
  return request({
    url: `/system/trade/v1/transfers/${transactionId}`,
    method: 'get'
  })
}

// 创建过户记录
export function createTransfer(data: any) {
  return request({
    url: '/system/trade/v1/transfers',
    method: 'post',
    data
  })
}

// 编辑过户记录
export function updateTransfer(id: number, data: any) {
  return request({
    url: `/system/trade/v1/transfers/${id}`,
    method: 'put',
    data
  })
}

// 确认过户完成
export function completeTransfer(id: number) {
  return request({
    url: `/system/trade/v1/transfers/${id}/complete`,
    method: 'put'
  })
}

// 上传过户文件
export function addTransferDocument(data: any) {
  return request({
    url: '/system/trade/v1/transfers/document',
    method: 'post',
    data
  })
}

// 查询过户文件列表
export function listTransferDocuments(transferId: number) {
  return request({
    url: `/system/trade/v1/transfers/${transferId}/documents`,
    method: 'get'
  })
}
