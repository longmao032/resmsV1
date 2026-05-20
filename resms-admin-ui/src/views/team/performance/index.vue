<template>
  <div class="team-performance-container p-6 bg-gray-50 min-h-full">
    <!-- 顶部数据看板 -->
    <div class="grid grid-cols-4 gap-6 mb-8">
      <div v-for="stat in summaryStats" :key="stat.label" class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col justify-between h-40">
        <div class="flex items-center justify-between">
          <div class="p-2 rounded-xl" :style="{ backgroundColor: stat.color + '15' }">
            <el-icon :size="24" :style="{ color: stat.color }"><component :is="stat.icon" /></el-icon>
          </div>
          <span class="text-[10px] font-bold" :class="stat.trend > 0 ? 'text-green-500' : 'text-red-500'">
            {{ stat.trend > 0 ? '+' : '' }}{{ stat.trend }}%
          </span>
        </div>
        <div>
          <div class="text-3xl font-black text-[#202124] mt-2">{{ stat.value }}<span class="text-sm ml-1 font-normal text-gray-400">{{ stat.unit }}</span></div>
          <div class="text-xs text-gray-500 font-medium uppercase tracking-wider mt-1">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <el-row :gutter="24">
      <!-- 左侧：趋势图与分布 -->
      <el-col :span="16">
        <div class="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 mb-6">
          <div class="flex items-center justify-between mb-8">
            <div>
              <h3 class="text-xl font-bold text-[#1f1f1f]">部门业绩增长趋势</h3>
              <p class="text-xs text-gray-400 mt-1">对比过去 6 个月的成交总额变化</p>
            </div>
            <el-select v-model="timeRange" size="small" class="google-input-flat !w-32">
              <el-option label="上半年" value="1" />
              <el-option label="下半年" value="2" />
            </el-select>
          </div>
          
          <!-- 模拟柱状图 -->
          <div class="h-64 flex items-end justify-between px-4 gap-4">
            <div v-for="item in trendData" :key="item.month" class="flex-1 flex flex-col items-center group">
              <div class="w-full bg-[#f1f3f4] rounded-t-lg relative overflow-hidden" :style="{ height: Math.max(item.value, 5) + '%' }">
                <div class="absolute inset-0 bg-gradient-to-t from-[#1a73e8] to-[#4285f4] opacity-80 group-hover:opacity-100 transition-opacity"></div>
              </div>
              <span class="text-[10px] text-gray-400 mt-3 font-medium">{{ item.month }}</span>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-6">
          <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
            <h4 class="text-sm font-bold text-gray-800 mb-4">房源类型分布</h4>
            <div class="space-y-4">
              <div v-for="type in houseTypeDist" :key="type.name">
                <div class="flex justify-between text-xs mb-1">
                  <span class="text-gray-600">{{ type.name }}</span>
                  <span class="font-bold">{{ type.value }}%</span>
                </div>
                <el-progress :percentage="type.value" :show-text="false" :stroke-width="12" :color="type.color" class="google-progress" />
              </div>
            </div>
          </div>
          <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
            <h4 class="text-sm font-bold text-gray-800 mb-4">客源转化漏斗</h4>
            <div class="flex flex-col gap-2">
              <div v-for="(step, index) in funnelData" :key="step.name" 
                   class="h-10 flex items-center justify-center text-white text-xs font-bold rounded-lg transition-transform hover:scale-[1.02]"
                   :style="{ width: (100 - index * 15) + '%', backgroundColor: '#1a73e8', opacity: 1 - index * 0.15 }">
                {{ step.name }}: {{ step.count }}
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧：销售排行 -->
      <el-col :span="8">
        <div class="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 h-full">
          <h3 class="text-xl font-bold text-[#1f1f1f] mb-6">销售英雄榜</h3>
          <div class="space-y-6">
            <div v-for="(sales, index) in rankingData" :key="sales.name" class="flex items-center justify-between p-3 rounded-2xl hover:bg-gray-50 transition-colors">
              <div class="flex items-center gap-4">
                <div class="w-8 h-8 rounded-full flex items-center justify-center font-bold" 
                     :class="index < 3 ? 'bg-orange-100 text-orange-600' : 'bg-gray-100 text-gray-400'">
                  {{ index + 1 }}
                </div>
                <div>
                  <div class="text-sm font-bold text-gray-800">{{ sales.name }}</div>
                  <div class="text-[10px] text-gray-400">成交 {{ sales.count }} 套</div>
                </div>
              </div>
              <div class="text-right">
                <div class="text-sm font-mono font-bold text-[#1a73e8]">¥{{ sales.amount }}万</div>
                <div class="text-[10px] text-green-500">↑ {{ sales.growth }}%</div>
              </div>
            </div>
          </div>
          <el-button class="w-full mt-8 !rounded-2xl !h-12 border-dashed" plain>查看完整排行</el-button>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, markRaw } from 'vue'
