import request from '@/utils/request'

export interface NotificationItem {
  id: number
  notificationId: number
  title: string
  content: string
  noticeType: number
  isRead: number
  sendTime: string
  senderName: string
  routerPath?: string
}

export function myNotifications(pageNum: number, pageSize: number, isRead?: number): Promise<{ records: NotificationItem[]; total: number; current: number; size: number }> {
  return request({
    url: '/api/portal/v1/notification/page',
    method: 'get',
    params: { pageNum, pageSize, isRead }
  })
}

export function markNotificationRead(id: number): Promise<boolean> {
  return request({
    url: `/api/portal/v1/notification/read/${id}`,
    method: 'put'
  })
}

export function markAllNotificationsRead(): Promise<boolean> {
  return request({
    url: '/api/portal/v1/notification/read-all',
    method: 'put'
  })
}

export function getUnreadCount(): Promise<number> {
  return request({
    url: '/api/portal/v1/notification/unread-count',
    method: 'get'
  })
}
