<script setup lang="ts">
import { ref, watch, nextTick, onBeforeUnmount, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  pageMessagesApi,
  sendMessageApi,
  readSessionApi,
  checkOnlineApi,
  type ChatMessage,
} from '@/api/chat'
import { wsService } from '@/utils/WebSocketService'
import { uploadFileApi } from '@/api/common'
import { marked } from 'marked'

const props = defineProps<{
  visible: boolean
  sessionId: number | null
  targetName: string
  targetUserId?: number
  houseContext?: {
    id: number
    projectName: string
    coverUrl?: string
    layout: string
    area: number
    price: number | string
    priceUnitText: string
  } | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const authStore = useAuthStore()

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const loading = ref(false)
const sending = ref(false)
const listRef = ref<HTMLElement | null>(null)

// Current user ID
const currentUserId = computed(() => authStore.userInfo?.userId ?? 0)

// Online status
const isOnline = ref(false)
let onlineTimer: ReturnType<typeof setInterval> | null = null

// WebSocket unsubscribe function
let unsubWs: (() => void) | null = null

// Format time for message groups
function formatMsgTime(time: string): string {
  const d = new Date(time)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  if (isToday) return `${hh}:${mm}`
  const M = d.getMonth() + 1
  const D = d.getDate()
  return `${M}/${D} ${hh}:${mm}`
}

// Check if we should show a time separator between two messages
function shouldShowTime(prev: ChatMessage | null | undefined, curr: ChatMessage): boolean {
  if (!prev) return true
  const gap = new Date(curr.createTime).getTime() - new Date(prev.createTime).getTime()
  return gap > 5 * 60 * 1000 // 5 minutes
}

// Load messages
async function loadMessages() {
  if (!props.sessionId) return
  loading.value = true
  try {
    const res = await pageMessagesApi(props.sessionId, 1, 50)
    // API returns in desc order, reverse to show oldest first
    messages.value = res.records.slice().reverse()
    await nextTick()
    scrollToBottom()
  } catch (e) {
    console.error('加载消息失败', e)
  } finally {
    loading.value = false
  }
}

// Send message
async function handleSend() {
  const text = inputText.value.trim()
  if (!text || !props.sessionId || sending.value) return

  sending.value = true
  try {
    const msg = await sendMessageApi({
      sessionId: props.sessionId,
      msgType: 1,
      content: text,
    })
    messages.value.push(msg)
    inputText.value = ''
    await nextTick()
    scrollToBottom()
  } catch (e) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

// Check target user online status
async function checkOnline() {
  if (!props.targetUserId) return
  try {
    isOnline.value = await checkOnlineApi(1, props.targetUserId)
  } catch {
    // ignore
  }
}

function startOnlinePolling() {
  stopOnlinePolling()
  checkOnline()
  onlineTimer = setInterval(checkOnline, 15000) // every 15s
}

function stopOnlinePolling() {
  if (onlineTimer) {
    clearInterval(onlineTimer)
    onlineTimer = null
  }
}

// 确保 WS 已连接（应用启动时或首次打开对话框时调用一次）
function ensureWsConnected() {
  wsService.connect()
}

// Subscribe to WS MESSAGE events for the current session
function subscribeWs() {
  // 先取消旧订阅，再注册新的，WS 连接本身由 ensureWsConnected 幂等维护
  if (unsubWs) {
    unsubWs()
    unsubWs = null
  }
  ensureWsConnected()
  unsubWs = wsService.on('MESSAGE', async (payload: ChatMessage) => {
    // Only handle messages for the current session
    if (payload.sessionId !== props.sessionId) return
    // Skip duplicates
    if (messages.value.some(m => m.id === payload.id)) return
    messages.value.push(payload)
    await nextTick()
    scrollToBottom()
  })
}

function unsubscribeWs() {
  if (unsubWs) {
    unsubWs()
    unsubWs = null
  }
  stopOnlinePolling()
}

function scrollToBottom() {
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

// Handle enter key
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

// Close dialog
function handleClose() {
  emit('update:visible', false)
}

// Watch visible change
watch(() => props.visible, async (val) => {
  if (val && props.sessionId) {
    await loadMessages()
    await readSessionApi(props.sessionId).catch(() => {})
    subscribeWs()
    startOnlinePolling()
  } else {
    unsubscribeWs()
    messages.value = []
    isOnline.value = false
  }
})

// Watch sessionId change — 切换会话时只重新订阅 handler，不重建连接
watch(() => props.sessionId, async (newVal, oldVal) => {
  if (newVal && props.visible && newVal !== oldVal) {
    await loadMessages()
    await readSessionApi(newVal).catch(() => {})
    subscribeWs()
  }
})

// 文件与图片上传发送逻辑
const imageInputRef = ref<HTMLInputElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

function triggerImageSelect() {
  imageInputRef.value?.click()
}

function triggerFileSelect() {
  fileInputRef.value?.click()
}

async function handleImageSelected(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file || !props.sessionId) return

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 10MB')
    return
  }

  sending.value = true
  try {
    const { url } = await uploadFileApi(file, 'CHAT')
    const msg = await sendMessageApi({
      sessionId: props.sessionId,
      msgType: 2,
      fileUrl: url,
      fileName: file.name,
      fileSize: file.size
    })
    messages.value.push(msg)
    await nextTick()
    scrollToBottom()
  } catch (err) {
    console.error('上传图片失败', err)
    ElMessage.error('图片发送失败')
  } finally {
    sending.value = false
    target.value = ''
  }
}

async function handleFileSelected(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file || !props.sessionId) return

  if (file.size > 50 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 50MB')
    return
  }

  sending.value = true
  try {
    const { url } = await uploadFileApi(file, 'CHAT')
    const msg = await sendMessageApi({
      sessionId: props.sessionId,
      msgType: 3,
      fileUrl: url,
      fileName: file.name,
      fileSize: file.size
    })
    messages.value.push(msg)
    await nextTick()
    scrollToBottom()
  } catch (err) {
    console.error('上传文件失败', err)
    ElMessage.error('文件发送失败')
  } finally {
    sending.value = false
    target.value = ''
  }
}

