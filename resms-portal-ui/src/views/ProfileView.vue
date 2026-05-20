<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Star, Calendar, Setting, Clock, ChatDotRound, InfoFilled, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getUserProfileApi, type UserProfile } from '@/api/user'

const router = useRouter()
const authStore = useAuthStore()
const profile = ref<UserProfile | null>(null)
const loading = ref(false)
const loadError = ref(false)

async function fetchProfile() {
  loading.value = true
  loadError.value = false
  try {
    profile.value = await getUserProfileApi()
  } catch (e) {
    console.error('获取用户信息失败', e)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function maskPhone(phone: string) {
  return phone?.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') || ''
}

async function handleLogout() {
  await authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

const menuItems = [
  { icon: Star, label: '我的收藏', path: '/favorites', desc: '查看收藏的房源' },
  { icon: Calendar, label: '预约记录', path: '/appointments', desc: '管理看房预约' },
  { icon: Setting, label: '账号设置', path: '/settings', desc: '修改个人资料' },
  { icon: Clock, label: '浏览记录', path: '/history', desc: '最近浏览的房源' },
  { icon: ChatDotRound, label: '意见反馈', path: '/feedback', desc: '提交问题与建议' },
  { icon: InfoFilled, label: '关于我们', path: '/about', desc: '了解平台信息' },
]

onMounted(fetchProfile)
</script>

<template>
  <div class="min-h-screen bg-slate-50/50 pb-20">
    <!-- 顶部用户信息卡片 -->
    <div class="bg-gradient-to-br from-primary to-blue-600 pt-28 pb-10 px-6">
      <div class="max-w-3xl mx-auto">
        <div v-loading="loading" class="flex items-center gap-5">
          <!-- 加载骨架 -->
          <template v-if="loading">
            <div class="w-20 h-20 rounded-2xl bg-white/10 animate-pulse" />
            <div class="flex flex-col gap-2">
              <div class="w-32 h-6 bg-white/10 rounded animate-pulse" />
              <div class="w-24 h-4 bg-white/10 rounded animate-pulse" />
            </div>
          </template>
          <!-- 加载失败 -->
          <template v-else-if="loadError">
            <div class="text-center w-full py-4">
              <p class="text-white/70 text-sm mb-2">加载失败</p>
              <button
                class="px-6 py-1.5 bg-white/20 rounded-xl text-sm text-white font-semibold hover:bg-white/30 transition-colors"
                @click="fetchProfile"
              >
                重新加载
              </button>
            </div>
          </template>
          <!-- 正常显示 -->
          <template v-else>
            <!-- 头像 -->
            <div
              class="w-20 h-20 rounded-2xl border-3 border-white/30 overflow-hidden shadow-xl cursor-pointer hover:scale-105 transition-transform bg-white"
              @click="router.push('/settings')"
            >
              <img
                :src="profile?.avatarUrl || authStore.userInfo?.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'"
                alt="avatar"
                class="w-full h-full object-cover"
              />
            </div>
            <!-- 用户信息 -->
            <div class="flex flex-col gap-1 text-white">
              <div class="flex items-center gap-2">
                <span class="text-2xl font-bold tracking-tight">
                  {{ profile?.nickname || authStore.userInfo?.nickname || '用户' }}
                </span>
                <span class="px-2 py-0.5 bg-white/20 text-[10px] font-bold rounded-lg uppercase backdrop-blur-sm">
                  PRO
                </span>
              </div>
              <span class="text-sm text-white/70">
                {{ maskPhone(profile?.phone || authStore.userInfo?.phone || '') }}
              </span>
            </div>
          </template>
        </div>

        <!-- 统计数据 -->
        <div v-if="!loadError" class="mt-8 grid grid-cols-3 gap-4">
          <div v-loading="loading" element-loading-background="rgba(255,255,255,0.05)" class="bg-white/10 backdrop-blur-md rounded-2xl px-5 py-4 text-center cursor-pointer hover:bg-white/20 transition-colors" @click="router.push('/favorites')">
            <div class="text-2xl font-bold text-white">{{ profile?.favoriteCount ?? '-' }}</div>
            <div class="text-xs text-white/60 mt-1">收藏房源</div>
          </div>
          <div v-loading="loading" element-loading-background="rgba(255,255,255,0.05)" class="bg-white/10 backdrop-blur-md rounded-2xl px-5 py-4 text-center cursor-pointer hover:bg-white/20 transition-colors" @click="router.push('/appointments')">
            <div class="text-2xl font-bold text-white">{{ profile?.appointmentCount ?? '-' }}</div>
            <div class="text-xs text-white/60 mt-1">预约记录</div>
          </div>
          <div v-loading="loading" element-loading-background="rgba(255,255,255,0.05)" class="bg-white/10 backdrop-blur-md rounded-2xl px-5 py-4 text-center" @click="router.push('/history')">
            <div class="text-2xl font-bold text-white">{{ profile?.browseCount ?? '-' }}</div>
            <div class="text-xs text-white/60 mt-1">浏览记录</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 功能入口网格 -->
    <div class="max-w-3xl mx-auto px-6 -mt-4">
      <div class="bg-white rounded-[32px] shadow-lg p-6">
        <div class="grid grid-cols-3 gap-4">
          <div
            v-for="item in menuItems"
            :key="item.path"
            class="flex flex-col items-center gap-2 py-4 rounded-2xl cursor-pointer transition-all hover:bg-slate-50 hover:scale-105 group"
            @click="router.push(item.path)"
          >
            <div class="w-12 h-12 rounded-xl bg-primary/5 flex items-center justify-center text-primary group-hover:bg-primary/10 transition-colors">
              <el-icon :size="22"><component :is="item.icon" /></el-icon>
            </div>
            <span class="text-sm font-semibold text-slate-700">{{ item.label }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 退出登录 -->
    <div class="max-w-3xl mx-auto px-6 mt-6">
      <el-button
        class="w-full !h-12 !rounded-2xl !font-bold !text-base"
        @click="handleLogout"
      >
        <el-icon class="mr-2"><SwitchButton /></el-icon>
        退出登录
      </el-button>
    </div>
  </div>
</template>
