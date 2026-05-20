import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { UserNotificationVO } from '@/types/system/notice'

/** 分页查询我的通知列表 */
export function myNotifications(pageNum: number, pageSize: number, isRead?: number) {
  return request<Result<{
    records: UserNotificationVO[]
    total: number
    size: number
    current: number
  }>>({
    url: '/system/v1/user-notification/my-page',
    method: 'get',
    params: { pageNum, pageSize, isRead }
  })
}

/** 标记单条通知为已读 */
export function markNotificationRead(id: number) {
  return request<Result<boolean>>({
    url: `/system/v1/user-notification/read/${id}`,
    method: 'put'
  })
}

/** 全部标记为已读 */
export function markAllNotificationsRead() {
  return request<Result<boolean>>({
    url: '/system/v1/user-notification/read-all',
    method: 'put'
  })
}

/** 获取未读通知数量 */
export function getUnreadCount() {
  return request<Result<number>>({
    url: '/system/v1/user-notification/unread-count',
    method: 'get'
  })
}