function openLink(url: string | null) {
  if (url) {
    window.open(url, '_blank')
  }
}

// Markdown 渲染配置
marked.setOptions({
  breaks: true,
  gfm: true
})

function renderMarkdown(content: string | null | undefined): string {
  if (!content) return ''
  try {
    return marked.parse(content) as string
  } catch (e) {
    return content
  }
}

function parseHouseCard(content: string | null | undefined) {
  if (!content) return null
  if (!content.includes('我正在看这套房源') && !content.includes('我正在咨询该房源')) return null
  
  const imgMatch = content.match(/!\[.*?\]\((.*?)\)/)
  const coverUrl = imgMatch && imgMatch[1] ? imgMatch[1] : null
  
  const nameMatch = content.match(/\*\*房源名称\*\*：(.*?)(?:\n|$)/)
  const projectName = nameMatch && nameMatch[1] ? nameMatch[1].trim() : ''
  
  const priceMatch = content.match(/\*\*售价\/租金\*\*：(.*?)(?:\n|$)/)
  const price = priceMatch && priceMatch[1] ? priceMatch[1].trim() : ''
  
  const layoutMatch = content.match(/\*\*户型\/面积\*\*：(.*?)(?:\n|$)/)
  const layout = layoutMatch && layoutMatch[1] ? layoutMatch[1].trim() : ''
  
  const idMatch = content.match(/\/house\/(\d+)/)
  const houseId = idMatch && idMatch[1] ? Number(idMatch[1]) : null
  
  if (!projectName && !price) return null
  
  return {
    coverUrl,
    projectName,
    price,
    layout,
    houseId
  }
}

interface MessageSegment {
  type: 'text' | 'card'
  content: string
  cardData?: {
    coverUrl: string | null
    projectName: string
    price: string
    layout: string
    houseId: number | null
  }
}

