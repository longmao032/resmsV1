<template>
  <div class="transfer-management-container p-6 bg-gray-50 min-h-full">
    <div class="flex flex-col h-full bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <!-- 搜索区域 -->
      <div class="p-6 border-b border-gray-50">
        <el-form :inline="true" :model="queryParams" class="flex flex-wrap gap-x-8 gap-y-4">
          <el-form-item label="交易编号" class="!mb-0">
            <el-input v-model="queryParams.transactionNo" placeholder="TRX-2026..." clearable
              class="google-input-flat !w-48" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="房产地址" class="!mb-0">
            <el-input v-model="queryParams.houseAddress" placeholder="小区名称" clearable class="google-input-flat !w-40"
              @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="过户状态" class="!mb-0">
            <el-select v-model="queryParams.transferStatus" placeholder="全部" clearable class="google-input-flat !w-36">
              <el-option label="待过户" :value="0" />
              <el-option label="已完成" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="交易状态" class="!mb-0">
            <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="google-input-flat !w-36">
              <el-option label="已付首付" :value="2" />
              <el-option label="已过户" :value="3" />
            </el-select>
          </el-form-item>
          <div class="flex gap-2 ml-auto">
            <el-button v-hasPermi="['trade:transfer:query']" type="primary" icon="Search"
              class="!rounded-lg !bg-[#1a73e8] border-none px-6" @click="handleQuery">查询</el-button>
            <el-button v-hasPermi="['trade:transfer:query']" icon="Refresh" class="!rounded-lg px-6"
              @click="resetQuery">重置</el-button>
          </div>
        </el-form>
      </div>

      <!-- 操作栏 -->
      <div class="px-6 py-4 flex items-center justify-between">
        <div class="flex gap-3">
          <el-button v-hasPermi="['trade:transfer:add']" type="primary" icon="Plus"
            class="!rounded-lg px-5 !bg-[#1a73e8] border-none" @click="handleCreateTransfer">创建过户记录</el-button>
          <el-button v-hasPermi="['trade:transfer:query']" plain icon="Refresh" class="!rounded-lg px-5"
            @click="getList">刷新列表</el-button>
        </div>
        <div class="flex gap-1 text-gray-400 text-sm">
          共 <span class="font-bold text-[#1a73e8] mx-1">{{ total }}</span> 条过户记录
        </div>
      </div>

      <!-- 表格区域 -->
      <div class="flex-1 px-6 pb-6 overflow-hidden">
        <el-table v-loading="loading" :data="transferList" row-key="id" height="100%" class="google-table-flat"
          @row-click="handleDetail">
          <el-table-column label="交易编号" width="170">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <el-tag size="small" :type="row.transferStatus === 1 ? 'success' : 'warning'"
                  class="!rounded-md !border-none !min-w-[48px] text-center">
                  {{ row.transferStatus === 1 ? '已过户' : '待过户' }}
                </el-tag>
                <span class="font-mono text-[#1a73e8] font-medium text-sm cursor-pointer hover:underline">
                  {{ row.transactionNo }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="关联房源" min-width="220">
            <template #default="{ row }">
              <div class="flex items-center gap-3">
                <el-image :src="row.houseCover"
                  class="w-12 h-12 rounded-lg object-cover bg-gray-100">
                  <template #error>
                    <div class="w-full h-full flex items-center justify-center bg-gray-100 text-gray-400">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div class="flex flex-col min-w-0">
                  <span class="font-medium text-[#1f1f1f] truncate max-w-[160px]">{{ row.projectName }}</span>
                  <span class="text-xs text-gray-400">{{ row.houseNo }} | {{ row.area }}㎡</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="客户信息" width="150">
            <template #default="{ row }">
              <div class="flex flex-col">
                <span class="font-medium">{{ row.customerName }}</span>
                <span class="text-xs text-gray-400">{{ row.customerPhone }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="过户编号" width="180">
            <template #default="{ row }">
              <span v-if="row.transferNo" class="font-mono text-sm text-gray-500">{{ row.transferNo }}</span>
              <span v-else class="text-xs text-gray-300">—</span>
            </template>
          </el-table-column>
          <el-table-column label="产权证号" width="160">
            <template #default="{ row }">
              <span v-if="row.certificateNo" class="font-mono text-sm">{{ row.certificateNo }}</span>
              <span v-else class="text-xs text-gray-300">—</span>
            </template>
          </el-table-column>
          <el-table-column label="过户日期" width="120" align="center">
            <template #default="{ row }">
              <span v-if="row.transferDate" class="text-sm">{{ formatDate(row.transferDate) }}</span>
              <span v-else class="text-xs text-gray-300">—</span>
            </template>
          </el-table-column>
          <el-table-column label="过户文件" width="100" align="center">
            <template #default="{ row }">
              <el-button v-hasPermi="['trade:transfer:query']" link type="primary" size="small"
                @click.stop="handleViewDocuments(row)">
                {{ row.docCount || 0 }} 个文件
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right" align="center">
            <template #default="{ row }">
              <div class="flex gap-1 justify-center">
                <el-button v-if="row.transferStatus === 0" v-hasPermi="['trade:transfer:add']" link type="primary"
                  size="small" @click.stop="handleEditTransfer(row)">编辑</el-button>
                <el-button v-if="row.transferStatus === 0" v-hasPermi="['trade:transfer:complete']" link type="success"
                  size="small" @click.stop="handleConfirmComplete(row)">确认过户</el-button>
                <el-button v-if="row.transferId" v-hasPermi="['trade:transfer:upload']" link type="warning"
                  size="small" @click.stop="handleUploadDoc(row)">上传文件</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="px-6 py-4 border-t border-gray-50 flex justify-end">
        <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
          :total="total" layout="total, prev, pager, next" background class="google-pagination" />
      </div>
    </div>

    <!-- 创建/编辑过户对话框 -->
    <el-dialog v-model="transferDialogVisible" :title="isEditMode ? '编辑过户记录' : '创建过户记录'" width="650px"
      class="google-dialog" destroy-on-close>
      <div class="px-4 py-2">
        <el-form :model="transferForm" :rules="transferRules" ref="transferFormRef" label-position="top">
          <el-form-item label="选择交易订单" prop="transactionId">
            <el-select v-model="transferForm.transactionId" placeholder="选择待过户的交易订单" class="google-input-flat !w-full"
              filterable :disabled="isEditMode" @change="onTransactionChange">
              <el-option v-for="t in transactionOptions" :key="t.id" :label="`${t.transactionNo} - ${t.customer.realName} - ${t.house.projectName}`"
                :value="t.id" />
            </el-select>
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="不动产权证书号" prop="certificateNo">
                <el-input v-model="transferForm.certificateNo" placeholder="如：粤(2026)深圳市不动产权第XXXX号"
                  class="google-input-flat !w-full" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="不动产登记中心" prop="registrationCenter">
                <el-input v-model="transferForm.registrationCenter" placeholder="如：深圳市不动产登记中心"
                  class="google-input-flat !w-full" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="过户日期" prop="transferDate">
            <el-date-picker v-model="transferForm.transferDate" type="datetime" placeholder="选择过户办理日期"
              class="google-input-flat !w-full" value-format="YYYY-MM-DDTHH:mm:ss" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="transferForm.remark" type="textarea" :rows="3" placeholder="备注信息..."
              class="google-input-flat" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex gap-3 justify-end px-4 pb-4">
          <el-button @click="transferDialogVisible = false" class="!rounded-xl px-6">取消</el-button>
          <el-button type="primary" class="!rounded-xl px-8 !bg-[#1a73e8] border-none"
            @click="handleSubmitTransfer">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 过户详情抽屉 -->
    <el-drawer v-model="detailDrawerVisible" direction="rtl" size="520px" :with-header="false" class="transfer-detail-drawer">
      <div v-if="selectedTransfer" class="h-full flex flex-col bg-white">
        <!-- 头部 -->
        <div class="p-8 border-b border-gray-50 bg-[#f8f9fa]">
          <div class="flex justify-between items-start mb-4">
            <div>
              <div class="text-xs font-bold text-[#1a73e8] tracking-widest uppercase mb-1">Transfer Record</div>
              <h2 class="text-xl font-bold text-[#202124]">{{ selectedTransfer.transactionNo }}</h2>
            </div>
            <el-button icon="Close" circle @click="detailDrawerVisible = false"
              class="!border-none !bg-white shadow-sm" />
          </div>
          <div class="flex gap-3">
            <div class="flex-1 bg-white p-4 rounded-xl shadow-sm border border-gray-100">
              <div class="text-xs text-gray-400 mb-1">过户状态</div>
              <el-tag :type="selectedTransfer.transferStatus === 1 ? 'success' : 'warning'"
                class="!rounded-full px-3" effect="light">
                {{ selectedTransfer.transferStatus === 1 ? '已完成' : '待过户' }}
              </el-tag>
            </div>
            <div class="flex-1 bg-white p-4 rounded-xl shadow-sm border border-gray-100">
              <div class="text-xs text-gray-400 mb-1">过户编号</div>
              <div class="font-mono font-bold text-sm truncate">{{ selectedTransfer.transferNo || '—' }}</div>
            </div>
          </div>
        </div>

        <el-scrollbar class="flex-1">
          <div class="p-8 space-y-8">
            <!-- 基础信息 -->
            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                <el-icon class="text-[#1a73e8]"><InfoFilled /></el-icon> 基础信息
              </h3>
              <div class="grid grid-cols-2 gap-4">
                <div class="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">客户姓名</div>
                  <div class="font-bold">{{ selectedTransfer.customerName }}</div>
                </div>
                <div class="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">联系电话</div>
                  <div class="font-bold">{{ selectedTransfer.customerPhone }}</div>
                </div>
                <div class="p-4 rounded-2xl bg-gray-50 border border-gray-100 col-span-2">
                  <div class="text-xs text-gray-400 mb-1">房产地址</div>
                  <div class="font-bold">{{ selectedTransfer.houseAddress }}</div>
                </div>
              </div>
            </section>

            <!-- 过户信息 -->
            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                <el-icon class="text-[#1a73e8]"><Finished /></el-icon> 过户信息
              </h3>
              <div class="grid grid-cols-2 gap-4">
                <div class="p-4 rounded-2xl bg-blue-50/50 border border-blue-100">
                  <div class="text-xs text-blue-400 mb-1">不动产权证书号</div>
                  <div class="font-bold text-blue-700">{{ selectedTransfer.certificateNo || '—' }}</div>
                </div>
                <div class="p-4 rounded-2xl bg-blue-50/50 border border-blue-100">
                  <div class="text-xs text-blue-400 mb-1">登记中心</div>
                  <div class="font-bold text-blue-700">{{ selectedTransfer.registrationCenter || '—' }}</div>
                </div>
                <div class="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">过户日期</div>
                  <div class="font-bold">{{ selectedTransfer.transferDate ? formatDate(selectedTransfer.transferDate) : '—' }}</div>
                </div>
                <div class="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">经办人</div>
                  <div class="font-bold">{{ selectedTransfer.operatorName || '—' }}</div>
                </div>
              </div>
              <div v-if="selectedTransfer.remark" class="mt-4 p-4 rounded-2xl bg-yellow-50/50 border border-yellow-100">
                <div class="text-xs text-yellow-500 mb-1">备注</div>
                <div class="text-sm">{{ selectedTransfer.remark }}</div>
              </div>
            </section>

            <!-- 过户文件 -->
            <section>
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-sm font-bold text-gray-800 flex items-center gap-2">
                  <el-icon class="text-[#1a73e8]"><Files /></el-icon> 过户文件
                </h3>
                <el-button v-if="selectedTransfer.transferId" v-hasPermi="['trade:transfer:upload']" type="primary"
                  size="small" class="!rounded-lg" @click="handleUploadDoc(selectedTransfer)">上传文件</el-button>
              </div>
              <div v-if="documents.length === 0" class="text-center py-8 text-gray-300">
                <el-icon :size="40"><Folder /></el-icon>
                <p class="mt-2 text-sm">暂无过户文件</p>
              </div>
              <div v-else class="space-y-3">
                <div v-for="doc in documents" :key="doc.id"
                  class="flex items-center justify-between p-4 bg-gray-50 rounded-xl border border-gray-100 hover:border-blue-100 transition-colors">
                  <div class="flex items-center gap-3 min-w-0">
                    <el-tag size="small" class="!rounded-md !border-none !min-w-[56px] text-center flex-shrink-0"
                      :type="docTypeTag(doc.docType)">
                      {{ docTypeLabel(doc.docType) }}
                    </el-tag>
                    <div class="min-w-0">
                      <div class="font-medium text-sm truncate max-w-[200px]">{{ doc.docName || doc.docType }}</div>
                      <div class="text-xs text-gray-400">{{ doc.uploadTime }}</div>
                    </div>
                  </div>
                  <el-button link type="primary" size="small" @click="handleDownloadDoc(doc)">
                    <el-icon><Download /></el-icon>
                  </el-button>
                </div>
              </div>
            </section>
          </div>
        </el-scrollbar>

        <!-- 底部操作 -->
        <div class="p-6 border-t border-gray-50 flex gap-3">
          <el-button v-if="selectedTransfer.transferStatus === 0" v-hasPermi="['trade:transfer:complete']"
            type="success" class="flex-1 !rounded-xl !h-12" icon="Select"
            @click="handleConfirmComplete(selectedTransfer)">确认过户完成</el-button>
          <el-button @click="detailDrawerVisible = false" class="!rounded-xl !h-12">关闭</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 上传文件对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传过户文件" width="480px" class="google-dialog" destroy-on-close>
      <div class="px-2">
        <el-form :model="uploadForm" :rules="uploadRules" ref="uploadFormRef" label-position="top">
          <el-form-item label="文件类型" prop="docType">
            <el-select v-model="uploadForm.docType" placeholder="选择文件类型" class="google-input-flat !w-full">
              <el-option label="完税证明" value="tax_receipt" />
              <el-option label="新产权证" value="new_deed" />
              <el-option label="受理凭证" value="app_form" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="文件上传" prop="fileUrl">
            <el-upload class="google-upload-card w-full" :action="uploadUrl" :headers="uploadHeaders"
              :data="{ category: 'TRANSFER_DOC' }" :on-success="handleDocUploadSuccess" :show-file-list="false"
              :limit="1">
              <div v-if="uploadForm.fileUrl"
                class="relative group w-full h-36 flex items-center justify-center bg-gray-50 rounded-xl border-2 border-dashed border-green-200">
                <div class="text-center">
                  <el-icon class="text-green-500 text-4xl"><Document /></el-icon>
                  <p class="text-sm text-green-600 mt-2">文件已上传</p>
                  <p class="text-xs text-gray-400 mt-1">点击重新选择</p>
                </div>
              </div>
              <div v-else
                class="w-full h-36 flex flex-col items-center justify-center border-2 border-dashed border-gray-200 rounded-xl hover:border-blue-400 transition-colors bg-gray-50 cursor-pointer">
                <el-icon class="text-gray-400 text-3xl mb-2"><Plus /></el-icon>
                <span class="text-xs text-gray-400">点击上传文件（图片或PDF）</span>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item label="文件名称">
            <el-input v-model="uploadForm.docName" placeholder="文件名称（选填）" class="google-input-flat !w-full" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex gap-3 justify-end px-4 pb-4">
          <el-button @click="uploadDialogVisible = false" class="!rounded-xl px-6">取消</el-button>
          <el-button v-hasPermi="['trade:transfer:upload']" type="primary"
            class="!rounded-xl px-8 !bg-[#1a73e8] border-none" @click="handleSubmitDoc">上传</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 选择交易订单（用于创建过户） -->
    <el-dialog v-model="selectTransactionDialogVisible" title="选择待过户交易" width="800px" class="google-dialog"
      destroy-on-close>
      <div class="px-2">
        <el-table :data="transactionOptions" highlight-current-row @current-change="onSelectTransaction"
          height="400" class="google-table-flat">
          <el-table-column label="交易编号" prop="transactionNo" width="160" />
          <el-table-column label="客户" width="120">
            <template #default="{ row }">
              {{ row.customer?.realName }}
            </template>
          </el-table-column>
          <el-table-column label="房源" min-width="200">
            <template #default="{ row }">
              <div class="truncate">{{ row.house?.projectName }} - {{ row.house?.houseNo }}</div>
            </template>
          </el-table-column>
          <el-table-column label="成交价" width="120" align="right">
            <template #default="{ row }">
              ¥{{ (row.dealPrice || 0).toLocaleString() }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 2 ? 'success' : 'warning'" size="small" class="!rounded-full">
                {{ row.status === 2 ? '已付首付' : '已过户' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <div class="flex gap-3 justify-end px-4 pb-4">
          <el-button @click="selectTransactionDialogVisible = false" class="!rounded-xl px-6">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Refresh, Plus, Close, InfoFilled, Finished, Files, Folder, Download, Document, Picture, Select } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTransfer, createTransfer, updateTransfer, completeTransfer, addTransferDocument, listTransferDocuments } from '@/api/trade/transfer'
import { listOrder } from '@/api/trade/order'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const uploadUrl = import.meta.env.VITE_APP_BASE_API + '/v1/common/upload'
const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + localStorage.getItem('resms_token')
}))

