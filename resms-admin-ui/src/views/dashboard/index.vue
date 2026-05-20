<template>
  <div class="dashboard-container p-6 bg-[#f8f9fa] min-h-full">
    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-32">
      <el-icon class="is-loading" :size="32" color="#1a73e8">
        <Loading />
      </el-icon>
      <span class="ml-3 text-gray-400 text-sm">加载中...</span>
    </div>

    <!-- Error -->
    <div v-else-if="loadError" class="flex flex-col items-center py-32">
      <el-icon :size="48" color="#f56c6c">
        <WarningFilled />
      </el-icon>
      <p class="text-gray-500 mt-4 mb-2">数据加载失败</p>
      <el-button type="primary" @click="fetchData">重新加载</el-button>
    </div>

    <!-- Content -->
    <template v-else>
      <!-- 欢迎卡片 -->
      <el-card shadow="never"
        class="!border-none rounded-[32px] mb-6 bg-white shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-white/60">
        <div class="flex items-center justify-between p-4">
          <div class="flex items-center gap-6">
            <el-avatar :size="64" :src="userAvatar" class="bg-[#1a73e8] shadow-md !text-2xl font-bold shrink-0">
              <template v-if="!userAvatar">{{ displayName.charAt(0).toUpperCase() }}</template>
            </el-avatar>
            <div>
              <h1 class="text-2xl font-bold text-[#1f1f1f] mb-1">{{ greeting }}，{{ displayName }}</h1>
              <p class="text-gray-400 text-sm font-medium">{{ welcomeMessage }}</p>
            </div>
          </div>
          <div class="hidden md:block">
            <el-button 
              plain 
              class="!rounded-full !border-gray-100 !bg-gray-50/50 !text-gray-500 hover:!bg-white hover:!text-[#1a73e8] hover:!border-blue-100 transition-all px-6" 
              @click="fetchData"
            >
              刷新数据
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 核心统计指标 -->
      <el-row :gutter="20" class="mb-6">
        <el-col :span="6" v-for="item in stats" :key="item.title">
          <el-card shadow="never" class="!border-none rounded-[24px] hover:shadow-md transition-all">
            <div class="flex items-center gap-4">
              <div :class="['p-3 rounded-2xl', item.bg]">
                <el-icon :size="24" :class="item.color">
                  <component :is="item.icon" />
                </el-icon>
              </div>
              <div>
                <p class="text-sm text-gray-500 mb-1">{{ item.title }}</p>
                <h2 class="text-2xl font-bold text-gray-800">{{ item.value }}</h2>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <!-- 经营走势图 -->
        <el-col :span="16">
          <el-card shadow="never" class="!border-none rounded-[24px] h-[400px]">
            <template #header>
              <div class="flex items-center justify-between">
                <span class="font-bold text-gray-700">月度经营走势</span>
                <el-radio-group v-model="chartRange" size="small" @change="loadTrend">
                  <el-radio-button value="7d">近7天</el-radio-button>
                  <el-radio-button value="30d">近30天</el-radio-button>
                </el-radio-group>
              </div>
            </template>

            <!-- Chart area with states -->
            <div v-loading="trendLoading" class="h-[300px] w-full">
              <div v-if="trendEmpty && !trendLoading" class="flex items-center justify-center h-full">
                <el-empty description="暂无走势数据" />
              </div>
              <div v-else ref="trendChartRef" class="h-full w-full" />
            </div>
          </el-card>
        </el-col>

        <!-- 最近活动 -->
        <el-col :span="8">
          <el-card shadow="never" class="!border-none rounded-[24px] h-[400px]">
            <template #header>
              <span class="font-bold text-gray-700">最新动态</span>
            </template>
            <div v-if="activities.length === 0" class="flex items-center justify-center h-[300px]">
              <el-empty description="暂无动态" />
            </div>
            <el-timeline v-else>
              <el-timeline-item v-for="(activity, index) in activities" :key="index" :type="activity.type"
                :timestamp="activity.timestamp" size="large">
                <p class="text-sm font-medium text-gray-700">{{ activity.content }}</p>
                <p class="text-xs text-gray-400 mt-1">{{ activity.user }}</p>
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { Loading, WarningFilled, House, ShoppingBag, Money, User } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { useUserStore } from '@/store/user'
import { getDashboardStats, getDashboardTrend, getDashboardActivities, type DashboardStatsVO, type DashboardTrendVO, type DashboardActivityVO } from '@/api/dashboard'

const userStore = useUserStore()

const loading = ref(true)
const loadError = ref(false)

