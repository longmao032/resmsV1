<template>
  <div class="app-user-container p-6 bg-[#f8f9fa] min-h-full flex flex-col">
    <!-- 顶部状态概览 (Premium Card) -->
    <div class="grid grid-cols-4 gap-6 mb-8 shrink-0">
      <div v-for="(stat, index) in stats" :key="index" 
        class="stat-card p-6 rounded-3xl bg-white border border-gray-100 shadow-sm hover:shadow-md transition-all">
        <div class="flex items-center gap-4">
          <div :class="`w-12 h-12 rounded-2xl flex items-center justify-center bg-gradient-to-br ${stat.color} text-white shadow-lg` ">
            <el-icon :size="20"><component :is="stat.icon" /></el-icon>
          </div>
          <div>
            <div class="text-xs text-gray-400 font-medium uppercase tracking-wider">{{ stat.label }}</div>
            <div class="text-2xl font-bold text-[#202124]">{{ stat.value }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="flex-1 flex flex-col bg-white/70 backdrop-blur-xl rounded-[32px] shadow-sm border border-white overflow-hidden">
      <!-- 搜索区域 (Glassmorphism Search Bar) -->
      <div class="p-8 border-b border-gray-50/50 bg-white/40">
        <el-form :inline="true" :model="queryParams" class="flex flex-wrap gap-x-10 gap-y-4">
          <el-form-item label="手机号" class="!mb-0">
            <el-input v-model="queryParams.phone" placeholder="135...." clearable class="premium-input !w-52" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="用户昵称" class="!mb-0">
            <el-input v-model="queryParams.nickname" placeholder="搜索昵称..." clearable class="premium-input !w-52" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="状态" class="!mb-0">
            <el-select v-model="queryParams.status" placeholder="全部" clearable class="premium-input !w-32">
              <el-option label="正常" :value="1" />
              <el-option label="封禁" :value="0" />
            </el-select>
          </el-form-item>
          <div class="flex gap-3 ml-auto">
            <el-button v-hasPermi="['trade:appuser:query']" type="primary" icon="Search" class="search-btn" @click="handleQuery">搜索</el-button>
            <el-button v-hasPermi="['trade:appuser:query']" icon="Refresh" class="reset-btn" @click="resetQuery">重置</el-button>
          </div>
        </el-form>
      </div>

      <!-- 操作栏 -->
      <div class="px-8 py-4 flex items-center justify-between bg-white/20">
        <div class="flex gap-3">
          <el-button v-hasPermi="['trade:appuser:export']" type="primary" plain icon="Download" class="!rounded-xl px-6 !border-[#1a73e8] !text-[#1a73e8] hover:!bg-blue-50" @click="handleExport">导出 Excel</el-button>
          <el-button v-hasPermi="['trade:appuser:delete']" type="danger" plain icon="Delete" class="!rounded-xl px-6" :disabled="!ids.length" @click="handleDelete()">批量删除</el-button>
        </div>
        <div class="text-sm text-gray-400 font-medium">
          已选择 <span class="text-[#1a73e8] font-bold">{{ ids.length }}</span> 位用户
        </div>
      </div>

      <!-- 表格区域 -->
      <div class="flex-1 px-8 overflow-hidden">
        <el-table
          v-loading="loading"
          :data="userList"
          row-key="id"
          height="100%"
          class="premium-table"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="用户ID" prop="id" width="90" align="center">
            <template #default="{ row }">
              <span class="font-mono text-gray-400">#{{ String(row.id).padStart(3, '0') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用户档案" min-width="220">
            <template #default="{ row }">
              <div class="flex items-center gap-4 py-2">
                <el-avatar :size="48" :src="row.avatarUrl" class="shadow-sm border-2 border-white ring-1 ring-gray-100" />
                <div class="flex flex-col">
                  <span class="font-bold text-[#202124] text-base">{{ row.nickname || '未命名用户' }}</span>
                  <span class="text-xs text-gray-400 font-mono mt-1">{{ row.phone }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <!-- 微信标识列 (OpenID & UnionID) -->
          <el-table-column label="微信标识" min-width="200">
            <template #default="{ row }">
              <div class="flex flex-col gap-1">
                <div class="flex items-center gap-2 text-[11px]">
                  <el-tag size="small" type="success" effect="plain" class="!rounded-md !px-1.5 shrink-0">OpenID</el-tag>
                  <span class="font-mono text-gray-400 truncate">{{ row.wechatOpenid || '未绑定' }}</span>
                </div>
                <div class="flex items-center gap-2 text-[11px]">
                  <el-tag size="small" type="info" effect="plain" class="!rounded-md !px-1.5 shrink-0">UnionID</el-tag>
                  <span class="font-mono text-gray-400 truncate">{{ row.unionId || '未绑定' }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-switch
                v-hasPermi="['trade:appuser:status']"
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                inline-prompt
                active-text="正常"
                inactive-text="封禁"
                class="premium-switch"
                @change="handleStatusChange(row)"
              />
            </template>
          </el-table-column>

          <!-- 时间轨迹 (注册 & 更新) -->
          <el-table-column label="时间轨迹" width="200" align="center">
            <template #default="{ row }">
              <div class="flex flex-col items-center text-[11px] text-gray-400">
                <div class="flex items-center gap-1 mb-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-blue-400"></span>
                  <span>注册: {{ row.createTime }}</span>
                </div>
                <div class="flex items-center gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-orange-400"></span>
                  <span>更新: {{ row.updateTime }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="管理" width="120" fixed="right" align="center">
            <template #default="{ row }">
              <div class="flex items-center justify-center gap-1">
                <el-tooltip content="更多详情" placement="top">
                  <el-button v-hasPermi="['trade:appuser:query']" link icon="View" class="action-icon hover:!bg-blue-50 hover:!text-blue-600" @click="handleDetail(row)" />
                </el-tooltip>
                <el-tooltip content="删除用户" placement="top">
                  <el-button v-hasPermi="['trade:appuser:delete']" link icon="Delete" class="action-icon hover:!bg-red-50 hover:!text-red-600" @click="handleDelete(row)" />
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页组件 -->
      <div class="px-8 py-6 border-t border-gray-50 flex justify-between items-center bg-white/40">
        <div class="text-xs text-gray-400">当前显示 {{ (queryParams.pageNum - 1) * queryParams.pageSize + 1 }} - {{ Math.min(queryParams.pageNum * queryParams.pageSize, total) }} 条，共 {{ stats[0].value }} 条记录</div>
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          class="premium-pagination"
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </div>
    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="520px" class="premium-dialog" destroy-on-close>
      <div v-if="currentDetail" class="space-y-6">
        <!-- 基础信息 -->
        <div class="flex items-center gap-5 p-5 bg-[#f8f9fa] rounded-2xl">
          <el-avatar :size="64" :src="currentDetail.avatarUrl" class="shadow-md border-2 border-white" />
          <div>
            <div class="text-xl font-bold text-[#202124]">{{ currentDetail.nickname || '未命名用户' }}</div>
            <div class="text-sm font-mono text-gray-400 mt-1">{{ currentDetail.phone }}</div>
          </div>
        </div>

        <!-- 详细信息 -->
        <div class="grid grid-cols-2 gap-y-4 text-sm bg-[#f8f9fa]/50 p-5 rounded-2xl">
          <div class="text-gray-500">用户ID</div>
          <div class="text-right font-mono">#{{ String(currentDetail.id).padStart(3, '0') }}</div>

          <div class="text-gray-500">昵称</div>
          <div class="text-right font-medium">{{ currentDetail.nickname || '-' }}</div>

          <div class="text-gray-500">手机号</div>
          <div class="text-right font-mono">{{ currentDetail.phone }}</div>

          <div class="text-gray-500">微信 OpenID</div>
          <div class="text-right font-mono text-xs truncate">{{ currentDetail.wechatOpenid || '未绑定' }}</div>

          <div class="text-gray-500">微信 UnionID</div>
          <div class="text-right font-mono text-xs truncate">{{ currentDetail.unionId || '未绑定' }}</div>

          <div class="text-gray-500">当前状态</div>
          <div class="text-right">
            <el-tag :type="currentDetail.status === 1 ? 'success' : 'danger'" effect="light" class="!rounded-full">
              {{ currentDetail.status === 1 ? '正常' : '封禁' }}
            </el-tag>
          </div>

          <div class="text-gray-500">注册时间</div>
          <div class="text-right text-gray-600">{{ currentDetail.createTime }}</div>

          <div class="text-gray-500">更新时间</div>
          <div class="text-right text-gray-600">{{ currentDetail.updateTime }}</div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="detailVisible = false" class="!rounded-xl">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Search, Refresh, Delete, User, Timer, CircleCheck, WarnTriangleFilled, View, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAppUser, changeAppUserStatus, delAppUser, exportAppUser, getAppUserStats } from '@/api/trade/appuser'

// --- 类型定义 ---
interface AppUser {
  id: number
  phone: string
  nickname: string
  avatarUrl: string
  wechatOpenid?: string
  unionId?: string
  status: number
  createTime: string
  updateTime: string
}

// --- 状态 ---
const loading = ref(false)
const userList = ref<AppUser[]>([])
const total = ref(0)
const ids = ref<number[]>([])
const queryParams = reactive({ pageNum: 1, pageSize: 10, phone: '', nickname: '', status: undefined })

// --- 详情弹窗 ---
const detailVisible = ref(false)
const currentDetail = ref<AppUser | null>(null)

// 初始状态统计
const stats = reactive([
  { label: '用户总数', value: '0', icon: 'User', color: 'from-blue-500 to-blue-600' },
  { label: '本周新增', value: '0', icon: 'Timer', color: 'from-purple-500 to-purple-600' },
  { label: '正常状态', value: '0', icon: 'CircleCheck', color: 'from-green-500 to-green-600' },
  { label: '封禁异常', value: '0', icon: 'WarnTriangleFilled', color: 'from-red-500 to-red-600' }
])

const fetchStats = async () => {
  try {
    const res: any = await getAppUserStats()
    stats[0].value = res.data.totalCount.toLocaleString()
    stats[1].value = res.data.weeklyNewCount.toLocaleString()
    stats[2].value = res.data.normalCount.toLocaleString()
    stats[3].value = res.data.bannedCount.toLocaleString()
  } catch {
    // 统计接口失败不影响列表展示
  }
}

const getList = async () => {
  loading.value = true
  try {
    const response: any = await listAppUser(queryParams)
    userList.value = response.data.records
    total.value = response.data.total
    stats[0].value = total.value.toLocaleString()
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

// 初始加载
getList()
fetchStats()

const resetQuery = () => {
  queryParams.phone = ''
  queryParams.nickname = ''
  queryParams.status = undefined
  handleQuery()
}

const handleSelectionChange = (selection: any[]) => {
  ids.value = selection.map(item => item.id)
}

const handleStatusChange = async (row: AppUser) => {
  const text = row.status === 1 ? '启用' : '封禁'
  try {
    await changeAppUserStatus(row.id, row.status)
    ElMessage.success(`用户 ${row.nickname} 已成功${text}`)
    fetchStats()
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1 // 失败回滚
  }
}

const handleDelete = (row?: AppUser) => {
  const userIds = row ? [row.id] : ids.value
  ElMessageBox.confirm(`确认删除选中的 ${userIds.length} 位用户吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      loading.value = true
      for (const id of userIds) {
        await delAppUser(id)
      }
      ElMessage.success('删除成功')
      getList()
      fetchStats()
    } finally {
      loading.value = false
    }
  })
}

const handleDetail = (row: AppUser) => {
  currentDetail.value = row
  detailVisible.value = true
}

const handleExport = async () => {
  const params: any = {}
  if (queryParams.phone) params.phone = queryParams.phone
  if (queryParams.nickname) params.nickname = queryParams.nickname
  if (queryParams.status !== undefined) params.status = queryParams.status
  try {
    const res = await exportAppUser(params)
    const blob = new Blob([res])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = decodeURIComponent(
      (res as any).headers?.['content-disposition']?.split('filename*=utf-8\'\'')?.[1] || 'C端用户.xlsx'
    )
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('导出成功')
  } catch {
    // 错误由拦截器处理
  }
}
</script>

<style scoped>
.app-user-container { height: calc(100vh - 84px); }

/* Premium Stat Cards */
.stat-card {
  transform: translateZ(0);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.stat-card:hover { transform: translateY(-8px); }

/* Premium Table Styles */
.premium-table {
  --el-table-bg-color: transparent;
  --el-table-header-bg-color: #fcfcfd;
  --el-table-row-hover-bg-color: rgba(26, 115, 232, 0.04);
}
:deep(.el-table__header th) {
  padding: 20px 0;
  color: #5f6368;
  font-weight: 700;
  font-size: 12px;
}
:deep(.el-table__row) { height: 80px; transition: background 0.2s; }
:deep(.el-table__row td) { border-bottom: 1px solid #f1f3f4 !important; }

/* 按钮及输入框 */
.premium-input :deep(.el-input__wrapper) {
  border-radius: 14px;
  background-color: #fff;
  border: 1px solid #e8eaed;
}
.search-btn {
  background: #1a73e8 !important;
  border-radius: 14px !important;
  padding: 20px 24px !important;
}
.action-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
}

/* 分页器 */
:deep(.premium-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #1a73e8 !important;
}

/* 对话框 */
:deep(.premium-dialog) {
  border-radius: 24px;
  overflow: hidden;
}
:deep(.premium-dialog .el-dialog__header) {
  padding: 24px 28px 16px;
  margin-right: 0;
  border-bottom: 1px solid #f1f3f4;
}
:deep(.premium-dialog .el-dialog__title) {
  font-size: 18px;
  font-weight: bold;
  color: #202124;
}
:deep(.premium-dialog .el-dialog__body) {
  padding: 24px 28px;
}
</style>