// --- 类型定义 ---
interface TransferItem {
  id: number
  transactionId: number
  transactionNo: string
  transferId: number | null
  transferNo: string | null
  certificateNo: string | null
  registrationCenter: string | null
  transferDate: string | null
  transferStatus: number // 0=待过户 1=已完成
  operatorId: number | null
  operatorName: string | null
  remark: string | null
  projectName: string
  houseNo: string
  houseCover: string
  area: number
  customerName: string
  customerPhone: string
  houseAddress: string
  docCount: number
}

// --- 状态管理 ---
const loading = ref(false)
const total = ref(0)
const transferList = ref<TransferItem[]>([])
const transactionOptions = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  transactionNo: '',
  houseAddress: '',
  transferStatus: undefined as number | undefined,
  status: undefined as number | undefined
})

// --- 对话框状态 ---
const transferDialogVisible = ref(false)
const isEditMode = ref(false)
const transferFormRef = ref()
const transferForm = reactive({
  id: undefined as number | undefined,
  transactionId: undefined as number | undefined,
  certificateNo: '',
  registrationCenter: '',
  transferDate: '',
  remark: ''
})

const transferRules = {
  transactionId: [{ required: true, message: '请选择交易订单', trigger: 'change' }]
}

const transferRulesEdit = {
  certificateNo: [{ required: true, message: '请输入不动产权证书号', trigger: 'blur' }],
  registrationCenter: [{ required: true, message: '请输入登记中心', trigger: 'blur' }],
  transferDate: [{ required: true, message: '请选择过户日期', trigger: 'change' }]
}