function parseMessageSegments(content: string | null | undefined): MessageSegment[] {
  if (!content) return []
  
  const segments: MessageSegment[] = []
  // 全局正则匹配卡片结构
  const cardRegex = /(!\[.*?\]\((.*?)\)[\s\S]*?### 🏠 我正在(?:咨询该房源|看这套房源)：[\s\S]*?\*\*房源名称\*\*：(.*?)(?:\n|$)[\s\S]*?\*\*售价\/租金\*\*：(.*?)(?:\n|$)[\s\S]*?\*\*户型\/面积\*\*：(.*?)(?:\n|$)[\s\S]*?\[点击查看房源详情\]\(\/house\/(\d+)\))/g
  
  let lastIndex = 0
  let match
  
  while ((match = cardRegex.exec(content)) !== null) {
    const matchIndex = match.index
    const matchedText = match[0]
    
    // 如果匹配之前有普通文本段
    if (matchIndex > lastIndex) {
      const textVal = content.substring(lastIndex, matchIndex).trim()
      if (textVal) {
        segments.push({ type: 'text', content: textVal })
      }
    }
    
    const coverUrl = match[2] ? match[2].trim() : null
    const projectName = match[3] ? match[3].trim() : ''
    const price = match[4] ? match[4].trim() : ''
    const layout = match[5] ? match[5].trim() : ''
    const houseId = match[6] ? Number(match[6]) : null
    
    segments.push({
      type: 'card',
      content: matchedText,
      cardData: {
        coverUrl: coverUrl || null,
        projectName,
        price,
        layout,
        houseId
      }
    })
    
    lastIndex = cardRegex.lastIndex
  }
  
  if (lastIndex < content.length) {
    const textVal = content.substring(lastIndex).trim()
    if (textVal) {
      segments.push({ type: 'text', content: textVal })
    }
  }
  
  // 兼容性兜底：如果没有通过正则匹配出任何切片，但原有的 parseHouseCard 可以解析成功（如历史遗留的单卡片格式）
  if (segments.length === 0) {
    const legacyCard = parseHouseCard(content)
    if (legacyCard) {
      segments.push({
        type: 'card',
        content,
        cardData: legacyCard
      })
    } else {
      segments.push({ type: 'text', content })
    }
  }
  
  return segments
}

// 快速发送房源卡片提示
const showHouseLink = ref(true)

watch(() => props.visible, (val) => {
  if (val) {
    showHouseLink.value = true
  }
})

watch(() => props.sessionId, () => {
  showHouseLink.value = true
})

async function sendHouseCard() {
  if (!props.houseContext || !props.sessionId || sending.value) return

  sending.value = true
  const content = `![cover](${props.houseContext.coverUrl || ''})\n` +
    `### 🏠 我正在咨询该房源：\n` +
    `**房源名称**：${props.houseContext.projectName}\n` +
    `**售价/租金**：${props.houseContext.price}${props.houseContext.priceUnitText}\n` +
    `**户型/面积**：${props.houseContext.layout} · ${props.houseContext.area}㎡\n` +
    `[点击查看房源详情](/house/${props.houseContext.id})`;

  try {
    const msg = await sendMessageApi({
      sessionId: props.sessionId,
      msgType: 1,
      content: content,
    })
    messages.value.push(msg)
    showHouseLink.value = false
    await nextTick()
    scrollToBottom()
  } catch (err) {
    console.error('发送房源卡片失败', err)
    ElMessage.error('发送房源卡片失败')
  } finally {
    sending.value = false
  }
}

onBeforeUnmount(() => {
  unsubscribeWs()
})
</script>

<template>
  <Teleport to="body">
    <Transition name="chat-fade">
      <div v-if="visible" class="fixed inset-0 z-[9999] flex justify-end pointer-events-none" @click.self="handleClose">
        <Transition name="chat-slide">
          <div v-if="visible" class="w-full md:w-[450px] h-[100dvh] bg-slate-50 flex flex-col shadow-[0_0_40px_rgba(0,0,0,0.08)] border-l border-slate-200/60 overflow-hidden relative pointer-events-auto">
            
            <!-- Header -->
            <div class="bg-white/80 backdrop-blur-md px-6 py-4 flex items-center justify-between border-b border-slate-100 z-10 shrink-0">
              <div class="flex items-center gap-4">
                <div class="relative w-12 h-12 rounded-full bg-linear-to-br from-blue-400 to-blue-600 text-white flex items-center justify-center text-xl font-black shadow-lg shadow-blue-500/30">
                  {{ targetName?.[0] || '管' }}
                  <span
                    class="absolute bottom-0 right-0 w-3 h-3 border-2 border-white rounded-full"
                    :class="isOnline ? 'bg-green-500' : 'bg-slate-300'"
                  ></span>
                </div>
                <div class="flex flex-col">
                  <span class="text-lg font-black text-slate-800">{{ targetName || '专属顾问' }}</span>
                  <span
                    class="text-xs font-bold flex items-center gap-1 mt-0.5"
                    :class="isOnline ? 'text-green-500' : 'text-slate-400'"
                  >
                    <span
                      class="w-1.5 h-1.5 rounded-full"
                      :class="isOnline ? 'bg-green-500 animate-pulse' : 'bg-slate-300'"
                    ></span>
                    {{ isOnline ? '当前在线' : '离线' }}
                  </span>
                </div>
              </div>
              <button 
                class="w-10 h-10 rounded-full bg-slate-100 text-slate-500 flex items-center justify-center hover:bg-slate-200 hover:text-slate-700 transition-colors active:scale-95" 
                @click="handleClose"
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M18 6L6 18M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Messages -->
            <div ref="listRef" class="flex-1 overflow-y-auto px-6 py-6 flex flex-col gap-6 custom-scrollbar scroll-smooth">
              <div v-if="loading" class="flex flex-col items-center justify-center py-10 text-slate-400 gap-3">
                <div class="w-8 h-8 border-4 border-slate-200 border-t-blue-600 rounded-full animate-spin"></div>
                <span class="text-sm font-bold">同步聊天记录中...</span>
              </div>

              <template v-for="(msg, index) in messages" :key="msg.id">
                <!-- Time separator -->
                <div v-if="shouldShowTime(index > 0 ? messages[index - 1] : null, msg)" class="text-center">
                  <span class="text-xs font-bold text-slate-400 bg-slate-200/50 px-3 py-1 rounded-full">
                    {{ formatMsgTime(msg.createTime) }}
                  </span>
                </div>

                <!-- System message -->
                <div v-if="msg.msgType === 4" class="text-center">
                  <span class="text-xs font-bold text-slate-400 bg-slate-200/50 px-4 py-1.5 rounded-full">
                    {{ msg.content }}
                  </span>
                </div>

                <!-- Normal message -->
                <div v-else class="flex w-full" :class="msg.senderId === currentUserId && msg.senderType === 2 ? 'justify-end' : 'justify-start'">
                  <div 
                    class="max-w-[75%] px-5 py-3.5 text-[15px] font-medium leading-relaxed break-words shadow-sm relative group"
                    :class="[
                      msg.senderId === currentUserId && msg.senderType === 2 
                        ? 'bg-gradient-to-br from-indigo-500 to-indigo-600 text-white rounded-2xl rounded-tr-sm shadow-lg shadow-indigo-500/20' 
                        : 'bg-white text-slate-800 rounded-2xl rounded-tl-sm border border-slate-100 shadow-sm'
                    ]"
                  >
                    <div v-if="msg.msgType === 1" class="flex flex-col gap-3">
                      <template v-for="(seg, sIdx) in parseMessageSegments(msg.content)" :key="sIdx">
                        <!-- 房源卡片定制化组件 -->
                        <div v-if="seg.type === 'card' && seg.cardData" class="bg-white rounded-2xl p-4 flex flex-col gap-3 min-w-[240px] max-w-[280px] border border-slate-100 text-slate-800 shadow-xs select-none">
                          <div class="text-[11px] font-bold text-slate-400 flex items-center gap-1 select-none">
                            <span>🏠 咨询房源</span>
                          </div>
                          <img 
                            v-if="seg.cardData.coverUrl" 
                            :src="seg.cardData.coverUrl" 
                            class="w-full h-32 object-cover rounded-xl shadow-xs" 
                            alt="房源图片" 
                          />
                          <div class="flex flex-col gap-1 text-left">
                            <div class="font-extrabold text-[14px] text-slate-900 truncate">{{ seg.cardData.projectName }}</div>
                            <div class="text-[12px] text-slate-500 flex items-center gap-1.5 mt-0.5">
                              <span class="font-medium text-slate-400">售价/租金：</span>
                              <span class="font-black text-rose-650">{{ seg.cardData.price }}</span>
                            </div>
                            <div class="text-[12px] text-slate-500 flex items-center gap-1.5">
                              <span class="font-medium text-slate-400">户型/面积：</span>
                              <span class="font-semibold text-slate-700">{{ seg.cardData.layout }}</span>
                            </div>
                          </div>
                          <a 
                            v-if="seg.cardData.houseId"
                            :href="`/house/${seg.cardData.houseId}`" 
                            target="_blank" 
                            class="mt-1 text-center text-xs font-black text-indigo-650 hover:text-indigo-750 bg-indigo-50 hover:bg-indigo-100/80 py-2.5 rounded-xl border border-indigo-100/60 transition-all flex items-center justify-center cursor-pointer select-none"
                          >
                            点击查看房源详情
                          </a>
                        </div>
                        
                        <!-- 普通 Markdown 消息 -->
                        <div v-else class="markdown-body" v-html="renderMarkdown(seg.content)"></div>
                      </template>
                    </div>
                    <div v-else-if="msg.msgType === 2" class="cursor-pointer overflow-hidden rounded-xl active:scale-98 transition-transform" @click="openLink(msg.fileUrl)">
                      <img :src="msg.fileUrl || ''" alt="图片" class="max-w-[200px] max-h-[200px] object-cover hover:opacity-90 transition-opacity" />
                    </div>
                    <div 
                      v-else 
                      class="flex items-center gap-2 underline underline-offset-2 cursor-pointer"
                      :class="msg.senderId === currentUserId && msg.senderType === 2 ? 'text-white' : 'text-blue-600 hover:text-blue-700'"
                      @click="openLink(msg.fileUrl)"
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="shrink-0"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path><polyline points="13 2 13 9 20 9"></polyline></svg>
                      <span class="truncate max-w-[180px]" :title="msg.fileName || undefined">{{ msg.fileName || '查看附件' }}</span>
                    </div>
                  </div>
                </div>
              </template>

              <div v-if="!loading && messages.length === 0" class="flex-1 flex flex-col items-center justify-center text-slate-400 gap-4 mt-10">
                <div class="w-20 h-20 bg-blue-50 rounded-full flex items-center justify-center text-blue-500">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                </div>
                <p class="font-bold text-sm">快来开启您的专属服务吧</p>
              </div>
            </div>

            <!-- Input -->
            <div class="bg-white px-4 py-4 border-t border-slate-100 shrink-0">
              <!-- 淘宝式快捷发送房源卡片提示栏 -->
              <div 
                v-if="houseContext && showHouseLink" 
                class="mb-3 p-3 bg-slate-50 border border-slate-150 rounded-2xl flex items-center justify-between gap-3 animate-fade-in relative z-20"
              >
                <div class="flex items-center gap-3 min-w-0">
                  <img 
                    v-if="houseContext.coverUrl" 
                    :src="houseContext.coverUrl" 
                    class="w-12 h-12 object-cover rounded-lg flex-shrink-0" 
                  />
                  <div class="flex flex-col min-w-0 text-left">
                    <span class="text-[10px] font-bold text-slate-400">您可能想咨询该房源：</span>
                    <span class="text-sm font-black text-slate-800 truncate mt-0.5">{{ houseContext.projectName }}</span>
                    <span class="text-[11px] font-bold text-blue-600 mt-0.5">
                      {{ houseContext.price }}{{ houseContext.priceUnitText }} · {{ houseContext.layout }} · {{ houseContext.area }}㎡
                    </span>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <button 
                    class="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-black rounded-xl transition-all active:scale-95 whitespace-nowrap cursor-pointer"
                    @click="sendHouseCard"
                  >
                    发送卡片
                  </button>
                  <button 
                    class="p-1 hover:bg-slate-200 text-slate-400 hover:text-slate-600 rounded-lg transition-colors cursor-pointer"
                    @click="showHouseLink = false"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                      <line x1="18" y1="6" x2="6" y2="18"></line>
                      <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                  </button>
                </div>
              </div>

              <!-- 附件与文件发送工具栏 -->
              <div class="flex items-center gap-4 px-2 mb-3">
                <!-- 发送图片 -->
                <button 
                  class="text-slate-400 hover:text-blue-500 hover:scale-110 active:scale-95 transition-all cursor-pointer flex items-center justify-center p-1 rounded-lg hover:bg-slate-50"
                  title="发送图片"
                  :disabled="sending"
                  @click="triggerImageSelect"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                    <circle cx="8.5" cy="8.5" r="1.5"/>
                    <polyline points="21 15 16 10 5 21"/>
                  </svg>
                </button>
                
                <!-- 发送文件 -->
                <button 
                  class="text-slate-400 hover:text-blue-500 hover:scale-110 active:scale-95 transition-all cursor-pointer flex items-center justify-center p-1 rounded-lg hover:bg-slate-50"
                  title="发送文件"
                  :disabled="sending"
                  @click="triggerFileSelect"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/>
                  </svg>
                </button>

                <!-- 隐藏的文件/图片上传节点 -->
                <input 
                  ref="imageInputRef" 
                  type="file" 
                  accept="image/*" 
                  class="hidden" 
                  @change="handleImageSelected"
                />
                <input 
                  ref="fileInputRef" 
                  type="file" 
                  class="hidden" 
                  @change="handleFileSelected"
                />
              </div>

              <div class="flex items-end gap-3 bg-slate-50 border border-slate-200 rounded-3xl p-2 focus-within:border-blue-500 focus-within:ring-4 focus-within:ring-blue-500/10 transition-all">
                <textarea
                  v-model="inputText"
                  class="flex-1 bg-transparent border-none resize-none px-4 py-2 text-[15px] text-slate-800 placeholder-slate-400 outline-none max-h-[120px] min-h-[44px] custom-scrollbar"
                  placeholder="请输入消息..."
                  rows="1"
                  @keydown="handleKeydown"
                />
                <button
                  class="w-11 h-11 rounded-full flex items-center justify-center shrink-0 transition-all active:scale-95"
                  :class="inputText.trim() && !sending ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/30 hover:bg-blue-700' : 'bg-slate-200 text-slate-400 cursor-not-allowed'"
                  :disabled="!inputText.trim() || sending"
                  @click="handleSend"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="translate-x-[-1px] translate-y-[1px]">
                    <line x1="22" y1="2" x2="11" y2="13"></line>
                    <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                  </svg>
                </button>
              </div>
              <div class="mt-3 text-center text-[11px] font-bold text-slate-300">
                按 Enter 键发送，Shift + Enter 换行
              </div>
            </div>
            
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* Custom Scrollbar for Chat Messages */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 20px;
}
.custom-scrollbar:hover::-webkit-scrollbar-thumb {
  background-color: #94a3b8;
}

