<template>
  <div class="report-container p-6 bg-gray-50 min-h-full">
    <div class="flex flex-col h-full bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-32">
        <el-icon class="is-loading" :size="32" color="#1a73e8"><Loading /></el-icon>
        <span class="ml-3 text-gray-400 text-sm">加载中...</span>
      </div>

      <!-- Error -->
      <div v-else-if="loadError" class="flex flex-col items-center py-32">
        <el-icon :size="48" color="#f56c6c"><WarningFilled /></el-icon>
        <p class="text-gray-500 mt-4 mb-2">数据加载失败</p>
        <el-button type="primary" @click="fetchData">重新加载</el-button>
      </div>

      <!-- Empty -->
      <div v-else-if="noData" class="flex flex-col items-center py-32">
        <el-empty description="暂无经营数据" />
      </div>

      <!-- Content -->
      <template v-else>
        <!-- KPI Cards -->
        <div class="p-8 border-b border-gray-50 bg-white grid grid-cols-4 gap-8">
          <div v-for="kpi in kpis" :key="kpi.title" class="flex flex-col">
            <span class="text-xs text-gray-400 font-bold uppercase tracking-wider mb-2">{{ kpi.title }}</span>
            <div class="flex items-baseline gap-2">
              <span class="text-3xl font-bold" :class="kpi.color">{{ kpi.value }}</span>
              <span class="text-xs text-gray-400 font-mono">{{ kpi.sub }}</span>
            </div>
          </div>
        </div>

        <!-- Charts & Tables -->
        <el-scrollbar class="flex-1">
          <div class="p-8">
            <!-- Trend Chart + Project Table -->
            <div class="grid grid-cols-2 gap-8 mb-8">
              <el-card shadow="never" class="!border border-gray-100 !rounded-2xl" v-hasPermi="['finance:report:trend']">
                <template #header>
                  <div class="flex items-center justify-between">
                    <span class="font-bold text-gray-700">资金收支趋势（近{{ trendDays }}天）</span>
                    <div class="flex items-center gap-2">
                      <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
                        <el-radio-button :value="7">7天</el-radio-button>
                        <el-radio-button :value="30">30天</el-radio-button>
                        <el-radio-button :value="90">90天</el-radio-button>
                      </el-radio-group>
                    </div>
                  </div>
                </template>
                <div ref="trendChartRef" style="height:300px;width:100%" />
              </el-card>

              <el-card shadow="never" class="!border border-gray-100 !rounded-2xl" v-hasPermi="['finance:report:project']">
                <template #header>
                  <div class="flex items-center justify-between">
                    <span class="font-bold text-gray-700">楼盘经营概况</span>
                  </div>
                </template>
                <el-table :data="projectList" max-height="300" class="google-table-flat" stripe>
                  <el-table-column label="楼盘名称" prop="projectName" min-width="120" show-overflow-tooltip />
                  <el-table-column label="成交套数" prop="dealCount" width="80" align="center" />
                  <el-table-column label="收款金额" width="140" align="right">
                    <template #default="{ row }">
                      <span class="font-mono">¥{{ (row.totalIncome || 0).toLocaleString() }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="佣金支出" width="140" align="right">
                    <template #default="{ row }">
                      <span class="font-mono text-red-500">¥{{ (row.totalCommission || 0).toLocaleString() }}</span>
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>
            </div>

            <!-- Sales Ranking -->
            <el-card shadow="never" class="!border border-gray-100 !rounded-2xl mb-8" v-hasPermi="['finance:report:sales']">
              <template #header>
                <div class="flex items-center justify-between">
                  <span class="font-bold text-gray-700">销售业绩排行</span>
                  <span class="text-xs text-gray-400">按成交金额降序</span>
                </div>
              </template>
              <el-table :data="salesList" class="google-table-flat" stripe>
                <el-table-column type="index" label="排名" width="70" align="center">
                  <template #default="{ $index }">
                    <div v-if="$index < 3" class="w-6 h-6 rounded-full flex items-center justify-center font-bold text-xs mx-auto"
                      :class="[$index === 0 ? 'bg-yellow-100 text-yellow-600' : $index === 1 ? 'bg-gray-100 text-gray-500' : 'bg-orange-100 text-orange-600']">
                      {{ $index + 1 }}
                    </div>
                    <span v-else class="text-gray-400 font-mono">{{ $index + 1 }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="姓名" min-width="120">
                  <template #default="{ row }">
                    <div class="flex items-center gap-3">
                      <el-avatar :size="28" class="!bg-blue-100 !text-blue-600 font-bold text-xs">{{ (row.realName || '?').charAt(0) }}</el-avatar>
                      <span class="font-medium text-gray-700">{{ row.realName }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="成交单数" prop="totalOrders" width="100" align="center" />
                <el-table-column label="成交金额" width="170" align="right">
                  <template #default="{ row }">
                    <span class="font-bold text-gray-800 font-mono">¥{{ (row.totalDealAmount || 0).toLocaleString() }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="实际回款" width="170" align="right">
                  <template #default="{ row }">
                    <span class="font-mono text-green-600">¥{{ (row.totalActualPaid || 0).toLocaleString() }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="应发佣金" width="170" align="right">
                  <template #default="{ row }">
                    <span class="font-mono text-orange-500">¥{{ (row.totalCommission || 0).toLocaleString() }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="回款率" width="100" align="center">
                  <template #default="{ row }">
                    <span v-if="row.totalDealAmount">
                      {{ (row.totalActualPaid / row.totalDealAmount * 100).toFixed(1) }}%
                    </span>
                    <span v-else class="text-gray-300">-</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </el-scrollbar>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { Loading, WarningFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getProjectReport, getSalesReport, getTrendReport, type ProjectReportVO, type SalesReportVO, type TrendReportVO } from '@/api/finance/report'

const loading = ref(true)
const loadError = ref(false)

const projectList = ref<ProjectReportVO[]>([])
const salesList = ref<SalesReportVO[]>([])
const trendList = ref<TrendReportVO[]>([])
const trendDays = ref(30)

const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

const noData = computed(() =>
  !loading.value && !loadError.value &&
  projectList.value.length === 0 && salesList.value.length === 0 && trendList.value.length === 0
)

const kpis = computed(() => {
  const totalIncome = projectList.value.reduce((s, p) => s + (p.totalIncome || 0), 0)
  const totalComm = projectList.value.reduce((s, p) => s + (p.totalCommission || 0), 0)
  const totalDeals = projectList.value.reduce((s, p) => s + (p.dealCount || 0), 0)
  const salesCount = salesList.value.length

  return [
    {
      title: '总成交金额',
      value: '¥' + totalIncome.toLocaleString(),
      sub: '含所有项目',
      color: 'text-[#1a73e8]'
    },
    {
      title: '佣金支出总额',
      value: '¥' + totalComm.toLocaleString(),
      sub: '已发放佣金',
      color: 'text-red-500'
    },
    {
      title: '总成交套数',
      value: totalDeals.toLocaleString(),
      sub: '套',
      color: 'text-green-600'
    },
    {
      title: '销售人员',
      value: salesCount.toLocaleString(),
      sub: '人',
      color: 'text-orange-500'
    }
  ]
})

async function loadTrend() {
  try {
    const res = await getTrendReport(trendDays.value)
    trendList.value = res.data || []
  } catch {
    trendList.value = []
  }
  renderTrendChart()
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const dates = trendList.value.map(t => t.dateStr)
  const income = trendList.value.map(t => t.income || 0)
  const expense = trendList.value.map(t => t.expense || 0)

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v: number) => '¥' + v.toLocaleString()
    },
    legend: { data: ['资金流入', '资金流出'], bottom: 0 },
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { fontSize: 11, color: '#9aa0a6' }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        fontSize: 11,
        color: '#9aa0a6',
        formatter: (v: number) => v >= 10000 ? (v / 10000).toFixed(0) + 'w' : v
      }
    },
    series: [
      {
        name: '资金流入',
        type: 'bar',
        data: income,
        itemStyle: { color: '#1a73e8', borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 32
      },
      {
        name: '资金流出',
        type: 'bar',
        data: expense,
        itemStyle: { color: '#ea4335', borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 32
      }
    ]
  }, true)
}

const fetchData = async () => {
  loading.value = true
  loadError.value = false
  try {
    const [projectRes, salesRes] = await Promise.all([
      getProjectReport().catch(() => ({ data: [] as ProjectReportVO[] })),
      getSalesReport().catch(() => ({ data: [] as SalesReportVO[] }))
    ])
    projectList.value = projectRes.data || []
    salesList.value = salesRes.data || []
    await loadTrend()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchData()
  await nextTick()
  renderTrendChart()
})

// Resize chart on window resize
window.addEventListener('resize', () => {
  trendChart?.resize()
})
</script>

<style scoped>
.report-container {
  height: calc(100vh - 110px);
}

.google-table-flat {
  --el-table-header-bg-color: #f8f9fa;
  --el-table-row-hover-bg-color: #f8f9fa;
  --el-table-border: none;
}

:deep(.el-table__header th) {
  color: #5f6368;
  font-weight: 600;
  border-bottom: 1px solid #f1f3f4 !important;
}

:deep(.el-table__row) {
  height: 56px;
}

:deep(.el-table__row td) {
  border-bottom: 1px solid #f1f3f4 !important;
}
</style>