import { Money, TrendCharts, User, Tickets } from '@element-plus/icons-vue'
import { getTeamPerformance } from '@/api/finance/report'
import type { TeamPerformanceVO } from '@/api/finance/report'

const timeRange = ref('1')

const loading = ref(false)

const summaryStats = reactive([
  { label: '部门总成交', value: '0', unit: '万', trend: 0, icon: markRaw(Money), color: '#1a73e8' },
  { label: '新增意向客户', value: '0', unit: '人', trend: 0, icon: markRaw(User), color: '#34a853' },
  { label: '带看总量', value: '0', unit: '次', trend: 0, icon: markRaw(Tickets), color: '#fbbc04' },
  { label: '目标达成率', value: '0', unit: '%', trend: 0, icon: markRaw(TrendCharts), color: '#ea4335' }
])

const trendData = ref<{ month: string; value: number }[]>([])
const rankingData = ref<{ name: string; amount: number; count: number; growth: number }[]>([])
const houseTypeDist = ref<{ name: string; value: number; color: string }[]>([])
const funnelData = ref<{ name: string; count: number }[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getTeamPerformance()
    const data: TeamPerformanceVO = res.data

    // 顶部概览
    if (data.summary) {
      summaryStats[0].value = (data.summary.totalDealAmount / 10000).toFixed(2)
      summaryStats[1].value = data.summary.newCustomerCount.toLocaleString()
      summaryStats[2].value = data.summary.totalViewings.toLocaleString()
      summaryStats[3].value = data.summary.targetRate.toFixed(1)
    }

    // 月度趋势
    if (data.monthlyTrend && data.monthlyTrend.length > 0) {
      const maxVal = Math.max(...data.monthlyTrend.map(d => d.value))
      trendData.value = data.monthlyTrend.map(d => ({
        month: (d.month.split('-')[1]?.replace(/^0/, '') || '') + '月',
        value: maxVal > 0 ? Math.round((d.value / maxVal) * 100) : 0
      }))
    }

    // 销售排行
    if (data.salesRanking && data.salesRanking.length > 0) {
      rankingData.value = data.salesRanking.map((s, i) => ({
        name: s.realName,
        amount: (s.totalDealAmount / 10000),
        count: s.totalOrders,
        growth: 15 - i * 3
      }))
    }

    // 房源类型分布
    if (data.houseTypeDistribution && data.houseTypeDistribution.length > 0) {
      houseTypeDist.value = data.houseTypeDistribution
    }

    // 漏斗
    if (data.funnel) {
      funnelData.value = [
        { name: '录入', count: data.funnel.customerCount },
        { name: '带看', count: data.funnel.viewingCount },
        { name: '成交', count: data.funnel.dealCount }
      ]
    }
  } catch {
    // 加载失败不影响页面，保留初始空数据
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.team-performance-container {
  height: calc(100vh - 110px);
  overflow-y: auto;
}

/* 进度条圆角 */
:deep(.google-progress .el-progress-bar__inner) {
  border-radius: 6px;
}
:deep(.google-progress .el-progress-bar__outer) {
  background-color: #f1f3f4;
}

/* Select 扁平化 */
:deep(.google-input-flat .el-input__wrapper) {
  border-radius: 12px;
  background-color: #f1f3f4;
  box-shadow: none !important;
}
</style>
