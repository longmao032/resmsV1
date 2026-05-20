import request from '@/utils/request'

export interface HouseItem {
  houseId: string | null
  projectName: string | null
  district: string | null
  price: string | null
  layout: string | null
  area: string | null
  coverUrl: string | null
  sellingPoint: string | null
}

export interface ChatBlock {
  type: 'text' | 'house_card' | 'house_cards'
  content?: string
  houseIds?: string[]
}

export interface AiChatRequest {
  message: string
  sessionId?: string
  city?: string
}

export interface AiChatResponse {
  reply: string
  sessionId: string
  recommendations: HouseItem[]
  followUp?: string | null
  blocks?: ChatBlock[]
}

/** 向 AI 助手发送消息 */
export function sendAiMessageApi(data: AiChatRequest): Promise<AiChatResponse> {
  return request.post('/api/portal/v1/ai/chat', data, { timeout: 120_000 })
}
