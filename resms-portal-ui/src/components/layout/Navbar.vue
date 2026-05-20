<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Location, ArrowDown, ArrowRight, House, MapLocation, User, Star, Calendar, Setting, SwitchButton, Bell } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getUnreadCount } from '@/api/notification'
import groupData from '@/assets/group.json'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const isScrolled = ref(false)

// 省市数据
const provinces = groupData.data
const selectedProvince = ref(localStorage.getItem('resms-selected-province') || '')
const selectedCity = ref(localStorage.getItem('resms-selected-city') || '广州市')
const locationVisible = ref(false)
const searchQuery = ref('')

// 热门城市
const hotCities = [
  { label: '全国', value: '全国' },
  { label: '北京', value: '北京市' },
  { label: '上海', value: '上海市' },
  { label: '广州', value: '广州市' },
  { label: '深圳', value: '深圳市' },
  { label: '成都', value: '成都市' },
  { label: '杭州', value: '杭州市' },
  { label: '武汉', value: '武汉市' },
  { label: '南京', value: '南京市' }
]

// 所有城市的扁平列表，用于搜索
const allCities = computed(() => {
  const cities: { label: string; value: string; province: string }[] = []
  provinces.forEach(p => {
    // 1. 添加省份/直辖市本身 (有些用户可能直接搜北京)
    cities.push({ label: p.label, value: p.value, province: '' })
    
    p.children.forEach(c => {
      // 2. 添加二级城市 (如: 成都市, 或者直辖市下的 "市辖区" - 过滤掉"市辖区"这种无意义标签)
      if (!c.label.includes('市辖区')) {
        cities.push({ label: c.label, value: c.value, province: p.label })
      }
      
      // 3. 添加三级区县
      if (c.children && c.children.length > 0) {
        c.children.forEach(d => {
          cities.push({ label: d.label, value: d.value, province: `${p.label} ${c.label.replace('市辖区', '')}` })
        })
      }
    })
  })
  return cities
})

// 搜索过滤后的城市
const filteredCities = computed(() => {
  if (!searchQuery.value) return []
  return allCities.value.filter(c => 
    c.label.includes(searchQuery.value) || 
    c.province.includes(searchQuery.value)
  ).slice(0, 10) // 限制显示数量
})

const currentCities = computed(() => {
  const province = provinces.find(p => p.value === selectedProvince.value)
  return province ? province.children : []
})

function selectProvince(value: string) {
  selectedProvince.value = value
  selectedCity.value = ''
}

function selectCity(value: string) {
  selectedCity.value = value
  if (value === '全国') {
    selectedProvince.value = ''
  }
  localStorage.setItem('resms-selected-city', value)
  localStorage.setItem('resms-selected-province', selectedProvince.value)
  locationVisible.value = false
  searchQuery.value = ''
}

const displayLocation = computed(() => {
  if (selectedCity.value === '全国') return '全国'
  if (selectedCity.value) return selectedCity.value.replace(/市$/, '')
  if (selectedProvince.value) return selectedProvince.value.replace(/省$|市$/, '')
  return '全国' // 默认回退到全国
})

// 判断是否为沉浸式模式（仅首页且未滚动时）
const isImmersive = computed(() => {
  return route.path === '/' && !isScrolled.value
})

const handleScroll = () => {
  isScrolled.value = window.scrollY > 50
}

const unreadCount = ref(0)
let unreadTimer: ReturnType<typeof setInterval> | null = null

async function fetchUnread() {
  if (!authStore.isLoggedIn) return
  try {
    const res = await getUnreadCount()
    unreadCount.value = typeof res === 'number' ? res : (res as any)?.data ?? 0
  } catch { /* ignore */ }
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  if (authStore.isLoggedIn) {
    fetchUnread()
    unreadTimer = setInterval(fetchUnread, 60000)
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (unreadTimer) clearInterval(unreadTimer)
})

