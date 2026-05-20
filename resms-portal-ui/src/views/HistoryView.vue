<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Clock, Delete, House, MapLocation, View, Phone, ChatDotRound, Pointer } from '@element-plus/icons-vue'
import { getBrowseHistoryApi, removeBrowseHistoryApi, clearBrowseHistoryApi, type BrowseHistoryItem } from '@/api/user'

const router = useRouter()
const historyList = ref<BrowseHistoryItem[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const activeTab = ref(-1)

const resourceTabs = [
  { label: '全部', value: -1 },
  { label: '房源', value: 1 },
  { label: '项目', value: 2 },
]

const actionLabels: Record<string, string> = {
  view: '浏览',
  call: '电话咨询',
  visit: '实地带看',
  chat: '在线咨询',
}

const actionIcons: Record<string, any> = {
  view: View,
  call: Phone,
  visit: Pointer,
  chat: ChatDotRound,
}

async function fetchHistory() {
  loading.value = true
  try {
    const params: Record<string, number> = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (activeTab.value !== -1) params.resourceType = activeTab.value
    const res = await getBrowseHistoryApi(params)
    historyList.value = res.records
    total.value = res.total
  } catch (e) {
    console.error('获取浏览记录失败', e)
  } finally {
    loading.value = false
  }
}

async function handleRemove(item: BrowseHistoryItem) {
  try {
    await ElMessageBox.confirm('确定删除该浏览记录吗？', '提示', { type: 'warning' })
    await removeBrowseHistoryApi(item.id)
    ElMessage.success('已删除')
    fetchHistory()
  } catch { /* 取消 */ }
}

async function handleClear() {
  if (historyList.value.length === 0) return
  try {
    await ElMessageBox.confirm('确定清空所有浏览记录吗？', '提示', { type: 'warning' })
    await clearBrowseHistoryApi()
    ElMessage.success('已清空')
    fetchHistory()
  } catch { /* 取消 */ }
}

function handleTabChange(tab: number) {
  activeTab.value = tab
  pageNum.value = 1
  fetchHistory()
}

function handlePageChange(page: number) {
  pageNum.value = page
  fetchHistory()
}

function goDetail(item: BrowseHistoryItem) {
  const prefix = item.resourceType === 1 ? 'house' : 'project'
  router.push(`/${prefix}/${item.resourceId}`)
}

function formatDuration(seconds: number) {
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`
  return `${Math.floor(seconds / 3600)}小时`
}

function formatTime(time: string) {
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 172800000) return '昨天'
  return time.split(' ')[0]
}

onMounted(fetchHistory)
</script>

<template>
  <div class="min-h-screen bg-slate-50/50 pb-20">
    <!-- 顶部导航 -->
    <div class="bg-white pt-24 pb-8 px-6 border-b border-slate-100">
      <div class="max-w-5xl mx-auto">
        <el-breadcrumb :separator-icon="ArrowRight" class="mb-4">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/profile' }">个人中心</el-breadcrumb-item>
          <el-breadcrumb-item>浏览记录</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-slate-800">浏览记录</h1>
            <p class="text-sm text-slate-400 mt-1">最近浏览的房源与项目</p>
          </div>
          <el-button
            v-if="total > 0"
            text
            type="danger"
            :icon="Delete"
            @click="handleClear"
          >
            清空记录
          </el-button>
        </div>
      </div>
    </div>

    <!-- Tab 筛选 -->
    <div class="max-w-5xl mx-auto px-6 mt-6">
      <div class="bg-white rounded-2xl p-2 flex gap-1 shadow-sm">
        <div
          v-for="tab in resourceTabs"
          :key="tab.value"
          class="flex-1 text-center py-2.5 rounded-xl text-sm font-semibold cursor-pointer transition-all"
          :class="activeTab === tab.value
            ? 'bg-primary text-white shadow-sm'
            : 'text-slate-500 hover:bg-slate-50 hover:text-slate-700'"
          @click="handleTabChange(tab.value)"
        >
          {{ tab.label }}
        </div>
      </div>
    </div>

    <!-- 列表 -->
    <div class="max-w-5xl mx-auto px-6 mt-6">
      <div v-loading="loading" class="flex flex-col gap-3">
        <!-- 空状态 -->
        <div v-if="historyList.length === 0 && !loading" class="text-center py-20">
          <el-icon :size="48" class="text-slate-200 mb-4"><Clock /></el-icon>
          <p class="text-slate-400">暂无浏览记录</p>
          <el-button type="primary" class="mt-4" @click="router.push('/new-house')">去看看新房</el-button>
        </div>

        <!-- 记录卡片 -->
        <div
          v-for="item in historyList"
          :key="item.id"
          class="bg-white rounded-2xl shadow-sm hover:shadow-md transition-shadow p-4 flex gap-4 cursor-pointer group"
          @click="goDetail(item)"
        >
          <!-- 封面 -->
          <div class="w-24 h-20 rounded-xl overflow-hidden bg-slate-100 flex-shrink-0">
            <img v-if="item.resourceCover" :src="item.resourceCover" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-slate-300">
              <el-icon :size="28"><House /></el-icon>
            </div>
          </div>

          <!-- 信息 -->
          <div class="flex-1 min-w-0 flex flex-col justify-between">
            <div>
              <h3 class="text-sm font-bold text-slate-800 truncate group-hover:text-primary transition-colors">
                {{ item.resourceTitle }}
              </h3>
              <div class="flex items-center gap-3 text-xs text-slate-400 mt-1">
                <span class="flex items-center gap-1">
                  <el-icon :size="12"><MapLocation /></el-icon>
                  {{ item.city }} {{ item.district }}
                </span>
                <span v-if="item.layout">{{ item.layout }}</span>
                <span v-if="item.area">{{ item.area }}m²</span>
              </div>
            </div>
            <div class="flex items-center justify-between mt-1">
              <div class="flex items-center gap-2 text-xs">
                <span
                  class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md"
                  :class="{
                    'bg-blue-50 text-blue-500': item.actionType === 'view',
                    'bg-green-50 text-green-500': item.actionType === 'call',
                    'bg-orange-50 text-orange-500': item.actionType === 'visit',
                    'bg-purple-50 text-purple-500': item.actionType === 'chat',
                  }"
                >
                  <el-icon :size="12"><component :is="actionIcons[item.actionType] || View" /></el-icon>
                  {{ actionLabels[item.actionType] || '浏览' }}
                </span>
                <span class="text-slate-300">{{ formatDuration(item.duration) }}</span>
              </div>
              <div class="flex items-center gap-2">
                <span class="text-xs text-slate-300">{{ formatTime(item.viewTime) }}</span>
                <el-button
                  text
                  size="small"
                  type="danger"
                  :icon="Delete"
                  @click.stop="handleRemove(item)"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="mt-8 flex justify-center">
        <el-pagination
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>
