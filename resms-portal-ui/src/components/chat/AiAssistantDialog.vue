<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { sendAiMessageApi, type HouseItem, type ChatBlock } from '@/api/ai'
import { marked } from 'marked'
import HouseCard from './HouseCard.vue'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'transfer-human': []
}>()

const authStore = useAuthStore()

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  type: 'text' | 'suggestions'
  suggestions?: string[]
  recommendations?: HouseItem[]
  blocks?: ChatBlock[]
}

const messages = ref<Message[]>([])
const inputText = ref('')
const isTyping = ref(false)
const sessionId = ref<string | null>(null)
const listRef = ref<HTMLElement | null>(null)

/**
 * Render standard markdown content using marked
 */
function renderMarkdown(content: string): string {
  try {
    const clean = content.replace(/\\n/g, '\n').replace(/\\"/g, '"')
    return marked.parse(clean) as string
  } catch (e) {
    console.error('Failed to parse markdown:', e)
    return content
  }
}

/** 在 recommendations 数组中匹配特定 houseId 的结构化数据 */
function findRecommendation(recommendations: HouseItem[] | undefined, houseId: string): HouseItem {
  const found = recommendations?.find(r => r.houseId === houseId)
  return found || {
    houseId: houseId,
    projectName: '未知项目',
    district: null,
    price: '暂无报价',
    layout: null,
    area: null,
    coverUrl: null,
    sellingPoint: null
  }
}

/**
 * Helper to build assistant message
 */
function createAssistantMessage(content: string, type: 'text' | 'suggestions' = 'text', suggestions?: string[]): Message {
  return {
    id: Date.now().toString(),
    role: 'assistant',
    content,
    type,
    suggestions
  }
}

function showGreeting() {
  messages.value.push(createAssistantMessage(
    `您好 ${authStore.userInfo?.nickname || '朋友'}！我是您的房产 AI 助手。我可以帮您寻找心仪的房源、计算房贷或解答购房政策，您想了解什么？`
  ))
  messages.value.push({
    id: (Date.now() + 1).toString(),
    role: 'assistant',
    content: '',
    type: 'suggestions',
    suggestions: ['推荐高性价比新房', '查询地铁周边房源', '如何办理购房贷款？', '联系人工顾问']
  })
}

function scrollToBottom() {
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || isTyping.value) return

  // 用户消息
  messages.value.push({
    id: Date.now().toString(),
    role: 'user',
    content: text,
    type: 'text'
  })
  inputText.value = ''
  await nextTick()
  scrollToBottom()

  // 调用 AI 接口
  isTyping.value = true
  try {
    const res = await sendAiMessageApi({
      message: text,
      sessionId: sessionId.value ?? undefined,
    })
    sessionId.value = res.sessionId
    const msg = createAssistantMessage(res.reply)
    if (res.recommendations && res.recommendations.length > 0) {
      msg.recommendations = res.recommendations
    }
    if (res.blocks && res.blocks.length > 0) {
      msg.blocks = res.blocks
    }
    messages.value.push(msg)
  } catch {
    ElMessage.error('AI 助手暂时无法响应，请稍后再试')
  } finally {
    isTyping.value = false
    await nextTick()
    scrollToBottom()
  }
}

