<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Management, Monitor, Expand, Fold,
  User, Key, SwitchButton, ArrowDown, Bell,
  ChatDotSquare, Message
} from '@element-plus/icons-vue'
import { getUnreadCount, myNotifications, markNotificationRead, markAllNotificationsRead } from '@/api/message/user-notification'
import { listSessions, readSession } from '@/api/message/chat'
import { wsService } from '@/utils/WebSocketService'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const sidebarCollapsed = ref(false)

// 动态获取菜单数据
const menuList = computed(() => {
  const list = [...userStore.menuList]
  const hasDashboard = list.some(m => m.path === '/dashboard' || m.path === 'dashboard')
  if (!hasDashboard) {
    list.unshift({ id: -1, menuName: '控制台', path: '/dashboard', icon: 'Monitor', children: [] })
  }
  return list
})

const resolvePath = (parentPath: string, childPath: string) => {
  if (childPath.startsWith('/')) return childPath
  const parent = parentPath.startsWith('/') ? parentPath : '/' + parentPath
  return `${parent}${parent.endsWith('/') ? '' : '/'}${childPath}`
}

// 面包屑
const breadcrumbs = computed(() => {
  return route.matched.filter(r => r.meta?.title)
})

// 用户信息
const userName = computed(() => {
  return userStore.userInfo?.nickName || userStore.userInfo?.realName || userStore.userInfo?.username || '管理员'
})

const userAvatar = computed(() => {
  return userStore.userInfo?.avatar || ''
})

// 方法
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.clearToken()
    router.push('/login')
  })
}

const handleProfile = () => {
  router.push('/system/user/profile')
}

const handlePassword = () => {
  router.push({ path: '/system/user/profile', query: { tab: 'security' } })
}

// --- 消息中心 (通知 + 聊天) ---
const activeNotifyTab = ref<'notice' | 'chat'>('notice')
const unreadCount = ref(0)
const notifications = ref<any[]>([])
const chatSessions = ref<any[]>([])
const chatUnreadTotal = ref(0)
const notifyPopoverVisible = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

const totalUnread = computed(() => unreadCount.value + chatUnreadTotal.value)

const fetchUnreadCount = async () => {
  try {
    const [notifyRes, sessionsRes]: any = await Promise.all([
      getUnreadCount().catch(() => ({ data: 0 })),
      listSessions().catch(() => ({ data: [] }))
    ])
    unreadCount.value = notifyRes.data ?? 0
    chatSessions.value = sessionsRes.data || []
    chatUnreadTotal.value = chatSessions.value.reduce((sum: number, s: any) => sum + (s.unreadCount || 0), 0)
  } catch {
    // 静默处理
  }
}

const fetchNotifications = async () => {
  try {
    const res: any = await myNotifications(1, 10)
    notifications.value = res.data?.records || []
  } catch {
    notifications.value = []
  }
}

watch(notifyPopoverVisible, (val) => {
  if (val) {
    fetchNotifications()
    fetchUnreadCount()
  }
})

const handleMarkRead = async (id: number) => {
  try {
    await markNotificationRead(id)
    await fetchUnreadCount()
    await fetchNotifications()
  } catch {
    // 静默处理
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllNotificationsRead()
    unreadCount.value = 0
    await fetchNotifications()
  } catch {
    // 静默处理
  }
}

const timeAgo = (timeStr: string) => {
  if (!timeStr) return ''
  const now = Date.now()
  const t = new Date(timeStr).getTime()
  const diff = Math.floor((now - t) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 2592000) return `${Math.floor(diff / 86400)}天前`
  return timeStr.slice(0, 10)
}

/** 从菜单树中查找路径 */
const findMenuPath = (menus: any[], keyword: string, parentPath = ''): string | null => {
  for (const m of menus) {
    const fullPath = parentPath
      ? `${parentPath}${parentPath.endsWith('/') ? '' : '/'}${m.path}`
      : (m.path?.startsWith('/') ? m.path : (m.path ? '/' + m.path : ''))
    if (m.menuName?.includes(keyword) || m.component?.includes(keyword)) return fullPath
    if (m.children?.length) {
      const found = findMenuPath(m.children, keyword, fullPath)
      if (found) return found
    }
  }
  return null
}