// --- 详情抽屉 ---
const detailDrawerVisible = ref(false)
const selectedTransfer = ref<TransferItem | null>(null)
const documents = ref<any[]>([])

// --- 上传文件 ---
const uploadDialogVisible = ref(false)
const uploadFormRef = ref()
const uploadForm = reactive({
  transferId: undefined as number | undefined,
  docType: 'tax_receipt',
  docName: '',
  fileUrl: ''
})
const uploadRules = {
  docType: [{ required: true, message: '请选择文件类型', trigger: 'change' }],
  fileUrl: [{ required: true, message: '请上传文件', trigger: 'change' }]
}

// --- 选择交易 ---
const selectTransactionDialogVisible = ref(false)

// --- 工具 ---
const docTypeMap: Record<string, { label: string; tag: string }> = {
  tax_receipt: { label: '完税证明', tag: 'success' },
  new_deed: { label: '新产权证', tag: 'primary' },
  app_form: { label: '受理凭证', tag: 'warning' },
  other: { label: '其他', tag: 'info' }
}

const docTypeLabel = (type: string) => docTypeMap[type]?.label || type
const docTypeTag = (type: string) => docTypeMap[type]?.tag || 'info'

const formatDate = (dt: string) => dt?.split('T')[0] || dt?.split(' ')[0] || dt

