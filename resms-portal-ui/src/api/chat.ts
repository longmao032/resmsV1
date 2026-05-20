import request from '@/utils/request'

// ---- Types (match backend entities exactly) ----

export interface ChatMessage {
  id: number
  sessionId: number
  parentId: number | null
  senderType: number
  senderId: number
  senderName: string
  senderAvatar: string | null
  content: string
  msgType: number
  fileUrl: string | null
  fileSize: number | null
  fileName: string | null
  isRecalled: number
  createTime: string
}

export interface SessionMember {
  userType: number
  userId: number
  userName: string
  userAvatar: string | null
}

export interface SessionVO {
  id: number
  sessionType: number
  sessionName: string | null
  lastMessageContent: string | null
  lastMessageTime: string | null
  unreadCount: number
  isTop: boolean
  members: SessionMember[]
}

export interface CreateSessionDTO {
  sessionType: number
  sessionName?: string
  members: { userType: number; userId: number }[]
}

export interface SendMessageDTO {
  sessionId: number
  parentId?: number
  msgType: number
  content?: string
  fileUrl?: string
  fileSize?: number
  fileName?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ---- API Functions ----

/** 创建或获取会话 */
export function createSessionApi(dto: CreateSessionDTO): Promise<SessionVO> {
  return request.post('/api/portal/v1/chat/sessions', dto)
}

/** 获取我的会话列表 */
export function listSessionsApi(): Promise<SessionVO[]> {
  return request.get('/api/portal/v1/chat/sessions')
}

/** 分页拉取历史消息 */
export function pageMessagesApi(sessionId: number, pageNum = 1, pageSize = 30): Promise<PageResult<ChatMessage>> {
  return request.get(`/api/portal/v1/chat/sessions/${sessionId}/messages`, {
    params: { pageNum, pageSize }
  })
}

/** 发送消息 */
export function sendMessageApi(dto: SendMessageDTO): Promise<ChatMessage> {
  return request.post('/api/portal/v1/chat/messages', dto)
}

/** 标记会话已读 */
export function readSessionApi(sessionId: number): Promise<void> {
  return request.put(`/api/portal/v1/chat/sessions/${sessionId}/read`)
}

/** 删除/隐藏会话 */
export function deleteSessionApi(sessionId: number): Promise<void> {
  return request.delete(`/api/portal/v1/chat/sessions/${sessionId}`)
}

/** 查询用户是否在线 */
export function checkOnlineApi(userType: number, userId: number): Promise<boolean> {
  return request.get(`/api/portal/v1/chat/users/${userType}/${userId}/online`)
}
