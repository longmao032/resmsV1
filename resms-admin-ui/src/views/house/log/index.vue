<template>
  <div class="p-6 min-h-screen bg-gray-50/50">
    <!-- 头部装饰 -->
    <div class="flex items-center justify-between mb-8">
      <div class="flex items-center gap-4">
        <div class="w-14 h-14 bg-indigo-600 rounded-[22px] flex items-center justify-center text-white shadow-xl shadow-indigo-100">
          <el-icon :size="28"><List /></el-icon>
        </div>
        <div>
          <h2 class="text-2xl font-black text-gray-800 tracking-tight">房源变动日志</h2>
          <p class="text-xs text-gray-400 font-bold uppercase tracking-widest mt-0.5">Audit Trail & Status History</p>
        </div>
      </div>
      
      <div class="flex items-center gap-3">
        <el-button v-hasPermi="['house:log:export']" class="!rounded-2xl !h-12 !px-6 !border-none !bg-white !shadow-sm hover:!bg-gray-50 !text-gray-600 font-bold transition-all">
          <el-icon class="mr-2"><Download /></el-icon> 导出审计报告
        </el-button>
        <el-button v-hasPermi="['house:log:query']" type="primary" class="!rounded-2xl !h-12 !px-8 !border-none !bg-indigo-600 hover:!bg-indigo-700 shadow-lg shadow-indigo-200 font-bold transition-all" @click="handleQuery">
          <el-icon class="mr-2"><Refresh /></el-icon> 刷新日志
        </el-button>
      </div>
    </div>

    <!-- 过滤器卡片 -->
    <el-card class="!border-none !rounded-[32px] shadow-sm mb-6 overflow-visible">
      <div class="flex flex-wrap items-center gap-6 p-2">
        <div class="flex flex-col gap-1.5">
          <span class="text-[10px] font-black text-gray-400 uppercase ml-1">房源搜索</span>
          <el-input
            v-model="queryParams.houseNo"
            placeholder="输入房源编号/项目名"
            class="!w-64 google-input-log"
            clearable
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>

        <div class="flex flex-col gap-1.5">
          <span class="text-[10px] font-black text-gray-400 uppercase ml-1">操作人员</span>
          <el-input 
            v-model="queryParams.operatorName" 
            placeholder="操作人姓名" 
            class="!w-44 google-input-log" 
            clearable 
            @keyup.enter="handleQuery" 
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <span class="text-[10px] font-black text-gray-400 uppercase ml-1">变更时间区间</span>
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="!w-80 google-input-log"
          />
        </div>
      </div>
    </el-card>

    <!-- 日志列表主体 -->
    <el-card class="!border-none !rounded-[40px] shadow-sm overflow-hidden">
      <el-table 
        :data="logList" 
        v-loading="loading"
        class="log-table"
        :header-cell-style="{ background: '#fcfcfd', color: '#94a3b8', fontSize: '11px', fontWeight: '900', textTransform: 'uppercase', padding: '24px 0', letterSpacing: '1px' }"
      >
        <el-table-column label="房源信息" min-width="220">
          <template #default="{ row }">
            <div class="flex flex-col gap-1 py-1">
              <span class="text-sm font-black text-gray-800">{{ row.projectName }}</span>
              <span class="text-[10px] font-mono bg-gray-100 text-gray-500 px-2 py-0.5 rounded-md w-fit">{{ row.houseNo }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态轨迹" min-width="260">
          <template #default="{ row }">
            <div class="flex items-center gap-3">
              <div :class="`px-3 py-1 rounded-full text-[11px] font-bold ${getStatusClass(row.fromStatus)}`" v-if="row.fromStatus !== null">
                {{ getStatusLabel(row.fromStatus) }}
              </div>
              <span class="text-gray-300" v-if="row.fromStatus !== null">
                <el-icon><Right /></el-icon>
              </span>
              <div :class="`px-3 py-1 rounded-full text-[11px] font-bold ${getStatusClass(row.toStatus)}`">
                {{ getStatusLabel(row.toStatus) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="变更原因/描述" min-width="300">
          <template #default="{ row }">
            <div class="flex items-start gap-2 group">
              <div class="w-1.5 h-1.5 rounded-full bg-indigo-400 mt-2 flex-shrink-0 group-hover:scale-150 transition-transform"></div>
              <span class="text-sm text-gray-600 font-medium leading-relaxed">{{ row.changeReason || '系统自动处理' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作人" width="160">
          <template #default="{ row }">
            <div class="flex items-center gap-3">
              <el-avatar :size="24" class="!bg-indigo-50 !text-indigo-600 font-bold text-[10px]">
                {{ row.operatorName.substring(0, 1) }}
              </el-avatar>
              <div class="flex flex-col">
                <span class="text-sm font-bold text-gray-700">{{ row.operatorName }}</span>
                <span class="text-[10px] text-gray-400 font-mono">{{ row.ipAddress }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作时间" width="180" prop="createTime">
          <template #default="{ row }">
            <div class="flex items-center gap-2 text-gray-500">
              <el-icon :size="14"><Clock /></el-icon>
              <span class="text-xs font-bold">{{ row.createTime }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="p-8 flex justify-between items-center bg-gray-50/30">
        <div class="flex items-center gap-2">
          <span class="text-xs font-bold text-gray-400">PAGE SIZE:</span>
          <el-select v-model="queryParams.pageSize" size="small" class="!w-20" @change="handleQuery">
            <el-option :label="20" :value="20" />
            <el-option :label="50" :value="50" />
          </el-select>
        </div>
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          layout="prev, pager, next"
          :total="total"
          class="log-pagination"
          @current-change="handleQuery"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { 
  List, Search, Download, Refresh, Right, 
  Clock
} from '@element-plus/icons-vue'
import { listStatusLog, type HouseStatusLog } from '@/api/house/log'

const loading = ref(false)
const total = ref(0)
const logList = ref<HouseStatusLog[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  houseNo: '',
  operatorName: '',
  dateRange: []
})

/** 查询日志列表 */
const handleQuery = async () => {
  loading.value = true
  try {
    const res = await listStatusLog(queryParams)
    logList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

/** 重置查询 */
const resetQuery = () => {
  queryParams.houseNo = ''
  queryParams.operatorName = ''
  queryParams.dateRange = []
  handleQuery()
}

onMounted(() => {
  handleQuery()
})

const getStatusLabel = (status: number | null) => {
  if (status === null) return '初始录入'
  const map: any = { 0: '待审核', 1: '在售', 2: '已预订', 3: '已成交', 4: '下架' }
  return map[status] || '未知'
}

const getStatusClass = (status: number | null) => {
  if (status === null) return 'bg-gray-50 text-gray-400'
  const map: any = {
    0: 'bg-amber-50 text-amber-600',
    1: 'bg-emerald-50 text-emerald-600',
    2: 'bg-blue-50 text-blue-600',
    3: 'bg-indigo-50 text-indigo-600',
    4: 'bg-rose-50 text-rose-600'
  }
  return map[status] || 'bg-gray-50 text-gray-400'
}
</script>

<style scoped>
.google-input-log :deep(.el-input__wrapper),
.google-input-log :deep(.el-select__wrapper),
.google-input-log.el-date-editor {
  border-radius: 16px;
  background-color: #f8fafc;
  box-shadow: none !important;
  border: 1px solid #f1f5f9;
  height: 48px;
  padding: 0 16px;
  transition: all 0.3s ease;
}

.google-input-log :deep(.el-input__wrapper.is-focus),
.google-input-log :deep(.el-select__wrapper.is-focus) {
  background-color: #fff;
  border-color: #4f46e5;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.05) !important;
}

.log-table :deep(.el-table__row) {
  transition: all 0.3s;
}

.log-table :deep(.el-table__row:hover) {
  background-color: #f8fafc !important;
}

.log-table :deep(.el-table__cell) {
  padding: 20px 0;
  border-bottom: 1px solid #f1f5f9;
}

.log-pagination :deep(.el-pager li) {
  border-radius: 12px;
  margin: 0 4px;
  font-weight: 800;
  color: #94a3b8;
  transition: all 0.3s;
}

.log-pagination :deep(.el-pager li.is-active) {
  background-color: #4f46e5;
  color: #fff;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}
</style>