const goNoticePage = () => {
  const path = findMenuPath(userStore.menuList, '通知') || findMenuPath(userStore.menuList, '公告') || '/message/notice'
  notifyPopoverVisible.value = false
  if (path) router.push(path)
}

const goChatPage = () => {
  const path = findMenuPath(userStore.menuList, '聊天') || findMenuPath(userStore.menuList, '消息') || '/message/chat'
  notifyPopoverVisible.value = false
  if (path) router.push(path)
}

const openChatSession = async (session: any) => {
  try {
    await readSession(session.id)
  } catch { /* ignore */ }
  goChatPage()
}

onMounted(() => {
  fetchUnreadCount()
  pollTimer = setInterval(fetchUnreadCount, 60000)

  // 全局 WebSocket 连接（用户在线状态、通知推送）
  wsService.connect()

  // 监听通知推送（可选，通知模块后续可发 NOTIFICATION 类型消息）
  wsService.on('NOTIFICATION', () => {
    fetchUnreadCount()
  })
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  wsService.disconnect()
})
</script>

<template>
  <div class="common-layout h-full">
    <el-container class="h-full">
      <!-- 侧边栏 -->
      <el-aside
        :width="sidebarCollapsed ? '64px' : '256px'"
        class="bg-[#f8f9fa] border-r border-gray-200 overflow-hidden flex flex-col transition-all duration-300"
      >
        <div class="h-16 flex items-center justify-center px-4 mb-2 overflow-hidden">
          <el-icon class="text-[#1a73e8] shrink-0" :size="24"><Management /></el-icon>
          <transition name="el-fade-in-linear">
            <span v-show="!sidebarCollapsed" class="text-[20px] font-medium tracking-tight text-[#1f1f1f] ml-3 whitespace-nowrap">RESMS Admin</span>
          </transition>
        </div>

        <el-scrollbar class="flex-1">
          <el-menu
            :default-active="route.path"
            :collapse="sidebarCollapsed"
            active-text-color="#041e49"
            background-color="transparent"
            class="el-menu-vertical border-none custom-sidebar-menu"
            text-color="#444746"
            router
            unique-opened
          >
            <template v-for="menu in menuList" :key="menu.id">
              <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="menu.path">
                <template #title>
                  <el-icon :size="20"><component :is="menu.icon" /></el-icon>
                  <span class="ml-1">{{ menu.menuName }}</span>
                </template>
                <el-menu-item
                  v-for="child in menu.children"
                  :key="child.id"
                  :index="resolvePath(menu.path, child.path)"
                >
                  <el-icon :size="20"><component :is="child.icon" /></el-icon>
                  <span class="ml-1">{{ child.menuName }}</span>
                </el-menu-item>
              </el-sub-menu>

              <el-menu-item v-else :index="menu.path.startsWith('/') ? menu.path : '/' + menu.path">
                <el-icon :size="20"><component :is="menu.icon" /></el-icon>
                <span class="ml-1">{{ menu.menuName }}</span>
              </el-menu-item>
            </template>
          </el-menu>
        </el-scrollbar>
      </el-aside>

      <el-container>
        <!-- 顶部导航栏 -->
        <el-header class="bg-white border-b flex items-center justify-between px-6 shadow-sm" style="height: 64px">
          <!-- 左侧：折叠按钮 + 面包屑 -->
          <div class="flex items-center gap-4 overflow-hidden">
            <el-button :icon="sidebarCollapsed ? Expand : Fold" text class="!text-gray-500 !text-lg shrink-0" @click="toggleSidebar" />
            <el-breadcrumb separator="/" class="hidden sm:flex whitespace-nowrap">
              <el-breadcrumb-item
                v-for="(crumb, idx) in breadcrumbs"
                :key="crumb.path"
                :to="idx < breadcrumbs.length - 1 ? crumb.path : undefined"
              >
                {{ crumb.meta?.title }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>

          <!-- 右侧：工具栏 + 通知 + 用户信息 -->
          <div class="flex items-center gap-1 shrink-0">
            <!-- 消息中心按钮 (通知 + 聊天) -->
            <el-popover
              v-model:visible="notifyPopoverVisible"
              placement="bottom-end"
              :width="380"
              trigger="click"
              popper-class="notify-popover"
            >
              <template #reference>
                <el-badge :value="totalUnread" :max="99" :hidden="totalUnread === 0" class="!align-middle">
                  <el-button text class="!text-gray-500 !text-lg relative">
                    <el-icon><Bell /></el-icon>
                  </el-button>
                </el-badge>
              </template>

              <div class="notify-panel">
                <!-- 选项卡头部 -->
                <div class="flex items-center border-b border-gray-100">
                  <div
                    class="flex-1 px-4 py-3 text-center text-sm font-bold cursor-pointer transition-colors"
                    :class="activeNotifyTab === 'notice' ? 'text-[#1a73e8] border-b-2 border-[#1a73e8]' : 'text-gray-500 hover:text-gray-700'"
                    @click="activeNotifyTab = 'notice'"
                  >
                    通知
                    <span v-if="unreadCount > 0" class="ml-1 text-[10px] bg-[#1a73e8] text-white rounded-full px-1.5">{{ unreadCount }}</span>
                  </div>
                  <div
                    class="flex-1 px-4 py-3 text-center text-sm font-bold cursor-pointer transition-colors"
                    :class="activeNotifyTab === 'chat' ? 'text-[#1a73e8] border-b-2 border-[#1a73e8]' : 'text-gray-500 hover:text-gray-700'"
                    @click="activeNotifyTab = 'chat'"
                  >
                    消息
                    <span v-if="chatUnreadTotal > 0" class="ml-1 text-[10px] bg-[#1a73e8] text-white rounded-full px-1.5">{{ chatUnreadTotal }}</span>
                  </div>
                  <el-button link type="info" size="small" class="!mr-2" @click="notifyPopoverVisible = false">
                    关闭
                  </el-button>
                </div>

                <!-- 通知标签 -->
                <div v-show="activeNotifyTab === 'notice'" class="max-h-[360px] overflow-y-auto">
                  <div v-if="unreadCount > 0" class="px-4 pt-2 text-right">
                    <el-button link type="primary" size="small" @click="handleMarkAllRead">全部已读</el-button>
                  </div>
                  <div v-if="notifications.length === 0" class="flex flex-col items-center py-10 text-gray-400">
                    <el-icon :size="36" class="mb-2"><Bell /></el-icon>
                    <span class="text-sm">暂无通知</span>
                  </div>
                  <div
                    v-for="item in notifications"
                    :key="item.id"
                    class="flex items-start gap-3 px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors"
                    :class="item.isRead === 0 ? 'bg-blue-50/40' : ''"
                    @click="handleMarkRead(item.id)"
                  >
                    <div class="mt-1 shrink-0">
                      <div class="w-2 h-2 rounded-full" :class="item.isRead === 0 ? 'bg-[#1a73e8]' : 'bg-gray-300'" />
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center justify-between gap-2">
                        <span class="text-sm font-medium truncate" :class="item.isRead === 0 ? 'text-gray-800' : 'text-gray-500'">
                          {{ item.title }}
                        </span>
                        <span class="text-[10px] text-gray-400 shrink-0">{{ timeAgo(item.sendTime) }}</span>
                      </div>
                      <p class="text-xs text-gray-400 mt-1 line-clamp-2">{{ item.content }}</p>
                    </div>
                  </div>
                  <div class="border-t border-gray-100 px-4 py-2 text-center">
                    <el-button link type="primary" size="small" @click="goNoticePage">查看全部通知</el-button>
                  </div>
                </div>

                <!-- 聊天标签 -->
                <div v-show="activeNotifyTab === 'chat'" class="max-h-[360px] overflow-y-auto">
                  <div v-if="chatSessions.length === 0" class="flex flex-col items-center py-10 text-gray-400">
                    <el-icon :size="36" class="mb-2"><ChatDotSquare /></el-icon>
                    <span class="text-sm">暂无消息</span>
                  </div>
                  <div
                    v-for="s in chatSessions"
                    :key="s.id"
                    class="flex items-center gap-3 px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors"
                    @click="openChatSession(s)"
                  >
                    <el-badge :is-dot="s.unreadCount > 0" class="shrink-0">
                      <el-avatar :size="36" class="!bg-blue-100 !text-blue-600 font-bold text-sm">
                        {{ (s.sessionName || '用')?.charAt(0)?.toUpperCase() }}
                      </el-avatar>
                    </el-badge>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center justify-between gap-2">
                        <span class="text-sm font-medium text-gray-800 truncate">{{ s.sessionName || '未知会话' }}</span>
                        <span class="text-[10px] text-gray-400 shrink-0">{{ timeAgo(s.lastMessageTime) }}</span>
                      </div>
                      <p class="text-xs text-gray-500 truncate mt-0.5">{{ s.lastMessageContent || '' }}</p>
                    </div>
                    <div v-if="s.unreadCount > 0" class="shrink-0">
                      <span class="text-[10px] bg-[#1a73e8] text-white rounded-full px-1.5">{{ s.unreadCount > 99 ? '99+' : s.unreadCount }}</span>
                    </div>
                  </div>
                  <div class="border-t border-gray-100 px-4 py-2 text-center">
                    <el-button link type="primary" size="small" @click="goChatPage">查看全部消息</el-button>
                  </div>
                </div>
              </div>
            </el-popover>

            <el-divider direction="vertical" class="!mx-2 !h-6" />

            <!-- 用户下拉 -->
            <el-dropdown trigger="click">
              <div class="flex items-center gap-2 cursor-pointer px-2 py-1 rounded-xl hover:bg-gray-100 transition-colors">
                <el-avatar :size="32" :src="userAvatar" class="bg-[#1a73e8] shrink-0">
                  <template v-if="!userAvatar">{{ userName.charAt(0).toUpperCase() }}</template>
                </el-avatar>
                <span class="text-sm font-medium text-gray-700 hidden sm:inline">{{ userName }}</span>
                <el-icon class="text-gray-400 text-xs"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :icon="User" @click="handleProfile">个人中心</el-dropdown-item>
                  <el-dropdown-item :icon="Key" @click="handlePassword">修改密码</el-dropdown-item>
                  <el-dropdown-item divided :icon="SwitchButton" @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 主内容区 -->
        <el-main class="bg-[#f8f9fa]/50">
          <router-view v-slot="{ Component }">
            <transition name="fade-transform" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
/* 侧边栏 Material 3 风格 */
.custom-sidebar-menu {
  background-color: transparent !important;
  padding: 0 12px;
}

.custom-sidebar-menu :deep(.el-menu-item),
.custom-sidebar-menu :deep(.el-sub-menu__title) {
  height: 48px !important;
  line-height: 48px !important;
  margin: 2px 0 !important;
  border-radius: 24px !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  color: #444746 !important;
  padding: 0 12px !important;
}

.custom-sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: #d3e3fd !important;
  color: #041e49 !important;
  font-weight: 500;
}