function handleSuggestion(suggestion: string) {
  if (suggestion === '联系人工顾问') {
    emit('transfer-human')
    return
  }
  inputText.value = suggestion
  handleSend()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleClose() {
  emit('update:visible', false)
}

watch(() => props.visible, (val) => {
  if (val && messages.value.length === 0) {
    sessionId.value = null
    showGreeting()
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="ai-fade">
      <div v-if="visible" class="fixed inset-0 z-[9999] flex justify-end bg-slate-900/20 backdrop-blur-sm" @click.self="handleClose">
        <Transition name="ai-slide">
          <div v-if="visible" class="w-full md:w-[480px] h-[100dvh] bg-white flex flex-col shadow-2xl overflow-hidden">
            
            <!-- Header -->
            <div class="bg-linear-to-r from-blue-600 to-indigo-600 px-6 py-5 flex items-center justify-between text-white shrink-0 relative overflow-hidden">
              <!-- Background Pattern -->
              <div class="absolute inset-0 opacity-10 pointer-events-none">
                <svg width="100%" height="100%" fill="none"><pattern id="pattern" x="0" y="0" width="20" height="20" patternUnits="userSpaceOnUse"><circle cx="2" cy="2" r="1" fill="currentColor"/></pattern><rect width="100%" height="100%" fill="url(#pattern)"/></svg>
              </div>

              <div class="flex items-center gap-4 z-10">
                <div class="w-12 h-12 rounded-2xl bg-white/20 backdrop-blur-md flex items-center justify-center text-2xl shadow-inner animate-pulse-slow">
                  🤖
                </div>
                <div>
                  <h3 class="text-xl font-black tracking-tight">AI 智能助理</h3>
                  <div class="flex items-center gap-1.5 mt-0.5">
                    <span class="w-2 h-2 bg-green-400 rounded-full animate-pulse"></span>
                    <span class="text-xs font-bold text-blue-100 uppercase tracking-widest">Online & Processing</span>
                  </div>
                </div>
              </div>
              <button 
                class="w-10 h-10 rounded-xl bg-white/10 text-white flex items-center justify-center hover:bg-white/20 transition-all z-10 active:scale-95" 
                @click="handleClose"
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M18 6L6 18M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Messages Area -->
            <div ref="listRef" class="flex-1 overflow-y-auto px-6 py-8 flex flex-col gap-8 custom-scrollbar bg-slate-50/50">
              <template v-for="msg in messages" :key="msg.id">
                
                <!-- AI Message -->
                <div v-if="msg.role === 'assistant'" class="flex flex-col gap-3 max-w-[85%] w-full">
                  <div v-if="msg.type === 'text'" class="flex gap-3 w-full">
                    <div class="w-8 h-8 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center text-sm shrink-0 mt-1 shadow-sm font-bold">
                      AI
                    </div>
                    <div class="bg-white px-5 py-3.5 rounded-2xl rounded-tl-none shadow-sm border border-slate-100 text-slate-700 leading-relaxed font-medium w-full overflow-hidden">
                      <!-- 分块渲染（支持文卡混排与卡片合并） -->
                      <div v-if="msg.blocks && msg.blocks.length > 0" class="flex flex-col gap-3 w-full">
                        <template v-for="(block, bIdx) in msg.blocks" :key="bIdx">
                          <!-- 文本 Block -->
                          <div
                            v-if="block.type === 'text'"
                            v-html="renderMarkdown(block.content || '')"
                            class="markdown-content"
                          ></div>
                          
                          <!-- 单张房源卡片 Block -->
                          <div v-else-if="block.type === 'house_card' && block.houseIds?.length" class="flex mt-2">
                            <HouseCard :data="findRecommendation(msg.recommendations, block.houseIds[0])" />
                          </div>
                          
                          <!-- 房源卡片组 Block -->
                          <div v-else-if="block.type === 'house_cards' && block.houseIds?.length" class="flex flex-wrap gap-3 mt-2">
                            <HouseCard
                              v-for="hId in block.houseIds"
                              :key="hId"
                              :data="findRecommendation(msg.recommendations, hId)"
                            />
                          </div>
                        </template>
                      </div>

                      <!-- 兜底：无 blocks 时渲染纯文本 + 推荐卡片 -->
                      <div v-else class="w-full">
                        <div
                          v-html="renderMarkdown(msg.content)"
                          class="markdown-content"
                        ></div>
                        <div v-if="msg.recommendations?.length" class="flex flex-wrap gap-3 mt-4">
                          <HouseCard
                            v-for="(item, idx) in msg.recommendations"
                            :key="idx"
                            :data="item"
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Suggestion Chips -->
                  <div v-else-if="msg.type === 'suggestions'" class="flex flex-wrap gap-2 ml-11">
                    <button 
                      v-for="s in msg.suggestions" 
                      :key="s"
                      @click="handleSuggestion(s)"
                      class="px-4 py-2 bg-white border border-blue-100 text-blue-600 text-sm font-bold rounded-full hover:bg-blue-50 hover:border-blue-200 transition-all shadow-sm active:scale-95"
                    >
                      {{ s }}
                    </button>
                  </div>
                </div>

                <!-- User Message -->
                <div v-else class="flex justify-end">
                  <div class="max-w-[80%] bg-blue-600 text-white px-5 py-3.5 rounded-2xl rounded-tr-none shadow-lg shadow-blue-600/20 font-medium leading-relaxed">
                    {{ msg.content }}
                  </div>
                </div>
              </template>

              <!-- Typing Indicator -->
              <div v-if="isTyping" class="flex gap-3">
                <div class="w-8 h-8 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center text-sm shrink-0 font-bold">AI</div>
                <div class="bg-white px-4 py-3 rounded-2xl rounded-tl-none shadow-sm border border-slate-100 flex gap-1 items-center">
                  <span class="w-1.5 h-1.5 bg-slate-300 rounded-full animate-bounce"></span>
                  <span class="w-1.5 h-1.5 bg-slate-300 rounded-full animate-bounce [animation-delay:0.2s]"></span>
                  <span class="w-1.5 h-1.5 bg-slate-300 rounded-full animate-bounce [animation-delay:0.4s]"></span>
                </div>
              </div>
            </div>

            <!-- Footer / Input -->
            <div class="p-6 bg-white border-t border-slate-100">
              <div class="flex items-center gap-3 mb-4">
                <button 
                  @click="emit('transfer-human')"
                  class="flex items-center gap-2 px-4 py-2 bg-slate-100 text-slate-600 text-xs font-black rounded-lg hover:bg-slate-200 transition-colors"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                  转接人工顾问
                </button>
              </div>

              <div class="relative group">
                <textarea
                  v-model="inputText"
                  @keydown="handleKeydown"
                  rows="1"
                  placeholder="询问 AI 助手关于房源的信息..."
                  class="w-full bg-slate-50 border-2 border-transparent rounded-2xl px-5 py-4 pr-16 text-slate-800 placeholder-slate-400 focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition-all outline-none resize-none custom-scrollbar font-medium"
                ></textarea>
                <button 
                  @click="handleSend"
                  :disabled="!inputText.trim() || isTyping"
                  class="absolute right-2 top-1/2 -translate-y-1/2 w-12 h-12 bg-blue-600 text-white rounded-xl flex items-center justify-center shadow-lg shadow-blue-600/30 hover:bg-blue-700 disabled:opacity-50 disabled:shadow-none transition-all active:scale-95"
                >
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
                  </svg>
                </button>
              </div>
              <p class="mt-4 text-center text-[10px] text-slate-400 font-bold uppercase tracking-widest">
                AI powered by RESMS Smart Engine
              </p>
            </div>

          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 5px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 10px; }

@keyframes pulse-slow {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(0.98); }
}
.animate-pulse-slow { animation: pulse-slow 3s infinite ease-in-out; }

.ai-fade-enter-active, .ai-fade-leave-active { transition: opacity 0.4s ease; }
.ai-fade-enter-from, .ai-fade-leave-to { opacity: 0; }

.ai-slide-enter-active { transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1); }
.ai-slide-leave-active { transition: transform 0.4s cubic-bezier(0.7, 0, 0.84, 0); }
.ai-slide-enter-from, .ai-slide-leave-to { transform: translateX(100%); }

/* Markdown Styles */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3) {
  font-weight: 800;
  color: #1e293b;
  margin-top: 1rem;
  margin-bottom: 0.5rem;
}
.markdown-content :deep(h1) { font-size: 1.25rem; border-bottom: 2px solid #e2e8f0; padding-bottom: 0.25rem; }
.markdown-content :deep(h2) { font-size: 1.1rem; border-bottom: 1px solid #f1f5f9; padding-bottom: 0.25rem; }
.markdown-content :deep(h3) { font-size: 1rem; }

.markdown-content :deep(p) {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  line-height: 1.6;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  padding-left: 1.25rem;
}
.markdown-content :deep(ul) { list-style-type: disc; }
.markdown-content :deep(ol) { list-style-type: decimal; }

.markdown-content :deep(li) {
  margin-top: 0.25rem;
  margin-bottom: 0.25rem;
  font-size: 0.875rem;
}

.markdown-content :deep(strong) {
  font-weight: 800;
  color: #0f172a;
}

.markdown-content :deep(hr) {
  margin: 1rem 0;
  border: 0;
  border-top: 1px solid #e2e8f0;
}

.markdown-content :deep(code) {
  font-family: monospace;
  background-color: #f1f5f9;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
  font-size: 0.8rem;
}

.markdown-content :deep(blockquote) {
  border-left: 4px solid #3b82f6;
  background-color: #f8fafc;
  padding: 0.5rem 1rem;
  margin: 0.5rem 0;
  border-radius: 0 0.5rem 0.5rem 0;
  color: #475569;
  font-style: italic;
}
</style>
