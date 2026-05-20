<template>
  <div class="log-management-container p-6 bg-gray-50/50 min-h-full">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="!border-none rounded-[24px] shadow-sm mb-6">
      <el-form :inline="true" :model="queryParams" class="search-form -mb-4">
        <el-form-item label="系统模块">
          <el-input 
            v-model="queryParams.module" 
            placeholder="请输入模块名称" 
            clearable 
            class="google-input"
            @keyup.enter="handleQuery" 
          />
        </el-form-item>
        <el-form-item label="操作人员">
          <el-input 
            v-model="queryParams.userName" 
            placeholder="请输入操作人员" 
            clearable 
            class="google-input"
            @keyup.enter="handleQuery" 
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryParams.businessType" placeholder="业务类型" clearable style="width: 120px" class="google-input">
            <el-option label="房源" value="HOUSE" />
            <el-option label="用户" value="USER" />
            <el-option label="角色" value="ROLE" />
            <el-option label="部门" value="DEPT" />
            <el-option label="认证" value="AUTH" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="操作状态" clearable style="width: 120px" class="google-input">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 240px"
            class="google-input"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" class="!rounded-full px-5" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" class="!rounded-full px-5" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="!border-none rounded-[24px] shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex gap-3">
            <el-button icon="Download" class="!rounded-full px-5" plain @click="handleExport" v-hasPermi="['system:log:export']">导出</el-button>
          </div>
          <div class="flex gap-2">
            <el-tooltip content="刷新数据" placement="top" effect="dark">
              <el-button 
                icon="Refresh" 
                class="!p-2 hover:bg-gray-100 rounded-full transition-colors !border-none" 
                @click="handleQuery" 
              />
            </el-tooltip>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="logList"
        class="log-table"
      >
        <el-table-column label="编号" prop="id" width="80" align="center" />
        <el-table-column label="模块" prop="module" min-width="100" />
        <el-table-column label="操作描述" prop="operationDesc" min-width="150" show-overflow-tooltip />
        <el-table-column label="风险" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getRiskLevelTag(row.riskLevel)" effect="dark" class="!rounded-md border-none">
              {{ getRiskLevelLabel(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人员" prop="userName" width="100" />
        <el-table-column label="IP地址" prop="ipAddress" width="130" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light" class="!rounded-md">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="center">
          <template #default="{ row }">
            <span :class="row.executeTime > 500 ? 'text-orange-500 font-bold' : 'text-gray-500'">
              {{ row.executeTime }}ms
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作时间" prop="operationTime" width="170" align="center" />
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-tooltip content="查看详情" placement="top" effect="dark">
              <el-button 
                link 
                type="primary" 
                icon="View" 
                class="!p-2 hover:bg-blue-50 rounded-full transition-colors"
                @click="handleView(row)"
              />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-6 flex justify-end">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          class="google-pagination"
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </el-card>

    <!-- 日志详情弹窗 -->
    <el-dialog v-model="dialogVisible" title="操作日志详情" width="850px" append-to-body class="google-dialog">
      <el-form :model="detailForm" label-width="100px" class="detail-form">
        <el-row>
          <el-col :span="12">
            <el-form-item label="操作模块：">{{ detailForm.module }} / {{ detailForm.operationType }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险等级：">
              <el-tag :type="getRiskLevelTag(detailForm.riskLevel)" effect="dark" size="small">
                {{ getRiskLevelLabel(detailForm.riskLevel) }}
              </el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作人员：">{{ detailForm.userName }} / {{ detailForm.ipAddress }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务类型：">{{ detailForm.businessType || '无' }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="操作描述：">{{ detailForm.operationDesc }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="请求地址：">{{ detailForm.requestUrl }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="请求方式：">
              <el-tag size="small">{{ detailForm.requestMethod }}</el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行耗时：">
              <span :class="detailForm.executeTime > 500 ? 'text-orange-500 font-bold' : ''">
                {{ detailForm.executeTime }} ms
              </span>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="浏览器UA：">
              <span class="text-xs text-gray-500">{{ detailForm.userAgent }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="请求参数：">
              <div class="code-box bg-gray-900 text-green-400 p-4 rounded-lg font-mono text-xs overflow-auto max-h-40">
                {{ detailForm.requestParams }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="返回结果：">
              <div class="code-box bg-gray-50 text-gray-700 p-4 rounded-lg border font-mono text-xs overflow-auto max-h-40">
                {{ detailForm.responseResult }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="detailForm.status === 0">
            <el-form-item label="错误信息：">
              <div class="text-red-500 bg-red-50 p-3 rounded-lg border border-red-100 text-xs font-mono">
                {{ detailForm.errorMsg }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作状态：">
              <el-tag :type="detailForm.status === 1 ? 'success' : 'danger'">
                {{ detailForm.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作时间：">{{ detailForm.operationTime }}</el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, Delete, Download, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listLog, type OperationLog } from '@/api/system/log'

// --- 状态与参数 ---
const loading = ref(false)
const logList = ref<OperationLog[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dateRange = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  module: '',
  userName: '',
  businessType: undefined,
  status: undefined,
  beginTime: undefined as string | undefined,
  endTime: undefined as string | undefined
})

const detailForm = ref<any>({})

// --- 初始化 ---
onMounted(() => {
  handleQuery()
})

// --- 方法 ---
const getRiskLevelTag = (level: number) => {
  const map: any = {
    1: 'danger',
    2: 'warning',
    3: 'primary'
  }
  return map[level] || 'info'
}

const getRiskLevelLabel = (level: number) => {
  const map: any = {
    1: '高危',
    2: '中等',
    3: '普通'
  }
  return map[level] || '未知'
}

const handleQuery = async () => {
  loading.value = true
  if (dateRange.value && dateRange.value.length === 2) {
    queryParams.beginTime = dateRange.value[0]
    queryParams.endTime = dateRange.value[1]
  } else {
    queryParams.beginTime = undefined
    queryParams.endTime = undefined
  }
  
  try {
    const { data } = await listLog(queryParams)
    logList.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: 10,
    module: '',
    userName: '',
    businessType: undefined,
    status: undefined,
    beginTime: undefined,
    endTime: undefined
  })
  dateRange.value = []
  handleQuery()
}

const handleView = (row: OperationLog) => {
  detailForm.value = { ...row }
  dialogVisible.value = true
}

const handleExport = () => {
  ElMessage.success('导出任务已提交')
}

</script>

<style scoped>
.log-management-container {
  height: calc(100vh - 120px);
}

/* 表格样式优化 */
.log-table {
  --el-table-header-bg-color: transparent;
  --el-table-row-hover-bg-color: #f8f9fa;
}

:deep(.el-table__header th) {
  color: #444746;
  font-weight: 600;
  border-bottom: 1px solid #f1f5f9;
}

/* 输入框优化 */
:deep(.google-input .el-input__wrapper) {
  border-radius: 8px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  border: 1px solid transparent;
}

:deep(.google-input .el-input__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px #1a73e8 !important;
}

.search-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #444746;
}

:deep(.el-card) {
  border-radius: 24px !important;
  box-shadow: 0 1px 2px rgba(60,64,67,0.3), 0 1px 3px 1px rgba(60,64,67,0.15) !important;
}

:deep(.el-card__header) {
  border-bottom: 1px solid #f1f5f9;
  padding: 20px 24px;
}

.code-box {
  word-break: break-all;
  white-space: pre-wrap;
}
</style>

<style>
/* 全局弹窗样式优化 */
.google-dialog {
  border-radius: 28px !important;
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(0,0,0,0.1) !important;
}

.google-dialog .el-dialog__header {
  margin-right: 0;
  padding: 24px 24px 16px;
}

.google-dialog .el-dialog__title {
  font-size: 24px;
  color: #1f1f1f;
}

.detail-form .el-form-item {
  margin-bottom: 12px;
  border-bottom: 1px solid #f8f9fa;
  padding-bottom: 8px;
}

.detail-form .el-form-item:last-child {
  border-bottom: none;
}
</style>