/* Transitions */
.chat-fade-enter-active,
.chat-fade-leave-active {
  transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.chat-fade-enter-from,
.chat-fade-leave-to {
  opacity: 0;
}

.chat-slide-enter-active {
  transition: transform 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.chat-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.chat-slide-enter-from,
.chat-slide-leave-to {
  transform: translateX(100%);
}

/* Markdown Styling */
.markdown-body :deep(p) {
  margin: 0 0 6px 0;
}
.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}
.markdown-body :deep(strong) {
  font-weight: 700;
}
.markdown-body :deep(em) {
  font-style: italic;
}
.markdown-body :deep(ul) {
  list-style-type: disc;
  padding-left: 20px;
  margin: 4px 0 8px 0;
}
.markdown-body :deep(ol) {
  list-style-type: decimal;
  padding-left: 20px;
  margin: 4px 0 8px 0;
}
.markdown-body :deep(li) {
  margin-bottom: 4px;
}
.markdown-body :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  padding: 2px 6px;
  border-radius: 6px;
  font-size: 0.88em;
}

/* C端用户自己发的（渐变蓝紫色背景） */
.bg-gradient-to-br .markdown-body :deep(code) {
  background-color: rgba(255, 255, 255, 0.15);
  color: #ffffff;
}
.bg-gradient-to-br .markdown-body :deep(a) {
  color: #e0e7ff;
  text-decoration: underline;
}

/* 销售顾问发的（白色背景） */
.bg-white .markdown-body :deep(code) {
  background-color: #f1f5f9;
  color: #0f172a;
}
.bg-white .markdown-body :deep(a) {
  color: #4f46e5;
  text-decoration: underline;
}
</style>
