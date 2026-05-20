import request from '@/utils/request'
import type { Result, PageResult } from '@/types/api'

/** 会话成员 */
export interface SessionMember {
  userType: number
  userId: number
  userName?: string
  userAvatar?: string
}

/** 会话 */
export interface ChatSession {
  id: number
  sessionType: number
  sessionName?: string
  lastMessageContent?: string
  lastMessageTime?: string
  unreadCount: number
  isTop: boolean
  members: SessionMember[]
}

/** 消息 */
export interface ChatMessage {
  id: number
  sessionId: number
  parentId?: number
  senderType: number
  senderId: number
  senderName: string
  senderAvatar?: string
  content?: string
  msgType: number
  fileUrl?: string
  fileSize?: number
  fileName?: string
  isRecalled: number
  createTime: string
}

/** 创建会话参数 */
export interface CreateSessionData {
  sessionType: number
  sessionName?: string
  members: { userType: number; userId: number }[]
}

/** 发送消息参数 */
export interface SendMessageData {
  sessionId: number
  parentId?: number
  msgType: number
  content?: string
  fileUrl?: string
  fileSize?: number
  fileName?: string
}

/** 查询我的会话列表 */
export function listSessions() {
  return request<Result<ChatSession[]>>({
    url: '/system/v1/chat/sessions',
    method: 'get'
  })
}

/** 创建或获取已有会话 */
export function createSession(data: CreateSessionData) {
  return request<Result<ChatSession>>({
    url: '/system/v1/chat/sessions',
    method: 'post',
    data
  })
}

/** 清零未读数 */
export function readSession(sessionId: number) {
  return request<Result<null>>({
    url: `/system/v1/chat/sessions/${sessionId}/read`,
    method: 'put'
  })
}

/** 分页拉取历史消息 */
export function listMessages(sessionId: number, pageNum = 1, pageSize = 30) {
  return request<Result<PageResult<ChatMessage>>>({
    url: `/system/v1/chat/sessions/${sessionId}/messages`,
    method: 'get',
    params: { pageNum, pageSize }
  })
}

/** 发送消息 */
export function sendMessage(data: SendMessageData) {
  return request<Result<ChatMessage>>({
    url: '/system/v1/chat/messages',
    method: 'post',
    data
  })
}

/** 撤回消息（2分钟内） */
export function recallMessage(messageId: number) {
  return request<Result<null>>({
    url: `/system/v1/chat/messages/${messageId}`,
    method: 'delete'
  })
}

/** 删除会话（当前用户） */
export function deleteSession(sessionId: number) {
  return request<Result<null>>({
    url: `/system/v1/chat/sessions/${sessionId}`,
    method: 'delete'
  })
}
