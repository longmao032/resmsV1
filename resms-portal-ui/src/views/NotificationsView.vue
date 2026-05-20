<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Bell, 
  Check, 
  Cpu, 
  Calendar, 
  Wallet, 
  Document, 
  Clock, 
  User, 
  ArrowRight, 
  MessageBox, 
  CircleCheck,
  Refresh,
  Message,
  InfoFilled,
  Delete
} from '@element-plus/icons-vue'
import { myNotifications, markNotificationRead, markAllNotificationsRead, getUnreadCount, type NotificationItem } from '@/api/notification'
import ChatDialog from '@/components/chat/ChatDialog.vue'
import { useAuthStore } from '@/stores/auth'
import { listSessionsApi, readSessionApi, deleteSessionApi, type SessionVO } from '@/api/chat'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const list = ref<NotificationItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const unreadNum = ref(0)

// 过滤状态选项：'all' | 'unread' | 'read'
const currentStatusTab = ref<'all' | 'unread' | 'read'>('all')

// 过滤类型选项：null (全部) | 1 (系统通知) | 2 (任务提醒) | 3 (交易动态) | 4 (审批通知) | 5 (聊天消息)
const currentTypeTab = ref<number | null>(null)

// 详情弹窗控制
const detailVisible = ref(false)
const selectedItem = ref<NotificationItem | null>(null)

// 动态类型信息映射
const typeConfigMap: Record<number, { label: string; type: string; icon: any; colorClass: string; bgClass: string; borderClass: string }> = {
  1: { 
    label: '系统通知', 
    type: 'primary', 
    icon: Cpu, 
    colorClass: 'text-blue-600 dark:text-blue-400',
    bgClass: 'bg-blue-50/50 dark:bg-blue-950/20',
    borderClass: 'hover:border-blue-300 dark:hover:border-blue-800'
  },
  2: { 
    label: '任务提醒', 
    type: 'warning', 
    icon: Calendar, 
    colorClass: 'text-amber-600 dark:text-amber-400',
    bgClass: 'bg-amber-50/50 dark:bg-amber-950/20',
    borderClass: 'hover:border-amber-300 dark:hover:border-amber-800'
  },
  3: { 
    label: '交易动态', 
    type: 'success', 
    icon: Wallet, 
    colorClass: 'text-emerald-600 dark:text-emerald-400',
    bgClass: 'bg-emerald-50/50 dark:bg-emerald-950/20',
    borderClass: 'hover:border-emerald-300 dark:hover:border-emerald-800'
  },
  4: { 
    label: '审批通知', 
    type: 'danger', 
    icon: Document, 
    colorClass: 'text-rose-600 dark:text-rose-400',
    bgClass: 'bg-rose-50/50 dark:bg-rose-950/20',
    borderClass: 'hover:border-rose-300 dark:hover:border-rose-800'
  },
  5: {
    label: '聊天消息',
    type: 'info',
    icon: Message,
    colorClass: 'text-indigo-600 dark:text-indigo-400',
    bgClass: 'bg-indigo-50/50 dark:bg-indigo-950/20',
    borderClass: 'hover:border-indigo-300 dark:hover:border-indigo-850'
  }
}

function getTypeConfig(type: number) {
  return typeConfigMap[type] || { 
    label: '通知', 
    type: 'info', 
    icon: Bell, 
    colorClass: 'text-gray-600 dark:text-gray-400',
    bgClass: 'bg-gray-50/50 dark:bg-gray-950/20',
    borderClass: 'hover:border-gray-300 dark:hover:border-gray-800'
  }
}

// 获取未读数
async function fetchUnread() {
  try {
    unreadNum.value = await getUnreadCount()
  } catch (err) {
    console.error('获取未读通知数失败', err)
  }
}

// 加载列表数据
async function fetchList() {
  loading.value = true
  try {
    const isReadVal = currentStatusTab.value === 'all' ? undefined : (currentStatusTab.value === 'unread' ? 0 : 1)
    const res = await myNotifications(pageNum.value, pageSize.value, isReadVal)
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载通知失败')
  } finally {
    loading.value = false
  }
}

