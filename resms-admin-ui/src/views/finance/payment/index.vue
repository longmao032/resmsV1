<template>
  <div class="payment-management-container p-6 bg-gray-50 min-h-full">
    <div class="flex flex-col h-full bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <!-- 搜索区域 -->
      <div class="p-6 border-b border-gray-50">
        <el-form :inline="true" :model="queryParams" class="flex flex-wrap gap-x-8 gap-y-4">
          <el-form-item label="交易编号" class="!mb-0">
            <el-input v-model="queryParams.transactionNo" placeholder="TRX-..." clearable class="google-input-flat !w-48" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="款项类型" class="!mb-0">
            <el-select v-model="queryParams.paymentType" placeholder="全部类型" clearable class="google-input-flat !w-36">
              <el-option label="定金" :value="1" />
              <el-option label="首付款" :value="2" />
              <el-option label="尾款" :value="3" />
              <el-option label="中介费" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item label="审核状态" class="!mb-0">
            <el-select v-model="queryParams.paymentStatus" placeholder="状态" clearable class="google-input-flat !w-32">
              <el-option label="待审核" :value="0" />
              <el-option label="有效" :value="1" />
              <el-option label="已作废" :value="2" />
              <el-option label="已驳回" :value="3" />
            </el-select>
          </el-form-item>
          <div class="flex gap-2 ml-auto">
            <el-button type="primary" icon="Search" class="!rounded-lg !bg-[#1a73e8] border-none px-6" @click="handleQuery">查询</el-button>
            <el-button icon="Refresh" class="!rounded-lg px-6" @click="resetQuery">重置</el-button>
          </div>
        </el-form>
      </div>

      <!-- 操作栏 -->
      <div class="px-6 py-4 flex items-center justify-between">
        <div class="flex gap-3">
          <el-button type="success" icon="CircleCheck" class="!rounded-lg px-5 border-none" :disabled="!multipleSelection.length" @click="handleBatchAudit">批量审核</el-button>
          <el-button plain icon="Download" class="!rounded-lg px-5" @click="handleExport">导出流水</el-button>
        </div>
        <div class="flex gap-6 items-center">
          <div class="text-sm">
            本月收入: <span class="font-bold text-green-600">¥1,280,000.00</span>
          </div>
          <div class="text-sm">
            本月支出: <span class="font-bold text-red-500">¥45,000.00</span>
          </div>
        </div>
      </div>

      <!-- 表格区域 -->
      <div class="flex-1 px-6 pb-6 overflow-hidden">
        <el-table
          v-loading="loading"
          :data="paymentList"
          row-key="id"
          height="100%"
          class="google-table-flat"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column label="收据编号" prop="receiptNo" width="160">
            <template #default="{ row }">
              <span class="font-mono font-medium">{{ row.receiptNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="关联交易" prop="transactionNo" width="160">
            <template #default="{ row }">
              <el-link type="primary" :underline="false" class="font-mono">{{ row.transactionNo }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="款项详情" min-width="150">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <el-tag size="small" :type="row.flowType === 1 ? 'success' : 'danger'" effect="dark" class="!rounded-md !border-none">
                  {{ row.flowType === 1 ? '收' : '支' }}
                </el-tag>
                <span class="font-medium text-gray-700">{{ paymentTypeMap[row.paymentType] }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="150" align="right">
            <template #default="{ row }">
              <span class="text-lg font-bold" :class="row.flowType === 1 ? 'text-green-600' : 'text-red-500'">
                {{ row.flowType === 1 ? '+' : '-' }}¥{{ row.amount.toLocaleString() }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="支付方式" prop="paymentMethod" width="120" align="center" />
          <el-table-column label="付款人" prop="payerInfo" width="130" align="center">
            <template #default="{ row }">
              <span class="text-gray-700">{{ row.payerInfo || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType[row.paymentStatus]" class="!rounded-full px-4 low-sat-tag" effect="light">
                {{ statusMap[row.paymentStatus] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="经办/审核" width="150">
            <template #default="{ row }">
              <div class="text-xs text-gray-400">
                <div>经办: {{ row.financeName }}</div>
                <div v-if="row.auditName">审核: {{ row.auditName }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="发生时间" prop="createTime" width="160" align="center" />
          <el-table-column label="操作" width="180" fixed="right" align="center">
            <template #default="{ row }">
              <div class="flex gap-1 justify-center">
                <el-button link type="primary" @click="handleViewReceipt(row)">凭证</el-button>
                <el-button v-if="row.paymentStatus === 0" link type="success" @click="handleAudit(row)">审核</el-button>
                <el-button v-if="row.paymentStatus === 1" link type="danger" @click="handleVoid(row)">作废</el-button>
                <el-button v-if="row.paymentStatus === 1 && row.flowType === 1" link type="warning" @click="handleRefundApply(row)">退款</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="px-6 py-4 border-t border-gray-50 flex justify-end">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          class="google-pagination"
        />
      </div>
    </div>

    <!-- 凭证预览弹窗 -->
    <el-dialog v-model="receiptVisible" title="财务凭证详情" width="560px" class="google-dialog">
      <div v-if="currentPayment" class="space-y-6">
        <!-- 凭证图片 -->
        <div class="bg-gray-50 p-6 rounded-2xl border-2 border-dashed border-gray-200 text-center">
          <el-image v-if="currentPayment.proofUrl"
            :src="currentPayment.proofUrl"
            class="w-full h-48 rounded-lg shadow-sm mb-4 object-contain bg-white"
            style="cursor: zoom-in"
          />
          <div v-else class="w-full h-48 rounded-lg mb-4 flex items-center justify-center bg-gray-100 text-gray-300">
            <el-icon :size="48"><Picture /></el-icon>
          </div>
          <div class="text-xs text-gray-400">收据编号：{{ currentPayment.receiptNo }}</div>
        </div>

        <!-- 详情网格 -->
        <div class="grid grid-cols-2 gap-y-4 text-sm bg-gray-50/50 p-5 rounded-2xl">
          <div class="text-gray-400">交易编号</div>
          <div class="text-right font-mono">{{ currentPayment.transactionNo }}</div>

          <div class="text-gray-400">收据编号</div>
          <div class="text-right font-mono">{{ currentPayment.receiptNo }}</div>

          <div class="text-gray-400">款项类型</div>
          <div class="text-right">
            <el-tag size="small" :type="currentPayment.flowType === 1 ? 'success' : 'danger'" class="!rounded-md">
              {{ paymentTypeMap[currentPayment.paymentType] }}
            </el-tag>
          </div>

          <div class="text-gray-400">付款人</div>
          <div class="text-right font-medium">{{ currentPayment.payerInfo || '-' }}</div>

          <div class="text-gray-400">支付金额</div>
          <div class="text-right font-bold text-lg" :class="currentPayment.flowType === 1 ? 'text-green-600' : 'text-red-500'">
            {{ currentPayment.flowType === 1 ? '+' : '-' }}¥{{ currentPayment.amount.toLocaleString() }}
          </div>

          <div class="text-gray-400">支付方式</div>
          <div class="text-right">{{ currentPayment.paymentMethod }}</div>

          <div class="text-gray-400">经办财务</div>
          <div class="text-right">{{ currentPayment.financeName || '-' }}</div>

          <div class="text-gray-400">审核人</div>
          <div class="text-right">{{ currentPayment.auditName || '-' }}</div>

          <div class="text-gray-400">付款备注</div>
          <div class="text-right italic text-gray-500">{{ currentPayment.remark || '无' }}</div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3 pb-2">
          <el-button @click="receiptVisible = false" class="!rounded-xl">关闭</el-button>
          <el-button v-if="currentPayment?.paymentStatus === 0" type="primary" @click="handleAudit(currentPayment)" class="!rounded-xl !bg-[#1a73e8] border-none">审核处理</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 财务审核弹窗 -->
    <el-dialog v-model="auditVisible" title="财务审核" width="520px" class="google-dialog">
      <div v-if="auditTarget" class="space-y-5">
        <!-- 凭证预览（顶部） -->
        <div class="bg-gray-50 rounded-2xl border-2 border-dashed border-gray-200 overflow-hidden">
          <el-image v-if="auditTarget.proofUrl"
            :src="auditTarget.proofUrl"
            class="w-full h-44 object-contain bg-white" fit="contain" />
          <div v-else class="h-28 flex items-center justify-center bg-gray-100 text-gray-300">
            <el-icon :size="40"><Picture /></el-icon>
          </div>
        </div>

        <!-- 支付信息摘要 -->
        <div class="bg-blue-50/50 p-5 rounded-2xl border border-blue-100 grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
          <div class="text-gray-400">交易编号</div>
          <div class="text-right font-mono font-medium">{{ auditTarget.transactionNo }}</div>
          <div class="text-gray-400">收据编号</div>
          <div class="text-right font-mono">{{ auditTarget.receiptNo }}</div>
          <div class="text-gray-400">款项类型</div>
          <div class="text-right">
            <el-tag size="small" :type="auditTarget.flowType === 1 ? 'success' : 'danger'" class="!rounded-md">
              {{ paymentTypeMap[auditTarget.paymentType] }}
            </el-tag>
          </div>
          <div class="text-gray-400">付款人</div>
          <div class="text-right">{{ auditTarget.payerInfo || '-' }}</div>
          <div class="text-gray-400">提交金额</div>
          <div class="text-right font-bold">¥{{ auditTarget.amount.toLocaleString() }}</div>
          <div class="text-gray-400">支付方式</div>
          <div class="text-right">{{ auditTarget.paymentMethod }}</div>
        </div>

        <!-- 审核表单 -->
        <el-form :model="auditForm" label-position="top">
          <el-form-item label="实收金额（元）" required>
            <el-input-number v-model="auditForm.actualAmount" :min="0" :precision="2" :controls="false"
              class="!w-full google-input-flat" placeholder="请输入实际到账金额" />
          </el-form-item>

          <el-form-item label="审核结果" required>
            <el-radio-group v-model="auditForm.paymentStatus" class="flex gap-4">
              <el-radio-button :value="1">
                <el-icon class="mr-1"><CircleCheck /></el-icon> 审核通过
              </el-radio-button>
              <el-radio-button :value="0">
                <el-icon class="mr-1"><Close /></el-icon> 驳回
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item
            :label="auditForm.paymentStatus === 0 ? '驳回原因' : '审核备注'"
            :required="auditForm.paymentStatus === 0">
            <el-input v-model="auditForm.remark" type="textarea" :rows="3"
              :placeholder="auditForm.paymentStatus === 0 ? '请填写驳回原因（必填）' : '审核备注（选填）'"
              class="google-input-flat" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3 pb-2">
          <el-button @click="auditVisible = false" class="!rounded-xl">取消</el-button>
          <el-button type="primary" :loading="auditLoading" @click="handleAuditSubmit"
            class="!rounded-xl !bg-[#1a73e8] border-none">确认审核</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 退款申请弹窗 -->
    <el-dialog v-model="refundVisible" title="发起退款" width="480px" class="google-dialog">
      <div v-if="refundSource" class="space-y-5">
        <el-alert title="退款将产生一笔新的支出流水，需财务审核后生效。" type="warning" :closable="false" class="!rounded-xl" />

        <div class="bg-gray-50 p-4 rounded-2xl grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
          <div class="text-gray-400">原收据编号</div>
          <div class="text-right font-mono">{{ refundSource.receiptNo }}</div>
          <div class="text-gray-400">原收款金额</div>
          <div class="text-right font-bold">¥{{ refundSource.amount.toLocaleString() }}</div>
          <div class="text-gray-400">付款人</div>
          <div class="text-right">{{ refundSource.payerInfo || '-' }}</div>
        </div>

        <el-form :model="refundForm" label-position="top">
          <el-form-item label="退款金额（元）" required>
            <el-input-number v-model="refundForm.amount" :min="0.01" :precision="2" :controls="false"
              class="!w-full google-input-flat" />
          </el-form-item>
          <el-form-item label="退款方式" required>
            <el-select v-model="refundForm.refundMethod" class="!w-full google-input-flat">
              <el-option label="银行转账" value="银行转账" />
              <el-option label="微信支付" value="微信支付" />
              <el-option label="支付宝" value="支付宝" />
              <el-option label="现金" value="现金" />
            </el-select>
          </el-form-item>
          <el-form-item label="退款原因">
            <el-input v-model="refundForm.remark" type="textarea" :rows="3"
              placeholder="请填写退款原因" class="google-input-flat" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3 pb-2">
          <el-button @click="refundVisible = false" class="!rounded-xl">取消</el-button>
          <el-button type="warning" @click="handleRefundSubmit"
            class="!rounded-xl">提交退款申请</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, CircleCheck, Download, Picture, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPayment, auditPayment, voidPayment, applyRefund, exportPayment } from '@/api/finance/payment'

// --- 类型定义 ---
interface PaymentRecord {
  id: number
  receiptNo: string
  transactionNo: string
  paymentType: number
  flowType: number
  amount: number
  paymentMethod: string
  financeName: string
  auditName?: string
  paymentStatus: number
  payerInfo: string
  proofUrl?: string
  createTime: string
  remark?: string
}

// --- 映射 ---
const paymentTypeMap: Record<number, string> = {
  1: '交易定金',
  2: '房屋首付款',
  3: '交易尾款',
  4: '中介佣金'
}

const statusMap: Record<number, string> = {
  0: '待财务审核',
  1: '已确认入账',
  2: '已作废',
  3: '已驳回'
}

const statusTagType: Record<number, string> = {
  0: 'warning',
  1: 'success',
  2: 'danger',
  3: 'info'
}

const loading = ref(false)
const total = ref(0)
const paymentList = ref<PaymentRecord[]>([])
const multipleSelection = ref<PaymentRecord[]>([])
const currentPayment = ref<PaymentRecord | null>(null)
const receiptVisible = ref(false)

// --- 退款弹窗状态 ---
const refundVisible = ref(false)
const refundSource = ref<PaymentRecord | null>(null)
const refundForm = reactive({
  originalPaymentId: 0,
  amount: 0,
  refundMethod: '银行转账',
  payerInfo: '',
  proofUrl: '',
  remark: ''
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  transactionNo: '',
  paymentType: undefined,
  paymentStatus: undefined // 修正为 paymentStatus
})

// --- 方法 ---
const getList = async () => {
  loading.value = true
  try {
    const res = await listPayment(queryParams)
    paymentList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryParams.transactionNo = ''
  queryParams.paymentType = undefined
  queryParams.paymentStatus = undefined
  handleQuery()
}

const handleSelectionChange = (val: PaymentRecord[]) => {
  multipleSelection.value = val
}

const handleViewReceipt = (row: PaymentRecord) => {
  currentPayment.value = row
  receiptVisible.value = true
}

// --- 审核弹窗状态 ---
const auditVisible = ref(false)
const auditLoading = ref(false)
const auditTarget = ref<PaymentRecord | null>(null)
const auditForm = reactive({
  id: 0,
  paymentStatus: 1,
  actualAmount: 0,
  remark: ''
})

const handleAudit = (row: PaymentRecord) => {
  auditTarget.value = row
  auditForm.id = row.id
  auditForm.paymentStatus = 1
  auditForm.actualAmount = row.amount
  auditForm.remark = ''
  auditVisible.value = true
}

const handleAuditSubmit = async () => {
  if (auditForm.actualAmount <= 0) {
    ElMessage.warning('请输入实收金额')
    return
  }
  if (auditForm.paymentStatus === 0 && !auditForm.remark?.trim()) {
    ElMessage.warning('驳回必须填写原因')
    return
  }
  auditLoading.value = true
  try {
    await auditPayment({ ...auditForm })
    ElMessage.success(auditForm.paymentStatus === 1 ? '审核通过' : '已驳回')
    auditVisible.value = false
    receiptVisible.value = false
    getList()
  } finally {
    auditLoading.value = false
  }
}

/** 作废已通过的流水 */
const handleVoid = (row: PaymentRecord) => {
  ElMessageBox.confirm(
    `确认作废收据 ${row.receiptNo}（¥${row.amount.toLocaleString()}）吗？此操作不可撤销。`,
    '作废确认',
    { type: 'warning', confirmButtonText: '确认作废', cancelButtonText: '取消' }
  ).then(async () => {
    try {
      await voidPayment(row.id)
      ElMessage.success('已作废')
      getList()
    } catch { /* 错误由拦截器处理 */ }
  }).catch(() => {})
}

/** 退款申请 */
const handleRefundApply = (row: PaymentRecord) => {
  refundSource.value = row
  refundForm.originalPaymentId = row.id
  refundForm.amount = row.amount
  refundForm.refundMethod = row.paymentMethod
  refundForm.payerInfo = row.payerInfo
  refundForm.proofUrl = ''
  refundForm.remark = ''
  refundVisible.value = true
}

const handleRefundSubmit = async () => {
  if (refundForm.amount <= 0) {
    ElMessage.warning('请输入退款金额')
    return
  }
  try {
    await applyRefund({ ...refundForm })
    ElMessage.success('退款申请已提交，等待财务审核')
    refundVisible.value = false
    getList()
  } catch {
    // 错误由拦截器处理
  }
}

/** 导出流水 */
const handleExport = async () => {
  const params: any = {}
  if (queryParams.transactionNo) params.transactionNo = queryParams.transactionNo
  if (queryParams.paymentType !== undefined) params.paymentType = queryParams.paymentType
  if (queryParams.paymentStatus !== undefined) params.paymentStatus = queryParams.paymentStatus
  try {
    const res = await exportPayment(params)
    const blob = new Blob([res])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = decodeURIComponent(
      (res as any).headers?.['content-disposition']?.split('filename*=utf-8\'\'')?.[1] || '支付流水.xlsx'
    )
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('导出成功')
  } catch {
    // 错误由拦截器处理
  }
}

const handleBatchAudit = () => {
  const ids = multipleSelection.value.filter(p => p.paymentStatus === 0).map(p => p.id)
  if (ids.length === 0) return

  ElMessageBox.confirm(`确认批量审核通过选中的 ${ids.length} 笔流水吗？`, '批量操作', {
    type: 'warning'
  }).then(async () => {
    // 循环调用或后端增加批量接口，这里暂时演示循环调用
    for (const id of ids) {
       const row = multipleSelection.value.find(p => p.id === id)
       if (row) {
         await auditPayment({
           id: id,
           paymentStatus: 1,
           actualAmount: row.amount
         })
       }
    }
    ElMessage.success('批量审核完成')
    getList()
  })
}

onMounted(() => {
  getList()
})

</script>

<style scoped>
.payment-management-container {
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
  height: 64px;
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
</style>