.custom-sidebar-menu :deep(.el-menu-item:hover),
.custom-sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: #f0f4f9 !important;
  color: #1f1f1f !important;
}

.custom-sidebar-menu :deep(.el-menu-item .el-icon),
.custom-sidebar-menu :deep(.el-sub-menu__title .el-icon) {
  color: #444746 !important;
  margin-right: 8px !important;
  transition: color 0.2s;
}

.custom-sidebar-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #041e49 !important;
}

.custom-sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  padding-left: 42px !important;
}

/* 折叠状态下图标居中 */
.custom-sidebar-menu :deep(.el-menu--collapse .el-menu-item),
.custom-sidebar-menu :deep(.el-menu--collapse .el-sub-menu__title) {
  padding: 0 12px !important;
  justify-content: center;
}

/* 面包屑字号 */
:deep(.el-breadcrumb__inner) {
  font-weight: 500;
  color: #5f6368 !important;
}

:deep(.el-breadcrumb__inner.is-link) {
  color: #9aa0a6 !important;
}

/* 通知面板全局样式 */
:deep(.notify-popover) {
  padding: 0 !important;
  border-radius: 16px !important;
  overflow: hidden;
}

/* 时间轴连线隐藏 */
:deep(.el-timeline-item__tail) {
  border-left: 2px dashed #e8eaed;
}

:deep(.el-timeline-item__node) {
  background-color: #fff;
  border: 2px solid currentColor;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
