<template>
  <div class="history-page-wrapper h-full relative">
    <div class="history-management-container p-6 bg-gray-50 min-h-full">
      <div class="flex flex-col h-full bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <!-- 搜索与筛选 -->
      <div class="p-6 border-b border-gray-50">
        <el-form :inline="true" :model="queryParams" class="flex flex-wrap gap-x-8 gap-y-4">
          <el-form-item label="客户名称" class="!mb-0">
            <el-input v-model="queryParams.customerName" placeholder="客户姓名" clearable class="google-input-flat !w-40" />
          </el-form-item>
          <el-form-item label="行为类型" class="!mb-0">
            <el-select v-model="queryParams.actionType" placeholder="全部" clearable class="google-input-flat !w-36">
              <el-option label="线上浏览" value="view" />
              <el-option label="电话咨询" value="call" />
              <el-option label="实地带看" value="visit" />
            </el-select>
          </el-form-item>
          <div class="flex gap-2 ml-auto">
            <el-button v-hasPermi="['trade:history:query']" type="primary" icon="Search" class="!rounded-lg !bg-[#1a73e8] border-none px-6" @click="handleQuery">筛选</el-button>
            <el-button v-hasPermi="['trade:history:query']" icon="Refresh" class="!rounded-lg px-6" @click="resetQuery">重置</el-button>
          </div>
        </el-form>
      </div>

      <!-- 操作工具栏 -->
      <div class="px-6 py-4 flex items-center justify-between border-b border-gray-50">
        <div class="flex gap-3">
          <el-button v-hasPermi="['trade:history:add']" type="primary" icon="Plus" class="!rounded-lg px-5 !bg-[#1a73e8] border-none"
            @click="handleAdd">录入交互轨迹</el-button>
          <el-button v-hasPermi="['trade:history:export']" plain icon="Download" class="!rounded-lg px-5">导出分析报告</el-button>
        </div>
        <div class="flex items-center gap-4 text-xs text-gray-500">
          <span class="flex items-center gap-1"><el-icon>
              <InfoFilled />
            </el-icon> 自动记录线上浏览时长</span>
        </div>
      </div>

      <!-- 表格区域 -->
      <div class="flex-1 px-6 pb-6 overflow-hidden">
        <el-table v-loading="loading" :data="historyList" row-key="id" height="100%" class="google-table-flat">
          <el-table-column label="客户信息" width="160">
            <template #default="{ row }">
              <div class="flex items-center gap-3">
                <el-avatar :size="32" class="!bg-purple-100 !text-purple-600 font-bold text-xs">
                  {{ row.customerName?.charAt(0) || '?' }}
                </el-avatar>
                <span class="font-bold text-[#1f1f1f]">{{ row.customerName || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="交互房源" min-width="250">
            <template #default="{ row }">
              <div class="flex items-center gap-3">
                <el-image :src="row.houseCover" class="w-12 h-12 rounded-lg object-cover bg-gray-100">
                  <template #error>
                    <div class="w-full h-full flex items-center justify-center bg-gray-100 text-gray-300">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div class="flex flex-col">
                  <span class="font-medium text-[#1f1f1f] truncate w-64">{{ row.houseTitle }}</span>
                  <span class="text-xs text-gray-400">{{ row.houseType }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="行为类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="actionTagMap[row.actionType]" class="!rounded-full px-4 low-sat-tag" effect="light">
                {{ actionLabelMap[row.actionType] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="交互时长" width="160" align="center">
            <template #default="{ row }">
              <div class="flex flex-col items-center">
                <span class="text-xs font-mono mb-1"
                  :class="row.duration > 300 ? 'text-red-500 font-bold' : 'text-gray-500'">{{
                    formatDuration(row.duration) }}</span>
                <el-progress :percentage="Math.min(100, (row.duration / 600) * 100)" :show-text="false"
                  :stroke-width="3" class="w-20" :color="row.duration > 300 ? '#ea4335' : '#1a73e8'" />
              </div>
            </template>
          </el-table-column>
          <el-table-column label="意向评估" width="150" align="center">
            <template #default="{ row }">
              <el-rate v-model="row.interestLevel" disabled />
            </template>
          </el-table-column>
          <el-table-column label="最后交互" prop="viewTime" width="180" align="center">
            <template #default="{ row }">
              <span class="text-xs text-gray-400">{{ row.viewTime }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-hasPermi="['trade:history:query']" link type="primary" @click="handleViewDetail(row)">详细轨迹</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="px-6 py-4 border-t border-gray-50 flex justify-end">
        <el-pagination background layout="total, prev, pager, next" 
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total" 
          @current-change="getList"
          class="google-pagination" />
      </div>
    </div>

    <!-- 新增交互轨迹对话框 -->
    <el-dialog v-model="historyDialogVisible" title="录入客户交互轨迹" width="600px" class="google-dialog" destroy-on-close>
      <div class="px-2">
        <el-form :model="historyForm" :rules="historyRules" ref="historyFormRef" label-position="top">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="选择客户" prop="customerId">
                <el-select v-model="historyForm.customerId" placeholder="搜索客户" class="google-input-flat !w-full"
                  filterable>
                  <el-option v-for="c in customerOptions" :key="c.id" :label="c.realName" :value="c.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="关联房源" prop="houseId">
                <el-select v-model="historyForm.houseId" placeholder="搜索房源项目" class="google-input-flat !w-full"
                  filterable>
                  <el-option v-for="h in houseOptions" :key="h.id" :label="`${h.projectName} - ${h.houseNo}`" :value="h.id">
                    <div class="flex items-center justify-between w-full">
                      <span>{{ h.projectName }} - {{ h.houseNo }}</span>
                      <el-tag :type="h.houseType === 1 ? 'danger' : (h.houseType === 2 ? 'warning' : 'success')" size="small" effect="plain" class="!rounded-full border-none !bg-opacity-10 ml-2">
                        {{ h.houseType === 1 ? '新房' : (h.houseType === 2 ? '二手房' : '租房') }}
                      </el-tag>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="行为类型" prop="actionType">
            <el-radio-group v-model="historyForm.actionType" class="google-radio-group flex gap-2">
              <el-radio-button value="chat">在线咨询</el-radio-button>
              <el-radio-button value="call">电话咨询</el-radio-button>
              <el-radio-button value="visit">实地带看</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="交互时长 (分钟)">
                <el-input-number v-model="historyForm.duration" :min="1" :controls="false"
                  class="google-input-flat !w-full" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="交互时间">
                <el-date-picker v-model="historyForm.viewTime" type="datetime" placeholder="选择交互发生时间"
                  class="google-input-flat !w-full" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="意向评估">
            <div class="flex items-center gap-4">
              <el-rate v-model="historyForm.interest" class="!h-10" />
              <span class="text-xs text-gray-400">基于此次交互对房源的兴趣程度</span>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex gap-3 justify-end px-4 pb-4">
          <el-button @click="historyDialogVisible = false" class="!rounded-xl px-6">取消</el-button>
          <el-button v-hasPermi="['trade:history:add']" type="primary" class="!rounded-xl px-8 !bg-[#1a73e8] border-none"
            @click="handleSubmitHistory">保存轨迹</el-button>
        </div>
      </template>
    </el-dialog>
  </div>

  <!-- 轨迹详情抽屉 -->
  <el-drawer v-model="detailDrawerVisible" direction="rtl" size="480px" :with-header="false" class="history-detail-drawer">
    <div class="h-full flex flex-col bg-white">
      <template v-if="selectedFootprint">
        <div class="p-8 border-b border-gray-50 bg-[#f8f9fa]">
          <div class="flex justify-between items-start mb-4">
            <div>
              <div class="text-xs font-bold text-[#1a73e8] tracking-widest uppercase mb-1">Footprint Detail</div>
              <h2 class="text-xl font-bold text-[#202124]">客户交互轨迹</h2>
            </div>
            <el-button icon="Close" circle @click="detailDrawerVisible = false"
              class="!border-none !bg-white shadow-sm" />
          </div>
        </div>

        <el-scrollbar class="flex-1">
          <div class="p-8 space-y-6">
            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                客户信息
              </h3>
              <div class="flex items-center gap-4 p-4 rounded-2xl bg-gray-50 border border-gray-100">
                <el-avatar :size="48" class="!bg-purple-100 !text-purple-600 font-bold">
                  {{ selectedFootprint.customerName?.charAt(0) || '?' }}
                </el-avatar>
                <div>
                  <div class="font-bold text-lg">{{ selectedFootprint.customerName }}</div>
                </div>
              </div>
            </section>

            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                关联房源
              </h3>
              <div class="flex items-start gap-3 p-4 rounded-2xl bg-gray-50 border border-gray-100">
                <el-image :src="selectedFootprint.houseCover" class="w-20 h-20 rounded-xl object-cover bg-gray-100 flex-shrink-0" />
                <div class="min-w-0 flex-1">
                  <div class="font-medium text-[#1f1f1f] truncate">{{ selectedFootprint.houseTitle }}</div>
                  <div class="text-xs text-gray-400 mt-1">{{ selectedFootprint.houseNo }}</div>
                  <div class="flex flex-wrap gap-1 mt-2">
                    <el-tag size="small" effect="plain" class="!rounded-full">{{ selectedFootprint.houseType }}</el-tag>
                    <el-tag v-if="selectedFootprint.layout" size="small" effect="plain" class="!rounded-full">{{ selectedFootprint.layout }}</el-tag>
                    <el-tag v-if="selectedFootprint.area" size="small" effect="plain" class="!rounded-full">{{ selectedFootprint.area }}㎡</el-tag>
                    <el-tag v-if="selectedFootprint.orientation" size="small" effect="plain" class="!rounded-full">{{ selectedFootprint.orientation }}</el-tag>
                    <el-tag v-if="selectedFootprint.decoration" size="small" effect="plain" class="!rounded-full">{{ selectedFootprint.decoration }}</el-tag>
                  </div>
                </div>
              </div>
              <div v-if="selectedFootprint.floorLabel || selectedFootprint.unitPrice || selectedFootprint.totalPrice || selectedFootprint.rentPrice" class="mt-3 grid grid-cols-2 gap-3">
                <div v-if="selectedFootprint.floorLabel" class="p-3 rounded-xl bg-white border border-gray-100">
                  <div class="text-xs text-gray-400">楼层</div>
                  <div class="text-sm font-bold mt-0.5">{{ selectedFootprint.floorLabel }}</div>
                </div>
                <div v-if="selectedFootprint.unitPrice" class="p-3 rounded-xl bg-white border border-gray-100">
                  <div class="text-xs text-gray-400">单价</div>
                  <div class="text-sm font-bold mt-0.5 text-[#1a73e8]">{{ selectedFootprint.unitPrice }} 元/㎡</div>
                </div>
                <div v-if="selectedFootprint.totalPrice" class="p-3 rounded-xl bg-white border border-gray-100">
                  <div class="text-xs text-gray-400">总价</div>
                  <div class="text-sm font-bold mt-0.5 text-[#ea4335]">{{ selectedFootprint.totalPrice }} 万元</div>
                </div>
                <div v-if="selectedFootprint.rentPrice" class="p-3 rounded-xl bg-white border border-gray-100">
                  <div class="text-xs text-gray-400">月租</div>
                  <div class="text-sm font-bold mt-0.5 text-[#34a853]">{{ selectedFootprint.rentPrice }} 元/月</div>
                </div>
              </div>
            </section>

            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                交互信息
              </h3>
              <div class="grid grid-cols-2 gap-4">
                <div class="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">行为类型</div>
                  <el-tag :type="actionTagMap[selectedFootprint.actionType]" class="!rounded-full" effect="light">
                    {{ actionLabelMap[selectedFootprint.actionType] }}
                  </el-tag>
                </div>
                <div class="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">交互时长</div>
                  <div class="font-bold">{{ formatDuration(selectedFootprint.duration) }}</div>
                </div>
                <div class="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">意向评估</div>
                  <el-rate v-model="selectedFootprint.interestLevel" disabled />
                </div>
                <div class="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">交互时间</div>
                  <div class="font-bold text-sm">{{ selectedFootprint.viewTime }}</div>
                </div>
              </div>
              <div v-if="selectedFootprint.content" class="mt-4 p-4 rounded-2xl bg-yellow-50/50 border border-yellow-100">
                <div class="text-xs text-yellow-500 mb-1">备注</div>
                <div class="text-sm">{{ selectedFootprint.content }}</div>
              </div>
            </section>
          </div>
        </el-scrollbar>
      </template>
    </div>
  </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Search, Refresh, Plus, Download, InfoFilled, StarFilled, Picture, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listHistory, addHistory } from '@/api/trade/history'
import { listCustomer } from '@/api/trade/customer'
import { listHouse } from '@/api/house/house'

// --- 状态管理 ---
const loading = ref(false)
const historyList = ref<any[]>([])
const customerOptions = ref<any[]>([])
const houseOptions = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  customerName: '',
  actionType: ''
})

// 获取列表数据
const getList = async () => {
  loading.value = true
  try {
    const response: any = await listHistory(queryParams)
    historyList.value = response.data.records
    total.value = response.data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryParams.customerName = ''
  queryParams.actionType = ''
  handleQuery()
}

// 初始加载
getList()

// --- 表单状态 ---
const historyDialogVisible = ref(false)
const historyFormRef = ref()
const historyForm = reactive({
  customerId: undefined,
  houseId: undefined,
  actionType: 'chat',
  duration: 10,
  interest: 3,
  viewTime: new Date()
})

const historyRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  houseId: [{ required: true, message: '请选择房源', trigger: 'change' }],
  actionType: [{ required: true, message: '请选择行为类型', trigger: 'change' }]
}

const actionTagMap: Record<string, string> = {
  view: 'info',
  chat: 'primary',
  call: 'warning',
  visit: 'success'
}

const actionLabelMap: Record<string, string> = {
  view: '线上浏览',
  chat: '在线咨询',
  call: '电话咨询',
  visit: '实地带看'
}


const formatDuration = (s: number) => {
  const m = Math.floor(s / 60)
  const rs = s % 60
  return `${m}m ${rs}s`
}

// --- 轨迹详情 ---
const detailDrawerVisible = ref(false)
const selectedFootprint = ref<any>(null)

const handleViewDetail = (row: any) => {
  selectedFootprint.value = row
  detailDrawerVisible.value = true
}

const handleAdd = async () => {
  try {
    const [custRes, houseRes] = await Promise.all([
      listCustomer({ pageSize: 100 }),
      listHouse({ pageSize: 100 })
    ])
    customerOptions.value = custRes.data.records
    houseOptions.value = houseRes.data.records
    
    historyForm.customerId = undefined
    historyForm.houseId = undefined
    historyForm.actionType = 'chat'
    historyForm.duration = 10
    historyForm.interest = 3
    historyForm.viewTime = new Date()
    historyDialogVisible.value = true
  } catch (err) {
    ElMessage.error('加载基础数据失败')
  }
}

const handleSubmitHistory = async () => {
  const valid = await historyFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await addHistory({
      customerId: historyForm.customerId,
      resourceType: 1,
      resourceId: historyForm.houseId,
      actionType: historyForm.actionType,
      duration: (historyForm.duration || 0) * 60,
      interestLevel: historyForm.interest,
      viewTime: historyForm.viewTime
    })
    ElMessage.success('交互轨迹录入成功')
    historyDialogVisible.value = false
    getList()
  } catch {
    ElMessage.error('录入失败')
  }
}
</script>

<style scoped>
.history-management-container {
  height: calc(100vh - 110px);
}

/* Google 扁平化表格 */
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
  height: 72px;
}

:deep(.el-table__row td) {
  border-bottom: 1px solid #f1f3f4 !important;
}

/* 输入框扁平化规格 */
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

/* 低饱和度胶囊标签 */
.low-sat-tag {
  border: none;
  font-weight: 600;
}

/* 分页器 */
:deep(.google-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #1a73e8 !important;
}

/* 对话框圆角 */
:deep(.google-dialog) {
  border-radius: 28px;
  overflow: hidden;
}

:deep(.google-dialog .el-dialog__header) {
  padding: 24px 32px 16px;
  margin-right: 0;
  border-bottom: 1px solid #f1f3f4;
}

:deep(.google-dialog .el-dialog__title) {
  font-size: 20px;
  font-weight: bold;
  color: #202124;
}

:deep(.google-dialog .el-dialog__body) {
  padding: 24px 32px;
}

/* Radio Button 扁平化 */
:deep(.google-radio-group .el-radio-button__inner) {
  border-radius: 12px !important;
  border: 1px solid #f1f3f4 !important;
  background-color: #f8f9fa !important;
  color: #5f6368 !important;
  margin-right: 8px;
  padding: 10px 20px;
  box-shadow: none !important;
}

:deep(.google-radio-group .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #1a73e8 !important;
  color: #fff !important;
  border-color: #1a73e8 !important;
}
</style>
