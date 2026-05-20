<template>
  <div class="notice-container p-6 bg-[#f8f9fa] min-h-full">
    <!-- 搜索区 -->
    <el-card shadow="never" class="!border-none rounded-[24px] mb-6">
      <div class="flex items-center justify-between">
        <el-form :inline="true" :model="queryParams" class="flex items-center gap-4 -mb-4">
          <el-form-item label="公告标题">
            <el-input v-model="queryParams.title" placeholder="请输入标题" clearable class="google-input" />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="queryParams.noticeType" placeholder="全部" clearable class="google-input !w-32">
              <el-option label="系统通知" :value="1" />
              <el-option label="任务提醒" :value="2" />
              <el-option label="交易提醒" :value="3" />
              <el-option label="审批通知" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" class="!rounded-lg !bg-[#1a73e8]" @click="handleQuery" v-hasPermi="['message:notice:query']">查询</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" icon="Plus" class="!rounded-lg !bg-[#1a73e8]" @click="handleAdd" v-hasPermi="['message:notice:add']">发布公告</el-button>
      </div>
    </el-card>

    <!-- 列表区 -->
    <el-card shadow="never" class="!border-none rounded-[24px]">
      <el-table :data="noticeList" v-loading="loading" class="google-table">
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-medium text-gray-900 cursor-pointer hover:text-[#1a73e8]" @click="handleView(row)">
              {{ row.title }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getNoticeTypeTag(row.noticeType)" effect="light" class="!rounded-md border-none">
              {{ getNoticeTypeText(row.noticeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="接收范围" width="120" align="center">
          <template #default="{ row }">
            <span class="text-sm text-gray-600">{{ getReceiverTypeText(row.receiverType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布人" prop="senderName" width="120" />
        <el-table-column label="发布时间" width="180" align="center">
          <template #default="{ row }">
            {{ row.sendTime || row.createTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.withdrawStatus === 1" type="info" size="small">已撤回</el-tag>
            <el-tag v-else :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发送' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="阅读情况" width="150" align="center">
          <template #default="{ row }">
            <div v-if="row.status === 1 && row.withdrawStatus !== 1" class="flex flex-col items-center">
              <el-progress
                :percentage="row.totalReceiverCount > 0 ? Math.round((row.readCount / row.totalReceiverCount) * 100) : 0"
                :stroke-width="4"
                :show-text="false"
                class="w-16 mb-1"
                :status="row.readCount === row.totalReceiverCount ? 'success' : ''"
              />
              <span class="text-[10px] text-gray-400">{{ row.readCount }}/{{ row.totalReceiverCount }}</span>
            </div>
            <span v-else class="text-gray-300">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-1">
              <el-tooltip v-if="checkPermi(['message:notice:query'])" content="预览" placement="top" effect="dark">
                <el-button
                  link
                  icon="View"
                  class="!p-2 hover:bg-gray-100 rounded-full transition-colors text-[#1a73e8]"
                  @click="handleView(row)"
                />
              </el-tooltip>
              <el-tooltip
                v-if="row.status === 1 && row.withdrawStatus !== 1 && checkPermi(['message:notice:edit'])"
                content="编辑"
                placement="top"
                effect="dark"
              >
                <el-button
                  link
                  icon="Edit"
                  class="!p-2 hover:bg-blue-50 rounded-full transition-colors text-blue-500"
                  @click="handleEdit(row)"
                />
              </el-tooltip>
              <el-tooltip
                v-if="row.status === 1 && row.withdrawStatus !== 1 && checkPermi(['message:notice:withdraw'])"
                content="撤回"
                placement="top"
                effect="dark"
              >
                <el-button
                  link
                  icon="RefreshLeft"
                  class="!p-2 hover:bg-orange-50 rounded-full transition-colors text-orange-500"
                  @click="handleWithdraw(row)"
                />
              </el-tooltip>
              <el-tooltip v-if="checkPermi(['message:notice:delete'])" content="删除" placement="top" effect="dark">
                <el-button
                  link
                  icon="Delete"
                  class="!p-2 hover:bg-red-50 rounded-full transition-colors text-red-500"
                  @click="handleDelete(row)"
                />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <!-- 空状态 -->
        <template #empty>
          <div class="flex flex-col items-center py-10">
            <el-empty :description="loading ? '加载中...' : '暂无通知公告'" />
          </div>
        </template>
      </el-table>

      <div class="flex justify-end mt-6">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </el-card>

    <!-- 发布对话框 -->
    <el-dialog
      v-model="dialog.visible"
      title="发布公告"
      width="620px"
      append-to-body
      class="google-dialog"
    >
      <el-form ref="noticeFormRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" class="google-input" />
        </el-form-item>
        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="公告类型" prop="noticeType">
            <el-select v-model="form.noticeType" placeholder="请选择类型" class="google-input w-full">
              <el-option label="系统通知" :value="1" />
              <el-option label="任务提醒" :value="2" />
              <el-option label="交易提醒" :value="3" />
              <el-option label="审批通知" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="form.priority" placeholder="请选择优先级" class="google-input w-full">
              <el-option label="紧急" :value="1" />
              <el-option label="重要" :value="2" />
              <el-option label="普通" :value="3" />
            </el-select>
          </el-form-item>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="内容格式" prop="contentType">
            <el-select v-model="form.contentType" class="google-input w-full">
              <el-option label="纯文本" :value="1" />
              <el-option label="HTML" :value="2" />
              <el-option label="Markdown" :value="3" />
            </el-select>
          </el-form-item>
        </div>

        <el-divider border-style="dashed" class="!my-4">接收设置</el-divider>

        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="接收类型" prop="receiverType">
            <el-select v-model="form.receiverType" class="google-input w-full" @change="form.receiverIds = []">
              <el-option v-if="isAdmin" label="全体人员" :value="4" />
              <el-option label="指定部门" :value="2" />
              <el-option label="指定角色" :value="3" />
              <el-option label="指定用户" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.receiverType !== 4" label="接收目标" prop="receiverIds">
            <!-- 非管理员选部门：显示为只读标签 -->
            <el-tag v-if="form.receiverType === 2 && !isAdmin" type="info" class="!rounded-lg">
              {{ deptOptions.find(d => d.id === currentDeptId)?.deptName || '所在部门' }}
            </el-tag>
            <!-- 管理员选部门：显示部门树选择器 -->
            <el-tree-select
              v-if="form.receiverType === 2 && isAdmin"
              v-model="form.receiverIds"
              :data="deptOptions"
              multiple
              collapse-tags
              collapse-tags-indicator
              placeholder="请选择部门"
              class="google-input w-full"
              :props="{ label: 'deptName', value: 'id' }"
            />
            <el-select
              v-if="form.receiverType === 3"
              v-model="form.receiverIds"
              multiple
              collapse-tags
              placeholder="请选择角色"
              class="google-input w-full"
            >
              <el-option v-for="item in roleOptions" :key="item.id" :label="item.roleName" :value="item.id" />
            </el-select>
            <el-select
              v-if="form.receiverType === 1"
              v-model="form.receiverIds"
              multiple
              filterable
              collapse-tags
              :placeholder="isAdmin ? '请选择用户' : '请选择本部门用户'"
              class="google-input w-full"
            >
              <el-option v-for="item in userOptions" :key="item.id" :label="item.realName" :value="item.id" />
            </el-select>
          </el-form-item>
        </div>
        <div class="grid grid-cols-2 gap-4 mt-4">
          <el-form-item label="过期时间" prop="expireTime">
            <el-date-picker
              v-model="form.expireTime"
              type="datetime"
              placeholder="请选择过期时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              class="google-input w-full"
            />
          </el-form-item>
          <el-form-item label="跳转路径" prop="routerPath">
            <el-input v-model="form.routerPath" placeholder="如 /system/user" class="google-input" />
          </el-form-item>
        </div>

        <el-form-item label="内容" prop="content" class="mt-2">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
            class="google-input !rounded-xl"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="dialog.visible = false" class="!rounded-lg">取消</el-button>
          <el-button type="primary" :loading="submitLoading" class="!rounded-lg !bg-[#1a73e8]" @click="submitForm">
            发 布
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewDialog.visible"
      title="公告预览"
      width="700px"
      append-to-body
      class="google-dialog preview-dialog"
    >
      <div v-if="previewData" class="p-4">
        <div class="text-center mb-6">
          <h2 class="text-2xl font-bold text-gray-900 mb-2">{{ previewData.title }}</h2>
          <div class="flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-sm text-gray-500">
            <span>发送人：{{ previewData.senderName }}</span>
            <span>发布时间：{{ previewData.sendTime || previewData.createTime }}</span>
            <div class="flex items-center gap-2">
              <el-tag :type="getNoticeTypeTag(previewData.noticeType)" size="small" effect="light">
                {{ getNoticeTypeText(previewData.noticeType) }}
              </el-tag>
              <el-tag type="info" size="small" effect="plain">
                接收：{{ getReceiverTypeText(previewData.receiverType) }}
              </el-tag>
            </div>
          </div>
        </div>
        <el-divider />

        <!-- 加载状态 -->
        <div v-if="previewLoading" class="flex justify-center py-10">
          <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        </div>

        <!-- 内容为空 -->
        <div v-else-if="!previewData.content" class="text-center py-10 text-gray-400">
          暂无内容
        </div>

        <!-- HTML 内容 -->
        <div
          v-if="previewData.contentType === 2"
          class="notice-content text-gray-700 leading-relaxed min-h-[200px]"
          v-html="previewData.content"
        />
        <!-- 纯文本 / Markdown 内容 -->
        <div
          v-else
          class="notice-content text-gray-700 leading-relaxed min-h-[200px] whitespace-pre-wrap"
        >
          {{ previewData.content }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Plus, View, Delete, RefreshLeft, Loading } from '@element-plus/icons-vue'
import { listNotices, publishNotice, updateNotice, withdrawNotice, deleteNotices } from '@/api/system/notice'
import { listDeptTree } from '@/api/system/dept'
import { getUsersByDept, getSalesOptions } from '@/api/system/user'
import { useUserStore } from '@/store/user'
import { checkPermi } from '@/utils/permission'
import type { NotificationVO, NotificationSaveDTO } from '@/types/system/notice'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.userInfo?.dataScope === 1)
const currentDeptId = computed(() => userStore.userInfo?.deptId)

const loading = ref(false)
const submitLoading = ref(false)
const previewLoading = ref(false)
const total = ref(0)
const noticeList = ref<NotificationVO[]>([])
const noticeFormRef = ref<FormInstance>()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  noticeType: undefined as number | undefined
})

const dialog = reactive({
  visible: false
})

const editingId = ref<number | null>(null)

const previewDialog = reactive({
  visible: false
})

const previewData = ref<NotificationVO | null>(null)

const deptOptions = ref<any[]>([])
const roleOptions = ref([
  { id: 1, roleName: '超级管理员' },
  { id: 2, roleName: '部门经理' },
  { id: 3, roleName: '普通员工' },
  { id: 4, roleName: '财务人员' }
])
const userOptions = ref<{ id: number; realName: string }[]>([])

const fetchDeptTree = async () => {
  try {
    const res = await listDeptTree()
    deptOptions.value = res.data || []
  } catch { /* fallback to empty */ }
}

const fetchUsersByDept = async (deptId: number) => {
  try {
    const res = await getUsersByDept(deptId)
    userOptions.value = res.data || []
  } catch { /* fallback to empty */ }
}

const form = ref<NotificationSaveDTO>({
  title: '',
  noticeType: 1,
  content: '',
  contentType: 1,
  priority: 3,
  receiverType: isAdmin.value ? 4 : 2,
  receiverIds: [],
  expireTime: '',
  routerPath: ''
})

watch(() => form.value.receiverType, (type) => {
  form.value.receiverIds = []
  if (type === 2 && !isAdmin.value && currentDeptId.value) {
    form.value.receiverIds = [currentDeptId.value]
  }
  if (type === 1) {
    if (isAdmin.value) {
      getSalesOptions().then(res => { userOptions.value = res.data || [] }).catch(() => {})
    } else if (currentDeptId.value) {
      fetchUsersByDept(currentDeptId.value)
    }
  }
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
  receiverType: [{ required: true, message: '请选择接收类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const getNoticeTypeTag = (type: number) => {
  const map: Record<number, string> = { 1: 'danger', 2: 'warning', 3: 'success', 4: 'info' }
  return map[type] || 'info'
}

const getNoticeTypeText = (type: number) => {
  const map: Record<number, string> = { 1: '系统通知', 2: '任务提醒', 3: '交易提醒', 4: '审批通知' }
  return map[type] || '未知'
}

const getReceiverTypeText = (type: number) => {
  const map: Record<number, string> = { 1: '指定用户', 2: '指定部门', 3: '指定角色', 4: '全体人员' }
  return map[type] || '全体人员'
}

const getList = async () => {
  loading.value = true
  try {
    const res = await listNotices(
      queryParams.pageNum,
      queryParams.pageSize,
      queryParams.title || undefined,
      queryParams.noticeType
    )
    noticeList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    noticeList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const reset = () => {
  form.value = {
    title: '',
    noticeType: 1,
    content: '',
    contentType: 1,
    priority: 3,
    receiverType: isAdmin.value ? 4 : 2,
    receiverIds: [],
    expireTime: '',
    routerPath: ''
  }
  if (!isAdmin.value && currentDeptId.value) {
    form.value.receiverIds = [currentDeptId.value]
  }
  noticeFormRef.value?.resetFields()
}

const handleAdd = () => {
  editingId.value = null
  reset()
  dialog.visible = true
}

const handleEdit = (row: NotificationVO) => {
  editingId.value = row.id
  form.value = {
    title: row.title || '',
    noticeType: row.noticeType ?? 1,
    content: row.content || '',
    contentType: row.contentType ?? 1,
    priority: row.priority ?? 3,
    receiverType: row.receiverType ?? 4,
    receiverIds: row.receiverIds || [],
    expireTime: row.expireTime || '',
    routerPath: row.routerPath || ''
  }
  dialog.visible = true
}

const handleView = (row: NotificationVO) => {
  previewData.value = row
  previewDialog.visible = true
}

const submitForm = async () => {
  if (!noticeFormRef.value) return
  await noticeFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const payload = { ...form.value }
      // receiverType=4 (全体) 时不需要发送 receiverIds
      if (payload.receiverType === 4) {
        delete payload.receiverIds
      }
      // 过期时间为空时去除
      if (!payload.expireTime) {
        delete payload.expireTime
      }
      // 跳转路径为空时去除
      if (!payload.routerPath) {
        delete payload.routerPath
      }

      if (editingId.value) {
        await updateNotice(editingId.value, payload)
        ElMessage.success('编辑成功')
      } else {
        await publishNotice(payload)
        ElMessage.success('发布成功')
      }
      dialog.visible = false
      getList()
    } catch {
      ElMessage.error(editingId.value ? '编辑失败' : '发布失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDelete = (row: NotificationVO) => {
  ElMessageBox.confirm(`是否确认删除标题为 "${row.title}" 的公告?`, '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    boxType: 'confirm'
  }).then(async () => {
    try {
      await deleteNotices([row.id])
      ElMessage.success('删除成功')
      getList()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleWithdraw = (row: NotificationVO) => {
  ElMessageBox.confirm(`是否确认撤回公告 "${row.title}" ? 撤回后用户将不再可见。`, '提示', {
    confirmButtonText: '确定撤回',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await withdrawNotice(row.id)
      ElMessage.success('撤回成功')
      getList()
    } catch {
      ElMessage.error('撤回失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  getList()
  fetchDeptTree()
})
</script>

<style scoped>
.google-input :deep(.el-input__wrapper),
.google-input :deep(.el-textarea__inner) {
  border-radius: 12px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.google-input :deep(.el-input__wrapper):hover,
.google-input :deep(.el-textarea__inner):hover {
  background-color: #e8eaed;
}

.google-input :deep(.el-input__wrapper).is-focus,
.google-input :deep(.el-textarea__inner):focus {
  background-color: #fff;
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px #1a73e8 !important;
}

.google-input :deep(.el-select__wrapper) {
  border-radius: 12px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  border: 1px solid transparent;
}

.google-table {
  --el-table-header-bg-color: transparent;
  --el-table-row-hover-bg-color: #f8f9fa;
}

.google-dialog :deep(.el-dialog) {
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.1);
}

.google-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding: 24px 24px 16px;
}

.google-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 500;
  color: #1f1f1f;
}

.google-dialog :deep(.el-dialog__body) {
  padding: 16px 24px;
}

.google-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
}

.notice-content {
  font-family: 'Roboto', 'Inter', sans-serif;
  font-size: 16px;
  color: #3c4043;
}
</style>