// 切换已读/未读状态Tab
function handleStatusChange(tab: 'all' | 'unread' | 'read') {
  currentStatusTab.value = tab
  pageNum.value = 1
  fetchList()
}

// 切换类型Tab（通知类型仅在前端过滤以保证切换的即时感）
function handleTypeChange(type: number | null) {
  currentTypeTab.value = type
  if (type === 5) {
    fetchSessions()
  }
}

const chatVisible = ref(false)
const chatSessionId = ref<number | null>(null)
const chatTargetName = ref('销售专员')
const sessions = ref<SessionVO[]>([])
const sessionsLoading = ref(false)

async function fetchSessions() {
  sessionsLoading.value = true
  try {
    sessions.value = await listSessionsApi()
  } catch (err) {
    console.error('获取会话列表失败', err)
    ElMessage.error('加载聊天列表失败')
  } finally {
    sessionsLoading.value = false
  }
}

function getMemberName(session: SessionVO) {
  const other = session.members?.find(m => m.userType === 1)
  return other ? other.userName : (session.sessionName || '销售专员')
}

function getMemberAvatar(session: SessionVO) {
  const other = session.members?.find(m => m.userType === 1)
  return other ? other.userAvatar : null
}

function openChatSession(session: SessionVO) {
  chatSessionId.value = session.id
  chatTargetName.value = getMemberName(session)
  chatVisible.value = true
  if (session.unreadCount > 0) {
    session.unreadCount = 0
    readSessionApi(session.id).catch(e => {
      console.error('标记会话已读失败', e)
    })
  }
}

async function handleDeleteSession(session: SessionVO) {
  ElMessageBox.confirm(
    `确定要删除与“${getMemberName(session)}”的聊天记录吗？删除后将无法恢复。`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      draggable: true
    }
  ).then(async () => {
    try {
      await deleteSessionApi(session.id)
      ElMessage.success('删除成功')
      sessions.value = sessions.value.filter(s => s.id !== session.id)
    } catch (err) {
      console.error('删除会话失败', err)
      ElMessage.error('删除聊天失败，请稍后重试')
    }
  }).catch(() => {})
}

// 计算前端二次过滤后的列表
const filteredList = computed(() => {
  if (currentTypeTab.value === null) {
    return list.value
  }
  return list.value.filter(n => n.noticeType === currentTypeTab.value)
})

// 单个标为已读
async function handleRead(item: NotificationItem) {
  if (item.isRead === 1) return
  try {
    await markNotificationRead(item.id)
    item.isRead = 1
    await fetchUnread()
  } catch {
    ElMessage.error('标记已读失败')
  }
}

