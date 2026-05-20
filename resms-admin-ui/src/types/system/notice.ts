/** 分页查询参数 */
export interface PageQuery {
  pageNum: number
  pageSize: number
}

/** 通知公告 (管理端列表VO) */
export interface NotificationVO {
  id: number
  title: string
  content?: string
  noticeType: number
  receiverType: number
  status: number
  withdrawStatus: number
  totalReceiverCount: number
  readCount: number
  senderName: string
  sendTime?: string
  createTime?: string
}

/** 发布通知参数 */
export interface NotificationSaveDTO {
  title: string
  content: string
  contentType?: number
  noticeType: number
  receiverType: number
  receiverIds?: number[]
  priority?: number
  expireTime?: string
  routerPath?: string
  businessType?: string
  businessId?: number
}

/** 用户收件箱VO */
export interface UserNotificationVO {
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

/** 通知列表查询参数 */
export interface NoticeQuery extends PageQuery {
  title?: string
  noticeType?: number
}