// --- 获取列表 ---
const getList = async () => {
  loading.value = true
  try {
    const response: any = await listOrder(queryParams)
    const records = (response.data.records || []).filter((r: any) => r.paymentType !== 4)
    total.value = response.data.total

    // 为每个交易订单查询过户信息
    const items: TransferItem[] = []
    for (const order of records) {
      let transferInfo: any = null
      let docCount = 0
      try {
        transferInfo = await getTransfer(order.id)
        if (transferInfo?.data?.id) {
          const docs: any = await listTransferDocuments(transferInfo.data.id)
          docCount = docs.data?.length || 0
        }
      } catch {
        // 无过户记录
      }

      items.push({
        id: order.id,
        transactionId: order.id,
        transactionNo: order.transactionNo,
        transferId: transferInfo?.data?.id || null,
        transferNo: transferInfo?.data?.transferNo || null,
        certificateNo: transferInfo?.data?.certificateNo || null,
        registrationCenter: transferInfo?.data?.registrationCenter || null,
        transferDate: transferInfo?.data?.transferDate || null,
        transferStatus: transferInfo?.data?.status ?? 0,
        operatorId: transferInfo?.data?.operatorId || null,
        operatorName: transferInfo?.data?.operatorName || null,
        remark: transferInfo?.data?.remark || null,
        projectName: order.house?.projectName || '',
        houseNo: order.house?.houseNo || '',
        houseCover: order.house?.coverImage || '',
        area: order.house?.area || 0,
        customerName: order.customer?.realName || order.customerName || '',
        customerPhone: order.customer?.phone || order.customerPhone || '',
        houseAddress: order.houseAddress || `${order.house?.projectName || ''} ${order.house?.houseNo || ''}`,
        docCount
      })
    }
    transferList.value = items
  } catch (err) {
    console.error('获取过户列表失败:', err)
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
  queryParams.houseAddress = ''
  queryParams.transferStatus = undefined
  queryParams.status = undefined
  handleQuery()
}

// --- 创建过户 ---
const handleCreateTransfer = async () => {
  isEditMode.value = false
  // 清空表单
  transferForm.id = undefined
  transferForm.transactionId = undefined
  transferForm.certificateNo = ''
  transferForm.registrationCenter = ''
  transferForm.transferDate = ''
  transferForm.remark = ''

  // 加载可过户的交易（已付首付/已过户状态）
  try {
    const response: any = await listOrder({ pageSize: 200, status: undefined })
    const allOrders = response.data.records || []
    // 只展示买卖交易（非租房）中已付首付(2)和已过户(3)的交易，且没有过户记录的
    const transferable: any[] = []
    for (const order of allOrders) {
      if (order.paymentType === 4) continue
      if (order.status !== 2 && order.status !== 3) continue
      try {
        await getTransfer(order.id)
        // 如果有过户记录则跳过
      } catch {
        transferable.push(order)
      }
    }
    transactionOptions.value = transferable

    if (transferable.length === 0) {
      ElMessage.warning('没有可创建过户的交易订单（需要已付首付且未创建过户）')
      return
    }
    transferDialogVisible.value = true
  } catch (err) {
    ElMessage.error('加载交易数据失败')
  }
}

const onTransactionChange = (val: number) => {
  const order = transactionOptions.value.find((t: any) => t.id === val)
  if (order) {
    // 自动填入默认信息
  }
}

const handleSubmitTransfer = async () => {
  const valid = await transferFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (isEditMode.value && transferForm.id) {
      await updateTransfer(transferForm.id, {
        certificateNo: transferForm.certificateNo || undefined,
        registrationCenter: transferForm.registrationCenter || undefined,
        transferDate: transferForm.transferDate || undefined,
        remark: transferForm.remark || undefined
      })
      ElMessage.success('过户记录已更新')
    } else {
      await createTransfer({
        transactionId: transferForm.transactionId,
        certificateNo: transferForm.certificateNo || undefined,
        registrationCenter: transferForm.registrationCenter || undefined,
        transferDate: transferForm.transferDate || undefined,
        remark: transferForm.remark || undefined
      })
      ElMessage.success('过户记录创建成功')
    }
    transferDialogVisible.value = false
    getList()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

// --- 编辑过户 ---
const handleEditTransfer = async (row: TransferItem) => {
  isEditMode.value = true
  if (!row.transferId) {
    ElMessage.warning('该交易尚无过户记录')
    return
  }
  try {
    const res: any = await getTransfer(row.transactionId)
    const data = res.data
    transferForm.id = data.id
    transferForm.transactionId = row.transactionId
    transferForm.certificateNo = data.certificateNo || ''
    transferForm.registrationCenter = data.registrationCenter || ''
    transferForm.transferDate = data.transferDate || ''
    transferForm.remark = data.remark || ''
    transferDialogVisible.value = true
  } catch {
    ElMessage.error('获取过户信息失败')
  }
}

// --- 确认过户 ---
const handleConfirmComplete = (row: TransferItem) => {
  if (!row.transferId) {
    ElMessage.warning('请先创建过户记录')
    return
  }
  ElMessageBox.confirm(
    `确认完成过户「${row.transactionNo}」？此操作将交易推进至"已完成"状态。`,
    '确认过户',
    { confirmButtonText: '确认完成', cancelButtonText: '取消', type: 'success' }
  ).then(async () => {
    try {
      await completeTransfer(row.transferId!)
      ElMessage.success('过户已完成')
      detailDrawerVisible.value = false
      getList()
    } catch {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

// --- 文件操作 ---
const handleViewDocuments = async (row: TransferItem) => {
  selectedTransfer.value = row
  documents.value = []
  if (row.transferId) {
    try {
      const res: any = await listTransferDocuments(row.transferId)
      documents.value = res.data || []
    } catch {
      // 无文件
    }
  }
  detailDrawerVisible.value = true
}

const handleUploadDoc = (row: TransferItem) => {
  if (!row.transferId) {
    ElMessage.warning('请先创建过户记录')
    return
  }
  uploadForm.transferId = row.transferId!
  uploadForm.docType = 'tax_receipt'
  uploadForm.docName = ''
  uploadForm.fileUrl = ''
  uploadDialogVisible.value = true
}

const handleDocUploadSuccess = (res: any) => {
  if (res.code === 200) {
    uploadForm.fileUrl = res.data.url
  }
}

const handleSubmitDoc = async () => {
  const valid = await uploadFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await addTransferDocument({
      transferId: uploadForm.transferId,
      docType: uploadForm.docType,
      docName: uploadForm.docName || undefined,
      fileUrl: uploadForm.fileUrl
    })
    ElMessage.success('文件上传成功')
    uploadDialogVisible.value = false
    // 刷新文件列表
    if (selectedTransfer.value?.transferId) {
      const res: any = await listTransferDocuments(selectedTransfer.value.transferId)
      documents.value = res.data || []
    }
    getList()
  } catch {
    ElMessage.error('上传失败')
  }
}

const handleDownloadDoc = (doc: any) => {
  if (doc.fileUrl) {
    window.open(doc.fileUrl, '_blank')
  }
}

const handleDetail = (row: TransferItem) => {
  handleViewDocuments(row)
}

const onSelectTransaction = (row: any) => {
  if (row) {
    transferForm.transactionId = row.id
    selectTransactionDialogVisible.value = false
  }
}

// 初始加载
onMounted(() => {
  getList()
})
</script>

<style scoped>
.transfer-management-container {
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
  cursor: pointer;
}

:deep(.el-table__row td) {
  border-bottom: 1px solid #f1f3f4 !important;
}

/* 输入框扁平化规格 */
:deep(.google-input-flat .el-input__wrapper) {
  border-radius: 12px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.2s;
  padding: 8px 16px !important;
  font-size: 14px;
}

:deep(.google-input-flat .el-input__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px #1a73e8 !important;
}

:deep(.google-input-flat .el-input-number .el-input__wrapper) {
  padding-left: 15px !important;
  padding-right: 15px !important;
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

/* 抽屉样式 */
:deep(.transfer-detail-drawer .el-drawer__body) {
  padding: 0;
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

/* 上传卡片 */
:deep(.google-upload-card .el-upload) {
  width: 100%;
}
</style>