// 全部标为已读
async function handleReadAll() {
  if (unreadNum.value === 0) {
    ElMessage.info('暂无未读消息')
    return
  }
  
  ElMessageBox.confirm(
    '确定要将所有未读通知标记为已读吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
      draggable: true
    }
  ).then(async () => {
    try {
      await markAllNotificationsRead()
      list.value.forEach(n => n.isRead = 1)
      unreadNum.value = 0
      ElMessage.success('已全部标记为已读')
      fetchList()
    } catch {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

// 查看详情弹窗并自动标已读
async function showDetail(item: NotificationItem) {
  selectedItem.value = item
  detailVisible.value = true
  if (item.isRead === 0) {
    await handleRead(item)
  }
}

// 前往处理/跳转路由
function handleAction(item: NotificationItem) {
  detailVisible.value = false
  if (item.routerPath) {
    router.push(item.routerPath)
  }
}

// 时间美化
function formatTime(time: string | null | undefined) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN', { 
    month: 'short', 
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchList()
  fetchUnread()
})
</script>

<template>
  <div class="min-h-screen bg-linear-to-br from-slate-50 via-slate-100 to-indigo-50/30 pt-24 pb-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-5xl mx-auto">
      
      <!-- 主视觉玻璃拟态卡片容器 -->
      <div class="bg-white/80 backdrop-blur-xl border border-white/60 shadow-xl rounded-3xl overflow-hidden min-h-[600px] flex flex-col md:flex-row">
        
        <!-- 左侧控制台/抽屉导航栏 -->
        <div class="w-full md:w-80 bg-slate-50/50 border-b md:border-b-0 md:border-r border-slate-200/60 p-6 flex flex-col justify-between shrink-0">
          <div>
            <!-- 头部标题 -->
            <div class="flex items-center gap-3 mb-8">
              <div class="w-10 h-10 rounded-2xl bg-indigo-600 flex items-center justify-center text-white shadow-lg shadow-indigo-600/20">
                <Bell class="text-xl" />
              </div>
              <div>
                <h1 class="text-xl font-bold text-slate-800 tracking-tight">消息中心</h1>
                <p class="text-xs text-slate-400">系统与动态的即时汇总</p>
              </div>
            </div>

            <!-- 未读概览卡片 -->
            <div class="bg-linear-to-br from-indigo-500 to-violet-600 rounded-2xl p-5 text-white shadow-lg shadow-indigo-500/25 mb-6 relative overflow-hidden group">
              <div class="absolute -right-4 -bottom-4 text-white/10 group-hover:scale-110 transition-transform duration-300">
                <MessageBox class="w-24 h-24" />
              </div>
              <div class="relative z-10">
                <span class="text-xs text-indigo-100 uppercase tracking-wider font-medium">未读通知</span>
                <h2 class="text-4xl font-extrabold mt-1 tracking-tight">{{ unreadNum }}</h2>
                <div class="flex items-center justify-between mt-4 pt-4 border-t border-white/15">
                  <span class="text-xs text-indigo-100">全部消息: {{ total }} 条</span>
                  <button 
                    v-if="unreadNum > 0"
                    class="text-xs font-semibold bg-white/20 hover:bg-white/35 active:scale-95 transition-all text-white px-2.5 py-1 rounded-lg backdrop-blur-xs flex items-center gap-1 cursor-pointer"
                    @click="handleReadAll"
                  >
                    <Check class="w-3 h-3" /> 一键已读
                  </button>
                </div>
              </div>
            </div>

            <!-- 状态切换菜单 -->
            <nav class="space-y-1.5">
              <button 
                class="w-full flex items-center justify-between px-4 py-3 rounded-xl text-sm font-medium transition-all cursor-pointer"
                :class="currentStatusTab === 'all' 
                  ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/30' 
                  : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'"
                @click="handleStatusChange('all')"
              >
                <div class="flex items-center gap-3">
                  <Message class="w-4 h-4" />
                  <span>全部消息</span>
                </div>
                <span class="text-xs px-2 py-0.5 rounded-full bg-slate-200/60 text-slate-600 font-semibold">{{ total }}</span>
              </button>

              <button 
                class="w-full flex items-center justify-between px-4 py-3 rounded-xl text-sm font-medium transition-all cursor-pointer"
                :class="currentStatusTab === 'unread' 
                  ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/30' 
                  : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'"
                @click="handleStatusChange('unread')"
              >
                <div class="flex items-center gap-3">
                  <div class="relative">
                    <Bell class="w-4 h-4" />
                    <span v-if="unreadNum > 0" class="absolute -top-1 -right-1 w-2 h-2 bg-rose-500 rounded-full animate-ping" />
                    <span v-if="unreadNum > 0" class="absolute -top-1 -right-1 w-2 h-2 bg-rose-500 rounded-full" />
                  </div>
                  <span>未读</span>
                </div>
                <span 
                  v-if="unreadNum > 0" 
                  class="text-xs px-2 py-0.5 rounded-full bg-rose-500 text-white font-semibold"
                >
                  {{ unreadNum }}
                </span>
              </button>

              <button 
                class="w-full flex items-center justify-between px-4 py-3 rounded-xl text-sm font-medium transition-all cursor-pointer"
                :class="currentStatusTab === 'read' 
                  ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/30' 
                  : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'"
                @click="handleStatusChange('read')"
              >
                <div class="flex items-center gap-3">
                  <CircleCheck class="w-4 h-4" />
                  <span>已读</span>
                </div>
              </button>
            </nav>
          </div>

          <div class="mt-8 pt-4 border-t border-slate-200/60 hidden md:block">
            <div class="flex items-center justify-between text-xs text-slate-400">
              <span>上次更新：刚刚</span>
              <button 
                class="hover:text-indigo-600 transition-colors p-1 rounded-md hover:bg-slate-100 flex items-center justify-center cursor-pointer" 
                title="刷新列表" 
                @click="fetchList"
              >
                <Refresh class="w-3.5 h-3.5" :class="loading ? 'animate-spin' : ''" />
              </button>
            </div>
          </div>
        </div>

        <!-- 右侧内容与列表展示区 -->
        <div class="flex-1 min-w-0 p-6 sm:p-8 flex flex-col justify-between">
          <div>
            <!-- 顶部类型过滤器药丸 Tabs -->
            <div class="flex flex-wrap gap-2 mb-6">
              <button 
                class="px-4 py-1.5 rounded-full text-xs font-semibold transition-all border active:scale-95 cursor-pointer"
                :class="currentTypeTab === null 
                  ? 'bg-slate-800 border-slate-800 text-white shadow-xs' 
                  : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50 hover:text-slate-800'"
                @click="handleTypeChange(null)"
              >
                全部类型
              </button>
              
              <button 
                v-for="(config, typeKey) in typeConfigMap" 
                :key="typeKey"
                class="px-4 py-1.5 rounded-full text-xs font-semibold transition-all border flex items-center gap-1.5 active:scale-95 cursor-pointer"
                :class="currentTypeTab === Number(typeKey) 
                  ? 'bg-slate-800 border-slate-800 text-white shadow-xs' 
                  : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50 hover:text-slate-850'"
                @click="handleTypeChange(Number(typeKey))"
              >
                <component :is="config.icon" class="w-3.5 h-3.5" />
                <span>{{ config.label }}</span>
              </button>
            </div>

            <!-- 数据加载骨架屏 (通知消息) -->
            <div v-if="loading && currentTypeTab !== 5" class="space-y-4">
              <div v-for="i in 3" :key="i" class="bg-white/50 border border-slate-150 rounded-2xl p-5 space-y-3 animate-pulse">
                <div class="flex items-center justify-between">
                  <div class="h-4 w-1/3 bg-slate-200 rounded-sm"></div>
                  <div class="h-5 w-16 bg-slate-200 rounded-full"></div>
                </div>
                <div class="space-y-2">
                  <div class="h-3 w-full bg-slate-100 rounded-sm"></div>
                  <div class="h-3 w-5/6 bg-slate-100 rounded-sm"></div>
                </div>
                <div class="h-3 w-1/4 bg-slate-100 rounded-sm mt-2"></div>
              </div>
            </div>

            <!-- 空状态呈现 (通知消息) -->
            <div v-else-if="filteredList.length === 0 && currentTypeTab !== 5" class="flex flex-col items-center justify-center py-20 text-center">
              <div class="relative w-24 h-24 rounded-full bg-slate-100 flex items-center justify-center mb-6">
                <Bell class="text-4xl text-slate-350" />
                <div class="absolute inset-0 rounded-full border border-dashed border-slate-300 animate-spin-slow"></div>
              </div>
              <h3 class="text-base font-semibold text-slate-700">暂无通知消息</h3>
              <p class="text-xs text-slate-400 mt-1.5 max-w-xs leading-relaxed">
                当前筛选分类下没有任何消息提醒，有新动态时我们会第一时间通知您。
              </p>
              <button 
                class="mt-6 px-4 py-2 rounded-xl text-xs font-semibold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 active:scale-95 transition-all flex items-center gap-1.5 cursor-pointer"
                @click="fetchList"
              >
                <Refresh class="w-3.5 h-3.5" /> 刷新重试
              </button>
            </div>

            <!-- 消息卡片列表 (通知消息) -->
            <div v-else-if="currentTypeTab !== 5" class="space-y-3.5">
              <div
                v-for="item in filteredList"
                :key="item.id"
                class="group bg-white rounded-2xl p-5 border cursor-pointer transition-all duration-300 relative overflow-hidden flex flex-col sm:flex-row justify-between items-start gap-4 hover:-translate-y-0.5 hover:shadow-md"
                :class="[
                  item.isRead === 0 
                    ? 'border-indigo-100/80 shadow-xs bg-linear-to-r from-indigo-50/20 to-white' 
                    : 'border-slate-100 hover:border-slate-200 bg-white',
                  getTypeConfig(item.noticeType).borderClass
                ]"
                @click="showDetail(item)"
              >
                <!-- 未读状态的左侧发光高亮条 -->
                <div 
                  v-if="item.isRead === 0" 
                  class="absolute left-0 top-0 bottom-0 w-1 bg-indigo-500"
                />

                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-2">
                    <span 
                      v-if="item.isRead === 0" 
                      class="w-2.5 h-2.5 rounded-full bg-indigo-600 shrink-0 relative flex"
                    >
                      <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75"></span>
                    </span>
                    <h3 
                      class="font-semibold text-slate-800 truncate leading-snug group-hover:text-slate-900"
                      :class="item.isRead === 0 ? 'text-slate-900 font-bold' : ''"
                    >
                      {{ item.title }}
                    </h3>
                  </div>

                  <p class="text-sm text-slate-500 line-clamp-2 mb-3 leading-relaxed">
                    {{ item.content }}
                  </p>

                  <div class="flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-slate-400">
                    <div class="flex items-center gap-1">
                      <Clock class="w-3.5 h-3.5 shrink-0 text-slate-350" />
                      <span>{{ formatTime(item.sendTime) }}</span>
                    </div>
                    <span class="hidden sm:inline text-slate-250">•</span>
                    <div class="flex items-center gap-1">
                      <User class="w-3.5 h-3.5 shrink-0 text-slate-350" />
                      <span>{{ item.senderName }}</span>
                    </div>
                  </div>
                </div>

                <!-- 右侧标签和悬停跳转箭头 -->
                <div class="flex sm:flex-col items-end justify-between sm:justify-start gap-3 w-full sm:w-auto shrink-0 pt-3 sm:pt-0 border-t sm:border-t-0 border-slate-100">
                  <el-tag
                    :type="getTypeConfig(item.noticeType).type"
                    size="small"
                    class="!rounded-lg font-semibold shrink-0"
                    effect="light"
                  >
                    <div class="flex items-center gap-1">
                      <component :is="getTypeConfig(item.noticeType).icon" class="w-3 h-3" />
                      <span>{{ getTypeConfig(item.noticeType).label }}</span>
                    </div>
                  </el-tag>
                  
                  <span class="text-xs text-indigo-500 font-medium opacity-0 group-hover:opacity-100 group-hover:translate-x-1 transition-all duration-300 hidden sm:flex items-center gap-0.5">
                    查看详情 <ArrowRight class="w-3 h-3" />
                  </span>
                </div>
              </div>
            </div>

            <!-- 聊天会话列表 (聊天消息) -->
            <div v-if="currentTypeTab === 5">
              <div v-if="sessionsLoading" class="space-y-4">
                <div v-for="i in 3" :key="i" class="bg-white/50 border border-slate-150 rounded-2xl p-5 space-y-3 animate-pulse">
                  <div class="flex items-center gap-3">
                    <div class="w-12 h-12 rounded-full bg-slate-200"></div>
                    <div class="flex-1 space-y-2">
                      <div class="h-4 w-1/4 bg-slate-200 rounded"></div>
                      <div class="h-3 w-3/4 bg-slate-100 rounded"></div>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else-if="sessions.length === 0" class="flex flex-col items-center justify-center py-20 text-center">
                <div class="relative w-24 h-24 rounded-full bg-slate-100 flex items-center justify-center mb-6">
                  <Message class="text-4xl text-slate-350" />
                  <div class="absolute inset-0 rounded-full border border-dashed border-slate-300 animate-spin-slow"></div>
                </div>
                <h3 class="text-base font-semibold text-slate-700">暂无聊天记录</h3>
                <p class="text-xs text-slate-400 mt-1.5 max-w-xs leading-relaxed">
                  您当前还没有与任何顾问或销售专员发起在线咨询。
                </p>
                <button 
                  class="mt-6 px-4 py-2 rounded-xl text-xs font-semibold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 active:scale-95 transition-all flex items-center gap-1.5 cursor-pointer"
                  @click="fetchSessions"
                >
                  <Refresh class="w-3.5 h-3.5" /> 刷新重试
                </button>
              </div>
              <div v-else class="space-y-3.5">
                <div
                  v-for="session in sessions"
                  :key="session.id"
                  class="group bg-white rounded-2xl p-5 border cursor-pointer transition-all duration-300 relative overflow-hidden flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 hover:-translate-y-0.5 hover:shadow-md border-slate-100 hover:border-slate-200 w-full min-w-0"
                  @click="openChatSession(session)"
                >
                  <div 
                    v-if="session.unreadCount > 0" 
                    class="absolute left-0 top-0 bottom-0 w-1 bg-indigo-500"
                  />

                  <div class="flex items-center gap-4 flex-1 min-w-0">
                    <div class="relative shrink-0">
                      <el-avatar 
                        :size="48" 
                        :src="getMemberAvatar(session)" 
                        class="bg-indigo-100 text-indigo-600 font-bold border border-slate-100 shadow-xs"
                      >
                        {{ getMemberName(session).substring(0, 1) }}
                      </el-avatar>
                      <span 
                        v-if="session.unreadCount > 0" 
                        class="absolute -top-1 -right-1 min-w-5 h-5 px-1.5 rounded-full bg-rose-500 text-white text-[10px] font-bold border border-white flex items-center justify-center"
                      >
                        {{ session.unreadCount }}
                      </span>
                    </div>

                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-1">
                        <h3 class="font-semibold text-slate-800 truncate leading-snug group-hover:text-slate-900">
                          {{ getMemberName(session) }}
                        </h3>
                        <span class="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-500 font-semibold scale-90 origin-left">
                          销售顾问
                        </span>
                      </div>
                      <p class="text-sm text-slate-500 truncate leading-relaxed">
                        {{ session.lastMessageContent || '暂无对话消息' }}
                      </p>
                    </div>
                  </div>

                  <div class="flex sm:flex-col items-end justify-between sm:justify-start gap-3 w-full sm:w-auto shrink-0 pt-3 sm:pt-0 border-t sm:border-t-0 border-slate-100">
                    <span class="text-xs text-slate-400">
                      {{ formatTime(session.lastMessageTime) }}
                    </span>
                    <div class="flex items-center gap-2">
                      <button 
                        class="text-xs text-rose-600 font-semibold bg-rose-50 hover:bg-rose-100 hover:text-rose-700 active:scale-95 transition-all flex items-center justify-center cursor-pointer p-2 rounded-xl border border-rose-150"
                        title="删除聊天记录"
                        @click.stop="handleDeleteSession(session)"
                      >
                        <Delete class="w-3.5 h-3.5" />
                      </button>
                      <button
                        class="text-xs text-indigo-600 font-semibold bg-indigo-50 hover:bg-indigo-100 hover:text-indigo-700 active:scale-95 transition-all flex items-center gap-1 cursor-pointer px-3 py-1.5 rounded-xl border border-indigo-150"
                        @click.stop="openChatSession(session)"
                      >
                        进入对话 <ArrowRight class="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页器美化 -->
          <div v-if="total > pageSize && currentTypeTab !== 5" class="flex justify-center border-t border-slate-100 pt-6 mt-8">
            <el-pagination
              v-model:current-page="pageNum"
              :total="total"
              :page-size="pageSize"
              layout="prev, pager, next"
              background
              class="!p-0 !m-0 custom-pagination"
              @current-change="fetchList"
            />
          </div>
        </div>

      </div>

    </div>

    <!-- 精美通知详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :show-close="true"
      align-center
      destroy-on-close
      class="custom-dialog"
      width="540px"
    >
      <template #header>
        <div v-if="selectedItem" class="flex items-center gap-3">
          <div 
            class="w-10 h-10 rounded-xl flex items-center justify-center shadow-xs animate-pulse"
            :class="getTypeConfig(selectedItem.noticeType).bgClass"
          >
            <component 
              :is="getTypeConfig(selectedItem.noticeType).icon" 
              class="w-5.5 h-5.5" 
              :class="getTypeConfig(selectedItem.noticeType).colorClass"
            />
          </div>
          <div>
            <span class="text-xs font-semibold text-slate-400">
              {{ getTypeConfig(selectedItem.noticeType).label }}
            </span>
            <div class="flex items-center gap-1.5 mt-0.5">
              <span class="text-sm font-semibold text-slate-800 truncate max-w-[320px]">
                {{ selectedItem.title }}
              </span>
              <el-tag 
                v-slot:default
                v-if="selectedItem.isRead === 1" 
                type="info" 
                size="small" 
                class="!rounded-md"
                effect="plain"
              >
                已读
              </el-tag>
            </div>
          </div>
        </div>
      </template>

      <!-- 详情正文排版 -->
      <div v-if="selectedItem" class="py-2">
        <div class="bg-slate-50 rounded-2xl p-5 border border-slate-200/50 min-h-[120px] text-slate-700 text-sm leading-relaxed whitespace-pre-wrap select-text">
          {{ selectedItem.content }}
        </div>
        
        <div class="flex items-center justify-between mt-6 text-xs text-slate-400 px-1">
          <div class="flex items-center gap-1">
            <User class="w-3.5 h-3.5 text-slate-355" />
            <span>发件人：{{ selectedItem.senderName }}</span>
          </div>
          <div class="flex items-center gap-1">
            <Clock class="w-3.5 h-3.5 text-slate-355" />
            <span>发送时间：{{ selectedItem.sendTime }}</span>
          </div>
        </div>
      </div>

      <!-- 底部动作条 -->
      <template #footer>
        <div class="flex justify-end gap-2 border-t border-slate-100 pt-4">
          <el-button 
            class="!rounded-xl" 
            @click="detailVisible = false"
          >
            关闭
          </el-button>
          <el-button
            v-if="selectedItem?.routerPath"
            type="primary"
            class="!rounded-xl !bg-indigo-600 hover:!bg-indigo-700 !border-indigo-600 hover:!border-indigo-700 cursor-pointer"
            @click="handleAction(selectedItem)"
          >
            <div class="flex items-center gap-1">
              <span>立即去处理</span>
              <ArrowRight class="w-3 h-3" />
            </div>
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Chat Dialog Component -->
    <ChatDialog v-model:visible="chatVisible" :session-id="chatSessionId" :target-name="chatTargetName" />
  </div>