async function handleLogout() {
  await authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <nav
    :class="[
      'fixed top-0 left-0 w-full z-50 transition-all duration-300 px-6 py-4 flex items-center justify-between',
      isImmersive ? 'bg-transparent text-white' : 'bg-white/90 backdrop-blur-md shadow-md py-3 text-gray-800'
    ]"
  >
    <!-- Left Side: Logo & Location -->
    <div class="flex items-center gap-8">
      <!-- Logo -->
      <div class="flex items-center gap-2 cursor-pointer group" @click="router.push('/')">
        <div class="w-10 h-10 bg-primary rounded-xl flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
          <el-icon :size="24" color="white"><House /></el-icon>
        </div>
        <span class="text-xl font-bold tracking-tight" :class="!isImmersive ? 'text-primary' : 'text-white'">
          RESMS <span class="font-light opacity-80">Portal</span>
        </span>
      </div>

      <!-- Location Selector -->
      <div class="hidden sm:block">
        <el-popover 
          trigger="click" 
          :width="420" 
          v-model:visible="locationVisible"
          popper-class="location-popover"
          :show-after="0"
          :hide-after="0"
        >
          <template #reference>
            <div
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-full transition-all cursor-pointer group"
              :class="!isImmersive ? 'bg-gray-100 text-gray-700 hover:bg-gray-200' : 'bg-white/10 text-white hover:bg-white/20 backdrop-blur-md border border-white/10'"
            >
              <el-icon class="group-hover:animate-bounce"><Location /></el-icon>
              <span class="text-sm font-bold">{{ displayLocation }}</span>
              <el-icon :size="12" class="opacity-50 transition-transform group-hover:rotate-180"><ArrowDown /></el-icon>
            </div>
          </template>

          <div class="p-2 space-y-4">
            <!-- Search Area -->
            <div class="relative">
              <el-input
                v-model="searchQuery"
                placeholder="搜索城市 (如: 北京、成都)"
                :prefix-icon="Search"
                clearable
                class="location-search-input"
              />
              
              <!-- Search Results Dropdown -->
              <div v-if="searchQuery && filteredCities.length > 0" 
                class="absolute z-10 left-0 right-0 mt-1 bg-white border border-gray-100 shadow-xl rounded-lg overflow-hidden py-1"
              >
                <div
                  v-for="city in filteredCities"
                  :key="city.value"
                  class="px-4 py-2 hover:bg-blue-50 cursor-pointer flex items-center justify-between transition-colors"
                  @click="selectCity(city.value)"
                >
                  <span class="text-sm text-gray-700">{{ city.label }}</span>
                  <span class="text-xs text-gray-400">{{ city.province }}</span>
                </div>
              </div>
            </div>

            <!-- Default Option -->
            <div v-if="!searchQuery" class="mb-4">
              <div class="text-[10px] uppercase tracking-wider text-gray-400 font-bold mb-2 px-1">默认选项</div>
              <div
                class="inline-flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-xl border border-gray-100 bg-gray-50 text-gray-600 hover:border-primary hover:text-primary hover:bg-blue-50 cursor-pointer transition-all group"
                :class="{ 'border-primary text-primary bg-blue-50': selectedCity === '全国' }"
                @click="selectCity('全国')"
              >
                <el-icon class="text-lg" :class="selectedCity === '全国' ? 'text-primary' : 'text-gray-400'"><MapLocation /></el-icon>
                <span>全国范围</span>
              </div>
            </div>

            <!-- Hot Cities -->
            <div v-if="!searchQuery">
              <div class="text-[10px] uppercase tracking-wider text-gray-400 font-bold mb-2 px-1">热门城市</div>
              <div class="flex flex-wrap gap-2">
                <template v-for="city in hotCities" :key="city.value">
                  <div
                    v-if="city.value !== '全国'"
                    class="px-3 py-1 text-xs rounded-full border border-gray-100 bg-gray-50 text-gray-600 hover:border-primary hover:text-primary hover:bg-blue-50 cursor-pointer transition-all"
                    :class="{ 'border-primary text-primary bg-blue-50': selectedCity === city.value }"
                    @click="selectCity(city.value)"
                  >
                    {{ city.label }}
                  </div>
                </template>
              </div>
            </div>

            <!-- Cascade Selector -->
            <div v-if="!searchQuery" class="flex h-[280px] border border-gray-100 rounded-lg overflow-hidden">
              <!-- 省列表 -->
              <div class="w-1/2 overflow-y-auto bg-gray-50/50 border-r border-gray-100 custom-scrollbar">
                <div class="sticky top-0 bg-gray-50/80 backdrop-blur-sm px-3 py-1.5 text-[10px] font-bold text-gray-400 uppercase tracking-wider border-b border-gray-100/50">
                  省份 / 直辖市
                </div>
                <div
                  v-for="province in provinces"
                  :key="province.value"
                  class="px-4 py-2.5 text-sm cursor-pointer transition-all flex items-center justify-between group/item"
                  :class="selectedProvince === province.value ? 'text-primary font-bold bg-white' : 'text-gray-600 hover:bg-white/50'"
                  @click="selectProvince(province.value)"
                >
                  <span>{{ province.label.replace(/省$|市$/, '') }}</span>
                  <el-icon v-if="selectedProvince === province.value" :size="14"><ArrowRight /></el-icon>
                </div>
              </div>
              <!-- 市列表 -->
              <div class="w-1/2 overflow-y-auto bg-white custom-scrollbar">
                <div class="sticky top-0 bg-white/80 backdrop-blur-sm px-3 py-1.5 text-[10px] font-bold text-gray-400 uppercase tracking-wider border-b border-gray-100/50">
                  选择城市
                </div>
                <div v-if="selectedProvince">
                  <div
                    v-for="city in currentCities"
                    :key="city.value"
                    class="px-4 py-2.5 text-sm cursor-pointer transition-all hover:text-primary hover:bg-blue-50/30"
                    :class="selectedCity === city.value ? 'text-primary font-bold bg-blue-50/50' : 'text-gray-600'"
                    @click="selectCity(city.value)"
                  >
                    {{ city.label.replace(/市$/, '') }}
                  </div>
                </div>
                <div v-else class="h-full flex flex-col items-center justify-center text-gray-300 space-y-2">
                  <el-icon :size="32" class="opacity-20"><MapLocation /></el-icon>
                  <span class="text-xs">请先选择省份</span>
                </div>
              </div>
            </div>
          </div>
        </el-popover>
      </div>
    </div>

    <!-- Menu Items: Centered for better visual balance -->
    <div class="hidden md:flex flex-1 items-center justify-center gap-12 text-sm font-bold tracking-wide">
      <router-link
        to="/new-house"
        :class="[
          'transition-all hover:scale-110 px-4 py-2 rounded-full',
          !isImmersive 
            ? (route.path === '/new-house' ? 'text-primary bg-primary/5 shadow-sm' : 'text-slate-600 hover:text-primary hover:bg-primary/5') 
            : (route.path === '/new-house' ? 'text-white bg-white/20' : 'text-white/80 hover:text-white hover:bg-white/10')
        ]"
      >
        新房
      </router-link>
      <router-link
        to="/second-hand"
        :class="[
          'transition-all hover:scale-110 px-4 py-2 rounded-full',
          !isImmersive 
            ? (route.path === '/second-hand' ? 'text-primary bg-primary/5 shadow-sm' : 'text-slate-600 hover:text-primary hover:bg-primary/5') 
            : (route.path === '/second-hand' ? 'text-white bg-white/20' : 'text-white/80 hover:text-white hover:bg-white/10')
        ]"
      >
        二手房
      </router-link>
      <router-link
        to="/rent"
        :class="[
          'transition-all hover:scale-110 px-4 py-2 rounded-full',
          !isImmersive 
            ? (route.path === '/rent' ? 'text-primary bg-primary/5 shadow-sm' : 'text-slate-600 hover:text-primary hover:bg-primary/5') 
            : (route.path === '/rent' ? 'text-white bg-white/20' : 'text-white/80 hover:text-white hover:bg-white/10')
        ]"
      >
        租房
      </router-link>
    </div>

    <!-- Right Side Actions -->
    <div class="flex items-center gap-4">
      <!-- 已登录：通知铃铛 -->
      <router-link v-if="authStore.isLoggedIn" to="/notifications" class="relative mr-1 p-2 rounded-full hover:bg-gray-100 transition-colors">
        <el-icon :size="20" class="text-gray-600"><Bell /></el-icon>
        <span
          v-if="unreadCount > 0"
          class="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] flex items-center justify-center bg-red-500 text-white text-[10px] font-bold rounded-full px-1"
        >{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </router-link>

      <!-- 已登录：显示用户信息 -->
      <el-dropdown v-if="authStore.isLoggedIn" trigger="hover" popper-class="user-dropdown-popper">
        <div
          class="w-10 h-10 rounded-full border-2 border-white/20 overflow-hidden cursor-pointer hover:border-primary hover:ring-4 hover:ring-primary/10 transition-all shadow-md active:scale-90"
        >
          <img
            v-if="authStore.userInfo?.avatarUrl"
            :src="authStore.userInfo.avatarUrl"
            alt="avatar"
            class="w-full h-full object-cover transition-transform hover:scale-110"
          />
          <img v-else src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" alt="avatar" class="w-full h-full object-cover" />
        </div>
        <template #dropdown>
          <div class="user-dropdown-card w-72 bg-white overflow-hidden">
            <!-- User Info Header: MD3 High-Contrast Style -->
            <div class="px-6 py-7 bg-slate-50/80 border-b border-slate-100/80">
              <div class="flex items-center gap-4">
                <div class="relative">
                  <div class="w-14 h-14 rounded-2xl border-2 border-white shadow-md overflow-hidden bg-white">
                    <img
                      v-if="authStore.userInfo?.avatarUrl"
                      :src="authStore.userInfo.avatarUrl"
                      class="w-full h-full object-cover"
                    />
                    <img v-else src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" class="w-full h-full object-cover" />
                  </div>
                  <div class="absolute -bottom-1 -right-1 w-5 h-5 bg-green-500 border-2 border-white rounded-full"></div>
                </div>
                <div class="flex flex-col gap-0.5">
                  <div class="flex items-center gap-1.5">
                    <span class="text-base font-bold text-slate-800 tracking-tight">
                      {{ authStore.userInfo?.nickname || '用户名' }}
                    </span>
                    <span class="px-1.5 py-0.5 bg-primary/10 text-[10px] text-primary font-bold rounded uppercase">PRO</span>
                  </div>
                  <span class="text-xs text-slate-400 font-medium tracking-wide">
                    {{ authStore.userInfo?.phone?.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') }}
                  </span>
                </div>
              </div>
            </div>

            <!-- List Menu: MD3 Interactive Items -->
            <div class="p-2">
              <div class="grid gap-1">
                <div 
                  class="flex items-center gap-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-200 group hover:bg-primary/5 hover:translate-x-1"
                  @click="router.push('/profile')"
                >
                  <div class="w-8 h-8 rounded-lg bg-slate-50 flex items-center justify-center text-slate-400 group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                    <el-icon :size="18"><User /></el-icon>
                  </div>
                  <span class="text-sm font-semibold text-slate-600 group-hover:text-slate-900">个人中心</span>
                </div>

                <div 
                  class="flex items-center gap-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-200 group hover:bg-primary/5 hover:translate-x-1"
                  @click="router.push('/favorites')"
                >
                  <div class="w-8 h-8 rounded-lg bg-slate-50 flex items-center justify-center text-slate-400 group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                    <el-icon :size="18"><Star /></el-icon>
                  </div>
                  <span class="text-sm font-semibold text-slate-600 group-hover:text-slate-900">我的收藏</span>
                </div>

                <div 
                  class="flex items-center gap-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-200 group hover:bg-primary/5 hover:translate-x-1"
                  @click="router.push('/appointments')"
                >
                  <div class="w-8 h-8 rounded-lg bg-slate-50 flex items-center justify-center text-slate-400 group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                    <el-icon :size="18"><Calendar /></el-icon>
                  </div>
                  <span class="text-sm font-semibold text-slate-600 group-hover:text-slate-900">预约记录</span>
                </div>

                <div 
                  class="flex items-center gap-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-200 group hover:bg-primary/5 hover:translate-x-1"
                  @click="router.push('/settings')"
                >
                  <div class="w-8 h-8 rounded-lg bg-slate-50 flex items-center justify-center text-slate-400 group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                    <el-icon :size="18"><Setting /></el-icon>
                  </div>
                  <span class="text-sm font-semibold text-slate-600 group-hover:text-slate-900">账号设置</span>
                </div>
              </div>

              <!-- Logout: Distinct MD3 Action -->
              <div class="mt-2 pt-2 border-t border-slate-50">
                <div 
                  class="flex items-center gap-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-200 group hover:bg-red-50"
                  @click="handleLogout"
                >
                  <div class="w-8 h-8 rounded-lg bg-red-50/50 flex items-center justify-center text-red-400 group-hover:bg-red-100 group-hover:text-red-600 transition-colors">
                    <el-icon :size="18"><SwitchButton /></el-icon>
                  </div>
                  <span class="text-sm font-semibold text-slate-600 group-hover:text-red-700">退出登录</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </el-dropdown>

      <!-- 未登录：显示登录按钮 -->
      <el-button
        v-else
        type="primary"
        round
        class="!px-6 !font-bold"
        @click="router.push('/login')"
      >
        登录
      </el-button>
    </div>
  </nav>
</template>

<style scoped>
nav {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 自定义滚动条样式 */
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #e5e7eb;
  border-radius: 10px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #d1d5db;
}

/* 搜索框样式优化 */
:deep(.location-search-input .el-input__wrapper) {
  background-color: #f9fafb;
  box-shadow: none !important;
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  padding: 0 12px;
  transition: all 0.2s;
}

:deep(.location-search-input .el-input__wrapper.is-focus) {
  background-color: white;
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-8) !important;
}
</style>

<style>
@reference "@/assets/main.css";

/* 全局样式用于处理 Popover */
.location-popover {
  padding: 0 !important;
  border-radius: 12px !important;
  overflow: hidden;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1) !important;
  border: 1px solid #f3f4f6 !important;
  margin-top: 12px !important;
}

.location-popover .el-popper__arrow,
.user-dropdown-popper .el-popper__arrow {
  display: none; /* 隐藏小三角，更现代感 */
}

/* 用户下拉菜单容器样式 - 遵循 04 规范移除固定 px */
.user-dropdown-popper {
  padding: 0 !important;
  @apply !rounded-2xl !border-none;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25) !important;
  margin-top: 0.75rem !important;
}

.user-dropdown-card {
  @apply rounded-2xl;
}
</style>
