<script setup lang="ts">
import { ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import Navbar from './components/layout/Navbar.vue'
import ChatDialog from '@/components/chat/ChatDialog.vue'
import AiAssistantDialog from '@/components/chat/AiAssistantDialog.vue'
import { createSessionApi } from '@/api/chat'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useFavoritesStore } from '@/stores/favorites'
import { watch, onMounted } from 'vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const favoritesStore = useFavoritesStore()

const chatVisible = ref(false)
const aiVisible = ref(false)
const chatSessionId = ref<number | null>(null)

// 监听登录状态，自动拉取收藏
watch(() => authStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn) {
    favoritesStore.fetchFavorites()
  } else {
    favoritesStore.houseFavorites.clear()
    favoritesStore.projectFavorites.clear()
  }
}, { immediate: true })

onMounted(() => {
  if (authStore.isLoggedIn) {
    favoritesStore.fetchFavorites()
  }
})

// 开启 AI 咨询（第一步）
function openAiConsultation() {
  if (!authStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  aiVisible.value = true
}

// 从 AI 转接人工（由 AI 组件触发）
async function handleTransferHuman() {
  aiVisible.value = false
  try {
    // sessionType=1 私聊，members 必须同时包含双方
    const res = await createSessionApi({
      sessionType: 1,
      members: [
        { userType: 1, userId: 8 },                         // 指定经纪人或客服
        { userType: 2, userId: authStore.userInfo!.userId }, // 当前 C 端用户
      ]
    })
    chatSessionId.value = res.id
    chatVisible.value = true
  } catch (e) {
    console.error('转接人工失败', e)
    ElMessage.error('无法连接人工顾问，请稍后再试')
  }
}

// 保留原有的直接开启人工的方法（可选，如果某些场景需要直连）
async function openConsultation() {
  if (!authStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    const res = await createSessionApi({
      sessionType: 1,
      members: [
        { userType: 1, userId: 8 },
        { userType: 2, userId: authStore.userInfo!.userId },
      ]
    })
    chatSessionId.value = res.id
    chatVisible.value = true
  } catch (e) {
    console.error('开启咨询失败', e)
    ElMessage.error('创建咨询会话失败，请稍后重试')
  }
}
</script>

<template>
  <div class="app-container">
    <Navbar />
    <RouterView />

    <!-- Floating Consultation Button (Hide on Login page) -->
    <div v-if="route.path !== '/login'" class="fixed bottom-10 right-10 z-[100] group flex flex-col items-end gap-3">
      <!-- Tooltip / Greeting -->
      <div
        class="bg-white px-4 py-2 rounded-xl shadow-xl border border-gray-100 text-xs font-bold text-gray-700 animate-bounce-slow opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
        我是 AI 助手，有什么可以帮您？
      </div>

      <button @click="openAiConsultation"
        class="w-16 h-16 bg-linear-to-tr from-blue-600 to-indigo-600 rounded-2xl flex items-center justify-center text-white shadow-[0_20px_50px_rgba(37,99,235,0.3)] hover:shadow-[0_20px_50px_rgba(37,99,235,0.5)] transition-all duration-300 hover:scale-110 active:scale-95 group relative">
        <div class="absolute inset-0 bg-white/20 rounded-2xl scale-0 group-hover:scale-100 transition-transform duration-500"></div>
        <el-icon :size="32" class="group-hover:rotate-12 transition-transform z-10">
          <Cpu />
        </el-icon>
        <span
          class="absolute -top-1 -right-1 w-4 h-4 bg-green-400 rounded-full border-2 border-white animate-pulse"></span>
      </button>
    </div>

    <!-- AI Assistant Dialog -->
    <AiAssistantDialog v-model:visible="aiVisible" @transfer-human="handleTransferHuman" />

    <!-- Chat Dialog Component -->
    <ChatDialog v-model:visible="chatVisible" :session-id="chatSessionId" target-name="平台客服" />
  </div>
</template>

<style>
/* Global resets */
body,
html {
  margin: 0;
  padding: 0;
  width: 100%;
  overflow-x: hidden;
}

.app-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