</template>

<style>
/* 优雅的分页器与对话框深度美化定制 */
.custom-pagination .el-pager li {
  border-radius: 8px !important;
  background-color: #f1f5f9 !important;
  color: #475569 !important;
  font-weight: 600 !important;
  transition: all 0.2s;
  border: 1px solid transparent !important;
}
.custom-pagination .el-pager li:hover {
  color: #4f46e5 !important;
  background-color: #e0e7ff !important;
}
.custom-pagination .el-pager li.is-active {
  background-color: #4f46e5 !important;
  color: #ffffff !important;
  font-weight: 700 !important;
}
.custom-pagination .btn-prev, .custom-pagination .btn-next {
  border-radius: 8px !important;
  background-color: #f1f5f9 !important;
  border: 1px solid transparent !important;
  transition: all 0.2s;
}
.custom-pagination .btn-prev:hover, .custom-pagination .btn-next:hover {
  color: #4f46e5 !important;
  background-color: #e0e7ff !important;
}

.custom-dialog {
  border-radius: 24px !important;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-blur-md: 10px;
}
.custom-dialog .el-dialog__header {
  border-bottom: 1px solid #f1f5f9;
  padding: 20px 24px 16px !important;
  margin-right: 0 !important;
}
.custom-dialog .el-dialog__body {
  padding: 20px 24px !important;
}
.custom-dialog .el-dialog__footer {
  padding: 12px 24px 20px !important;
}

@keyframes spin-slow {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
.animate-spin-slow {
  animation: spin-slow 20s linear infinite;
}
</style>
