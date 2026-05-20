<template>
  <div class="commission-container p-6 bg-gray-50 min-h-full">
    <div class="flex flex-col h-full bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <!-- 搜索区域 -->
      <div class="p-6 border-b border-gray-50">
        <el-form :inline="true" :model="queryParams" class="flex flex-wrap gap-x-8 gap-y-4">
          <el-form-item label="发放状态" class="!mb-0">
            <el-select v-model="queryParams.status" placeholder="全部" clearable class="google-input-flat !w-32">
              <el-option label="待核算" :value="0" />
              <el-option label="已核算" :value="1" />
              <el-option label="已发放" :value="2" />
            </el-select>
          </el-form-item>
          <div class="flex gap-2 ml-auto">
            <el-button type="primary" icon="Search" class="!rounded-lg !bg-[#1a73e8] border-none px-6"
              @click="handleQuery">查询</el-button>
            <el-button icon="Refresh" class="!rounded-lg px-6" @click="resetQuery">重置</el-button>
          </div>
        </el-form>
      </div>

      <!-- 统计栏 -->
      <div class="px-6 py-3 flex items-center justify-between bg-gray-50/50 border-b border-gray-50">
        <div class="flex gap-6 text-sm">
          <span class="text-gray-500">待核算：<span class="font-bold text-orange-500">{{ stats.pending }}</span></span>
          <span class="text-gray-500">已核算待发放：<span class="font-bold text-blue-500">{{ stats.calculated }}</span></span>
          <span class="text-gray-500">已发放：<span class="font-bold text-green-500">{{ stats.issued }}</span></span>
        </div>
        <span class="text-xs text-gray-400">合计佣金总额：<span class="font-bold text-gray-700">¥{{ stats.totalAmount.toLocaleString() }}</span></span>
      </div>

      <!-- 表格区域 -->
      <div class="flex-1 px-6 pb-6 overflow-hidden">
        <el-table v-loading="loading" :data="commissionList" row-key="id" height="100%" class="google-table-flat"
          @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" />
          <el-table-column label="交易编号" prop="transactionNo" width="170">
            <template #default="{ row }">
              <span class="font-mono text-[#1a73e8] font-medium text-xs cursor-pointer hover:underline"
                @click="handlePreview(row)">
                {{ row.transactionNo || '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="销售人员" min-width="120">
            <template #default="{ row }">
              <div class="flex items-center gap-2.5">
                <el-avatar :size="26" class="!bg-blue-100 !text-blue-600 font-bold text-xs">
                  {{ (row.salesName || '?').charAt(0) }}
                </el-avatar>
                <span class="font-medium text-gray-800">{{ row.salesName || '未知' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="提成比例" width="100" align="center">
            <template #default="{ row }">
              <span class="text-gray-600">{{ row.commissionRate != null ? row.commissionRate + '%' : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="佣金金额" width="150" align="right">
            <template #default="{ row }">
              <span class="text-base font-bold text-gray-900">¥{{ row.amount?.toLocaleString() }}</span>
            </template>
          </el-table-column>
          <el-table-column label="核算人" width="110" align="center">
            <template #default="{ row }">
              <span class="text-sm text-gray-500">{{ row.financeName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType[row.status]" class="!rounded-full px-3" effect="light" size="small">
                {{ statusMap[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发放日期" width="160" align="center">
            <template #default="{ row }">
              <span class="text-xs text-gray-400">{{ row.issueTime || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="{ row }">
              <div class="flex gap-1 justify-center">
                <el-button v-if="row.status === 0" link type="primary" size="small"
                  @click="handleCalculate(row)" v-hasPermi="['finance:commission:calculate']">
                  核算
                </el-button>
                <el-button v-if="row.status === 1" link type="success" size="small"
                  @click="handleIssue(row)" v-hasPermi="['finance:commission:issue']">
                  发放
                </el-button>
                <el-button link type="info" size="small" @click="handlePreview(row)">
                  详情
                </el-button>
              </div>
            </template>
          </el-table-column>

          <template #empty>
            <div class="flex flex-col items-center py-16">
              <el-empty :description="loading ? '加载中...' : '暂无佣金记录'" />
            </div>
          </template>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="px-6 py-4 border-t border-gray-50 flex justify-end">
        <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
          :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          class="google-pagination" @size-change="getList" @current-change="getList" />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" direction="rtl" size="520px" :with-header="false"
      class="commission-detail-drawer">
      <div v-if="currentCommission" class="h-full flex flex-col">
        <!-- 顶部状态卡片 -->
        <div class="p-8 bg-gradient-to-br from-gray-50 to-white border-b border-gray-100">
          <div class="flex justify-between items-start mb-5">
            <div>
              <div class="text-xs font-bold text-[#1a73e8] tracking-widest uppercase mb-1">Commission Detail</div>
              <h2 class="text-xl font-bold text-gray-900">佣金详情</h2>
            </div>
            <el-button icon="Close" circle @click="drawerVisible = false"
              class="!border-none !bg-white/80 shadow-sm" />
          </div>
          <div class="flex gap-3">
            <div class="flex-1 bg-white p-4 rounded-xl shadow-sm border border-gray-100">
              <div class="text-xs text-gray-400 mb-1.5">当前状态</div>
              <el-tag :type="statusTagType[currentCommission.status]" class="!rounded-full px-3" effect="light">
                {{ statusMap[currentCommission.status] }}
              </el-tag>
            </div>
            <div class="flex-1 bg-white p-4 rounded-xl shadow-sm border border-gray-100">
              <div class="text-xs text-gray-400 mb-1.5">佣金金额</div>
              <div class="text-xl font-bold text-[#1a73e8]">¥{{ currentCommission.amount?.toLocaleString() }}</div>
            </div>
          </div>
        </div>

        <el-scrollbar class="flex-1">
          <div class="p-8 space-y-8">
            <!-- 基础信息 -->
            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                基础信息
              </h3>
              <div class="grid grid-cols-2 gap-y-4 text-sm bg-gray-50/70 p-5 rounded-2xl">
                <div class="text-gray-400">交易编号</div>
                <div class="text-right font-mono font-medium">{{ currentCommission.transactionNo || '-' }}</div>
                <div class="text-gray-400">销售人员</div>
                <div class="text-right font-medium">{{ currentCommission.salesName || '-' }}</div>
                <div class="text-gray-400">提成比例</div>
                <div class="text-right">{{ currentCommission.commissionRate != null ? currentCommission.commissionRate + '%' : '-' }}</div>
                <div class="text-gray-400">核算财务</div>
                <div class="text-right">{{ currentCommission.financeName || '-' }}</div>
                <div class="text-gray-400">核算时间</div>
                <div class="text-right">{{ currentCommission.calculateTime || '-' }}</div>
                <div class="text-gray-400">发放时间</div>
                <div class="text-right">{{ currentCommission.issueTime || '-' }}</div>
                <div class="text-gray-400">银行卡号</div>
                <div class="text-right font-mono">{{ currentCommission.bankCardNo || '-' }}</div>
                <div class="text-gray-400">创建时间</div>
                <div class="text-right">{{ currentCommission.createTime || '-' }}</div>
              </div>
            </section>

            <!-- 计算逻辑 -->
            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                计算逻辑
              </h3>
              <div class="p-5 rounded-2xl border border-dashed border-gray-200 bg-white">
                <div class="flex items-center justify-between text-sm text-gray-500 mb-3">
                  <span>成交总价</span>
                  <span class="font-mono">¥{{ currentCommission.amount && currentCommission.commissionRate
                    ? Math.round(currentCommission.amount / currentCommission.commissionRate * 100).toLocaleString()
                    : '?' }}</span>
                </div>
                <div class="flex items-center justify-between text-sm text-gray-500 mb-3">
                  <span>提成比例</span>
                  <span class="font-mono">{{ currentCommission.commissionRate }}%</span>
                </div>
                <el-divider class="!my-3" />
                <div class="flex items-center justify-between font-bold text-base text-[#1a73e8]">
                  <span>佣金金额</span>
                  <span>¥{{ currentCommission.amount?.toLocaleString() }}</span>
                </div>
              </div>
            </section>

            <!-- 财务备注 -->
            <section v-if="currentCommission.remark">
              <h3 class="text-sm font-bold text-gray-800 mb-4">财务备注</h3>
              <div class="p-4 rounded-2xl bg-yellow-50/70 border border-yellow-100 text-sm text-gray-600">
                {{ currentCommission.remark }}
              </div>
            </section>

            <!-- 操作时间轴 -->
            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4">操作记录</h3>
              <el-timeline class="ml-1">
                <el-timeline-item v-if="currentCommission.issueTime" :timestamp="currentCommission.issueTime" type="success">
                  佣金已发放
                  <p v-if="currentCommission.financeName" class="text-xs text-gray-400 mt-0.5">操作人：{{ currentCommission.financeName }}</p>
                </el-timeline-item>
                <el-timeline-item v-if="currentCommission.calculateTime" :timestamp="currentCommission.calculateTime" type="primary">
                  财务核算完成
                  <p v-if="currentCommission.financeName" class="text-xs text-gray-400 mt-0.5">核算人：{{ currentCommission.financeName }}</p>
                </el-timeline-item>
                <el-timeline-item v-if="currentCommission.createTime" :timestamp="currentCommission.createTime" type="info">
                  佣金记录创建（交易完成自动触发）
                </el-timeline-item>
              </el-timeline>
            </section>
          </div>
        </el-scrollbar>

        <!-- 底部操作 -->
        <div class="p-5 border-t border-gray-100 flex gap-3 bg-white">
          <el-button v-if="currentCommission.status === 0"
            type="primary" class="flex-1 !rounded-xl !h-11 !bg-[#1a73e8] border-none"
            @click="handleCalculate(currentCommission)" v-hasPermi="['finance:commission:calculate']">
            确认核算
          </el-button>
          <el-button v-if="currentCommission.status === 1"
            type="success" class="flex-1 !rounded-xl !h-11 border-none"
            @click="handleIssue(currentCommission)" v-hasPermi="['finance:commission:issue']">
            执行发放
          </el-button>
          <el-button class="flex-1 !rounded-xl !h-11" @click="drawerVisible = false">
            关闭
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCommissions, getCommission, calculateCommission, issueCommission, type Commission } from '@/api/finance/commission'

// --- 映射 ---
const statusMap: Record<number, string> = {
  0: '待核算', 1: '已核算', 2: '已发放'
}
const statusTagType: Record<number, string> = {
  0: 'info', 1: 'warning', 2: 'success'
}

// --- 统计 ---
const stats = reactive({
  pending: 0,
  calculated: 0,
  issued: 0,
  totalAmount: 0
})

// --- 状态 ---
const loading = ref(false)
const total = ref(0)
const commissionList = ref<Commission[]>([])
const multipleSelection = ref<Commission[]>([])
const currentCommission = ref<Commission | null>(null)
const drawerVisible = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined as number | undefined
})

// --- 列表加载 ---
const getList = async () => {
  loading.value = true
  try {
    const res = await listCommissions({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      status: queryParams.status
    })
    commissionList.value = res.data.records || []
    total.value = res.data.total || 0

    // 统计
    let pending = 0, calculated = 0, issued = 0, totalAmt = 0
    for (const c of commissionList.value) {
      totalAmt += c.amount || 0
      if (c.status === 0) pending++
      else if (c.status === 1) calculated++
      else if (c.status === 2) issued++
    }
    stats.pending = pending
    stats.calculated = calculated
    stats.issued = issued
    stats.totalAmount = totalAmt
  } catch {
    commissionList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryParams.status = undefined
  handleQuery()
}

const handleSelectionChange = (val: Commission[]) => {
  multipleSelection.value = val
}

// --- 详情 ---
const handlePreview = async (row: Commission) => {
  try {
    const res = await getCommission(row.id)
    currentCommission.value = res.data
  } catch {
    currentCommission.value = row
  }
  drawerVisible.value = true
}

// --- 核算 ---
const handleCalculate = (row: Commission) => {
  const name = row.salesName || `销售ID:${row.salesId}`
  ElMessageBox.confirm(
    `确认对 <strong>${name}</strong> 的佣金 <strong>¥${row.amount?.toLocaleString()}</strong> 进行核算？<br><br>核算后，该佣金将进入「已核算」状态，可执行发放。`,
    '核算确认',
    { confirmButtonText: '确认核算', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: true }
  ).then(async () => {
    try {
      await calculateCommission(row.id)
      ElMessage.success('核算完成')
      getList()
      if (currentCommission.value?.id === row.id) {
        currentCommission.value = { ...currentCommission.value, status: 1, calculateTime: new Date().toISOString() }
      }
    } catch {
      ElMessage.error('核算失败')
    }
  }).catch(() => {})
}

// --- 发放 ---
const handleIssue = (row: Commission) => {
  const name = row.salesName || `销售ID:${row.salesId}`
  ElMessageBox.confirm(
    `确认向 <strong>${name}</strong> 发放佣金 <strong style="color:#1a73e8">¥${row.amount?.toLocaleString()}</strong>？<br><br>系统将自动创建一笔佣金支出流水（已入账）。`,
    '发放确认',
    { confirmButtonText: '确认发放', cancelButtonText: '取消', type: 'success', dangerouslyUseHTMLString: true }
  ).then(async () => {
    try {
      await issueCommission(row.id)
      ElMessage.success('佣金发放成功')
      getList()
      if (currentCommission.value?.id === row.id) {
        currentCommission.value = { ...currentCommission.value, status: 2, issueTime: new Date().toISOString() }
      }
    } catch {
      ElMessage.error('发放失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.commission-container {
  height: calc(100vh - 110px);
}

.google-table-flat {
  --el-table-header-bg-color: #ffffff;
  --el-table-row-hover-bg-color: #f8f9fa;
  --el-table-border: none;
}

:deep(.el-table__header th) {
  color: #5f6368;
  font-weight: 600;
  border-bottom: 1px solid #f1f3f4 !important;
}

:deep(.el-table__row) {
  height: 62px;
}

:deep(.el-table__row td) {
  border-bottom: 1px solid #f1f3f4 !important;
}

:deep(.google-input-flat .el-input__wrapper) {
  border-radius: 8px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.2s;
}

:deep(.google-input-flat .el-input__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px #1a73e8 !important;
}

:deep(.google-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #1a73e8 !important;
}

:deep(.commission-detail-drawer .el-drawer__body) {
  padding: 0;
}
</style>