const statsData = ref<DashboardStatsVO | null>(null)
const trendList = ref<DashboardTrendVO[]>([])
const activities = ref<DashboardActivityVO[]>([])

const chartRange = ref('7d')
const trendLoading = ref(false)

const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

const trendEmpty = computed(() => trendList.value.length === 0)

/** 根据时段生成问候语 */
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早安'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const displayName = computed(() => {
  const info = userStore.userInfo
  return info?.realName || info?.username || '管理员'
})

/** 用户头像 */
const userAvatar = computed(() => {
  return userStore.userInfo?.avatar || ''
})

/** 欢迎语 */
const welcomeMessage = computed(() => {
  const now = new Date()
  const dateStr = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
  const activityCount = activities.value.length
  return `今天是 ${dateStr}。${activityCount > 0 ? `您有 ${activityCount} 条新动态。` : ''}`
})

const stats = computed(() => [
  {
    title: '新增房源',
    value: statsData.value?.newHouses?.toLocaleString() ?? '-',
    icon: House,
    color: 'text-blue-600',
    bg: 'bg-blue-50'
  },
  {
    title: '本月订单',
    value: statsData.value?.monthlyOrders?.toLocaleString() ?? '-',
    icon: ShoppingBag,
    color: 'text-orange-600',
    bg: 'bg-orange-50'
  },
  {
    title: '佣金总额',
    value: statsData.value?.totalCommission != null ? `¥${(statsData.value.totalCommission / 1000).toFixed(0)}k` : '-',
    icon: Money,
    color: 'text-green-600',
    bg: 'bg-green-50'
  },
  {
    title: '活跃客户',
    value: statsData.value?.activeClients?.toLocaleString() ?? '-',
    icon: User,
    color: 'text-purple-600',
    bg: 'bg-purple-50'
  }
])

/** 格式化日期标签 */
function formatTrendLabels(list: DashboardTrendVO[]): DashboardTrendVO[] {
  if (chartRange.value === '7d') return list
  return list.map(item => {
    // 对于30天数据，只显示 MM-DD 格式
    const short = item.dateStr.length > 5 ? item.dateStr.slice(5) : item.dateStr
    return { ...item, dateStr: short }
  })
}

async function loadTrend() {
  trendLoading.value = true
  try {
    const days = chartRange.value === '7d' ? 7 : 30
    const res = await getDashboardTrend(days)
    trendList.value = res.data || []
  } catch {
    trendList.value = []
  } finally {
    trendLoading.value = false
  }
  // 确保 DOM 已就绪
  await nextTick()
  renderTrendChart()
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  
  // 检查是否已有实例，避免重复初始化
  let chartInstance = echarts.getInstanceByDom(trendChartRef.value)
  if (!chartInstance) {
    chartInstance = echarts.init(trendChartRef.value)
    trendChart = chartInstance
  }

  const labels = formatTrendLabels(trendList.value).map(t => t.dateStr)
  const values = trendList.value.map(t => t.value || 0)

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v: number) => {
        if (v >= 10000) return `¥${(v / 10000).toFixed(2)}w`
        return `¥${v.toLocaleString()}`
      }
    },
    grid: { left: 60, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: labels,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { fontSize: 11, color: '#9aa0a6' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
      axisLabel: {
        fontSize: 11,
        color: '#9aa0a6',
        formatter: (v: number) => v >= 10000 ? `${(v / 10000).toFixed(0)}w` : v
      }
    },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      showSymbol: false,
      lineStyle: { color: '#1a73e8', width: 2.5 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(26,115,232,0.25)' },
          { offset: 1, color: 'rgba(26,115,232,0.02)' }
        ])
      },
      itemStyle: { color: '#1a73e8' }
    }]
  }, true)
  
  // 强制调整尺寸，防止容器大小变化导致的渲染问题
  chartInstance.resize()
}

async function fetchData() {
  loading.value = true
  loadError.value = false
  try {
    const [statsRes, activitiesRes] = await Promise.all([
      getDashboardStats().catch(() => ({ data: null as DashboardStatsVO | null })),
      getDashboardActivities().catch(() => ({ data: [] as DashboardActivityVO[] }))
    ])
    statsData.value = statsRes.data
    activities.value = activitiesRes.data || []
    await loadTrend()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
  // 等待 DOM 更新（loading=false 后内容区恢复，trendChartRef 就绪）
  await nextTick()
  renderTrendChart()
}

onMounted(async () => {
  await fetchData()
})

// Resize chart on window resize
window.addEventListener('resize', () => {
  trendChart?.resize()
})
</script>

<style scoped>
:deep(.el-timeline-item__node--large) {
  width: 14px;
  height: 14px;
}
</style>
