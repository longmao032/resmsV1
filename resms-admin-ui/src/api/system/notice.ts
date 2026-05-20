import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { NotificationVO, NotificationSaveDTO } from '@/types/system/notice'

/** 分页查询通知列表 */
export function listNotices(pageNum: number, pageSize: number, title?: string, noticeType?: number) {
  return request<Result<{
    records: NotificationVO[]
    total: number
    size: number
    current: number
  }>>({
    url: '/system/v1/notification/page',
    method: 'get',
    params: { pageNum, pageSize, title, noticeType }
  })
}

/** 发布通知 */
export function publishNotice(data: NotificationSaveDTO) {
  return request<Result<boolean>>({
    url: '/system/v1/notification/publish',
    method: 'post',
    data
  })
}

/** 撤回通知 */
export function withdrawNotice(id: number) {
  return request<Result<boolean>>({
    url: `/system/v1/notification/withdraw/${id}`,
    method: 'put'
  })
}

/** 编辑通知 */
export function updateNotice(id: number, data: NotificationSaveDTO) {
  return request<Result<boolean>>({
    url: `/system/v1/notification/update/${id}`,
    method: 'put',
    data
  })
}

/** 批量删除通知 */
export function deleteNotices(ids: number[]) {
  return request<Result<boolean>>({
    url: '/system/v1/notification/batch',
    method: 'delete',
    data: ids
  })
}
