<template>
  <div class="team-user-container p-6 bg-gray-50 min-h-full">
    <!-- 部门概览 -->
    <div class="mb-8 flex items-end justify-between">
      <div>
        <h2 class="text-3xl font-bold text-[#202124] tracking-tight">部门成员</h2>
        <p class="text-gray-500 mt-2 flex items-center gap-2">
          <el-tag size="small" type="success" class="!rounded-full">运行中</el-tag>
          共 {{ total }} 位成员
        </p>
      </div>
      <div class="flex gap-3">
        <el-button v-hasPermi="['team:user:add']" type="primary" icon="Plus" class="!rounded-full px-6 !bg-[#1a73e8] border-none shadow-md hover:shadow-lg transition-all" @click="handleAdd">新增成员</el-button>
        <el-button v-hasPermi="['team:user:export']" icon="Download" class="!rounded-full px-6">导出名册</el-button>
      </div>
    </div>

    <!-- 部门月度目标 -->
    <div v-if="queryParams.deptId" class="bg-white p-5 rounded-2xl shadow-sm border border-gray-100 mb-6">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-2">
          <span class="text-sm font-bold text-gray-700">{{ deptTarget ? currentMonth + ' 月度目标' : '未设置月度目标' }}</span>
          <el-button v-hasPermi="['team:performance:edit']" text circle size="small" @click="handleEditTarget">
            <el-icon><EditPen /></el-icon>
          </el-button>
        </div>
        <span v-if="deptTarget" class="text-sm text-gray-500 font-medium">目标 {{ deptTarget.targetAmount }}万</span>
      </div>
      <div v-if="deptTarget">
        <div class="flex justify-between text-xs text-gray-400 mb-1">
          <span>已完成</span>
          <span>{{ totalPerformance.toFixed(1) }}万 / {{ deptTarget.targetAmount }}万</span>
        </div>
        <el-progress
          :percentage="Math.min(Math.round((totalPerformance / deptTarget.targetAmount) * 100), 100)"
          :stroke-width="8"
          :show-text="false"
          :color="totalPerformance / deptTarget.targetAmount >= 0.8 ? '#34a853' : '#1a73e8'"
          class="google-progress"
        />
      </div>
      <p v-else class="text-xs text-gray-400">点击右侧编辑按钮为本月设置目标</p>
    </div>

    <!-- 搜索与筛选 -->
    <div class="bg-white p-4 rounded-2xl shadow-sm border border-gray-100 mb-6 flex flex-wrap items-center gap-4">
      <el-input v-model="searchText" placeholder="搜索成员姓名、手机号..." class="google-input-flat !w-80" clearable @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-tree-select
        v-if="canSwitchDept"
        v-model="queryParams.deptId"
        :data="deptTreeData"
        :props="{ label: 'deptName', value: 'id', children: 'children' }"
        value-key="id"
        placeholder="部门筛选"
        check-strictly
        clearable
        class="google-input-flat !w-48"
        @change="handleQuery"
      />
      <el-tag v-else type="info" size="large" class="!rounded-full !px-4 !py-1.5 !text-sm !font-medium">
        {{ userStore.userInfo?.deptName || '我的部门' }}
      </el-tag>
      <el-radio-group v-model="queryParams.status" class="google-radio-group" @change="handleQuery">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button :value="1">在职</el-radio-button>
        <el-radio-button :value="0">休假</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center py-20">
      <el-icon class="is-loading" :size="32" color="#1a73e8"><Loading /></el-icon>
    </div>

    <!-- 空状态 -->
    <el-empty v-else-if="!loading && teamMembers.length === 0" description="暂无部门成员数据" />

    <!-- 成员卡片网格 -->
    <template v-else>
      <el-row :gutter="24">
        <el-col :span="6" v-for="user in teamMembers" :key="user.id" class="mb-6">
          <el-card shadow="hover" class="!border-none !rounded-3xl overflow-hidden group hover:shadow-xl transition-all duration-300">
            <div class="p-6">
              <div class="flex items-start justify-between mb-4">
                <el-avatar :size="64" :src="user.avatar" class="border-4 border-white shadow-sm ring-2 ring-blue-50">
                  {{ user.realName?.substring(0, 1) || '?' }}
                </el-avatar>
                <div class="flex flex-col items-end">
                  <el-tag :type="user.roleColor as any" size="small" class="!rounded-full border-none px-3 font-bold scale-90 origin-right">{{ user.roleName }}</el-tag>
                  <span class="text-[10px] text-gray-400 mt-2 uppercase font-mono">{{ user.employeeNo }}</span>
                </div>
              </div>

              <div class="mb-4">
                <h3 class="text-xl font-bold text-[#1f1f1f]">{{ user.realName }}</h3>
                <p class="text-xs text-gray-400 mt-1 flex items-center gap-1">
                  <el-icon><Phone /></el-icon> {{ user.phone }}
                </p>
              </div>

              <!-- 黄金复合视图：店长/超管专属管理区域 -->
              <div v-if="isManager" class="bg-gray-50 rounded-2xl p-4 mb-4">
                <div class="flex items-center justify-between mb-2">
                  <span class="text-xs text-gray-500">本月业绩</span>
                  <!-- 快捷在职状态切换 -->
                  <div class="flex items-center gap-1.5 scale-90 origin-right">
                    <span class="text-[10px] font-bold" :class="user.status === 1 ? 'text-[#34a853]' : 'text-gray-400'">
                      {{ user.status === 1 ? '在职' : '休假' }}
                    </span>
                    <el-switch
                      :model-value="user.status"
                      :active-value="1"
                      :inactive-value="0"
                      active-color="#34a853"
                      inactive-color="#dadce0"
                      size="small"
                      :loading="statusLoadingMap[user.id]"
                      @change="(val: any) => handleStatusChange(user, val)"
                    />
                  </div>
                </div>
                <p v-if="performanceMap[user.id]" class="text-lg font-bold text-[#1a73e8]">
                  {{ performanceMap[user.id].totalDealAmount.toFixed(1) }}<span class="text-xs font-normal text-gray-400">万</span>
                  <el-tag size="small" class="!rounded-full !ml-2" type="info">{{ performanceMap[user.id].totalOrders }}单</el-tag>
                </p>
                <p v-else class="text-xs text-gray-400 py-2">暂无数据</p>
              </div>

              <!-- 黄金复合视图：普通置业顾问专属协作与隐私隔离区域 -->
              <div v-else class="bg-gray-50 rounded-2xl p-4 mb-4 flex flex-col justify-between h-[84px]">
                <div class="flex items-center justify-between">
                  <span class="text-xs text-gray-500">在职状态</span>
                  <el-tag
                    :type="user.status === 1 ? 'success' : 'danger'"
                    size="small"
                    class="!rounded-full border-none px-2.5 font-bold scale-90 origin-right"
                  >
                    {{ user.status === 1 ? '🟢 在职在岗' : '🔴 今日休假' }}
                  </el-tag>
                </div>
                <!-- 业务擅长标签墙 -->
                <div class="flex flex-wrap gap-1.5 overflow-hidden max-h-[36px] mt-2">
                  <el-tag
                    v-for="(tag, index) in (user.remark ? user.remark.split(',') : ['精通一手', '专业带看', '高效服务'])"
                    :key="index"
                    size="small"
                    class="!rounded-md border-none px-2 scale-90 origin-left font-semibold text-[10px]"
                    type="info"
                    effect="light"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </div>

              <div class="flex gap-2">
                <el-button class="flex-1 !rounded-xl !h-10 text-xs font-bold" plain @click="handleViewPerformance(user)">查看绩效</el-button>
                <el-button class="!rounded-xl !w-10 !h-10 !p-0" plain :loading="chatLoadingMap[user.id]" @click="handleChat(user)">
                  <el-icon><ChatDotRound /></el-icon>
                </el-button>
                <el-dropdown v-if="checkPermi(['team:user:dept']) || checkPermi(['team:user:promote']) || checkPermi(['team:user:handover']) || checkPermi(['team:user:remove'])" trigger="click" @command="(cmd: string) => handleMoreCommand(cmd, user)">
                  <el-button class="!rounded-xl !w-10 !h-10 !p-0" plain>
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu class="!rounded-2xl !p-2 !border-none shadow-xl">
                      <el-dropdown-item v-if="checkPermi(['team:user:dept'])" command="dept" class="!rounded-xl !py-2">
                        <el-icon><Switch /></el-icon>调整部门
                      </el-dropdown-item>
                      <el-dropdown-item v-if="checkPermi(['team:user:promote'])" command="promote" class="!rounded-xl !py-2">
                        <el-icon><Top /></el-icon>设为负责人
                      </el-dropdown-item>
                      <el-dropdown-item v-if="checkPermi(['team:user:handover'])" command="handover" class="!rounded-xl !py-2">
                        <el-icon><RefreshRight /></el-icon>工作交接
                      </el-dropdown-item>
                      <el-dropdown-item v-if="checkPermi(['team:user:remove'])" command="remove" divided class="!rounded-xl !py-2 !text-red-500">
                        <el-icon><Remove /></el-icon>移除部门
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 分页 -->
      <div class="flex justify-between items-center pt-2 pb-6">
        <span class="text-xs text-gray-400 font-medium">显示 {{ teamMembers.length }} / {{ total }} 位成员</span>
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          layout="prev, pager, next, sizes"
          :total="total"
          :page-sizes="[12, 24, 48]"
          class="team-pagination"
          @current-change="handleQuery"
          @size-change="handleQuery"
        />
      </div>
    </template>

    <!-- 新增成员对话框 -->
    <el-dialog v-model="addDialogVisible" title="新增成员" width="480px" class="google-dialog" :close-on-click-modal="false">
      <el-form :model="addForm" label-position="top" class="px-4">
        <el-form-item label="登录用户名">
          <el-input v-model="addForm.username" placeholder="用于登录系统的账号" class="google-input-flat" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="addForm.realName" placeholder="请输入真实姓名" class="google-input-flat" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="addForm.phone" placeholder="请输入手机号" class="google-input-flat" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false" class="!rounded-full px-6">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAddSubmit" class="!rounded-full px-6 !bg-[#1a73e8] border-none">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 调整部门对话框 -->
    <el-dialog v-model="deptDialogVisible" title="调整部门" width="400px" class="google-dialog" :close-on-click-modal="false">
      <div class="px-4 py-2">
        <p class="text-sm text-gray-500 mb-4">将 <strong>{{ selectedUser?.realName }}</strong> 调至</p>
        <el-tree-select
          v-model="deptForm.deptId"
          :data="deptTreeData"
          :props="{ label: 'deptName', value: 'id', children: 'children' }"
          value-key="id"
          placeholder="请选择目标部门"
          check-strictly
          class="w-full google-input-flat"
        />
      </div>
      <template #footer>
        <el-button @click="deptDialogVisible = false" class="!rounded-full px-6">取消</el-button>
        <el-button type="primary" @click="handleDeptSubmit" class="!rounded-full px-6 !bg-[#1a73e8] border-none">确认调动</el-button>
      </template>
    </el-dialog>

    <!-- 设置月度目标 -->
    <el-dialog v-model="targetDialogVisible" title="设置月度目标" width="420px" class="google-dialog" :close-on-click-modal="false">
      <div class="px-4 py-2">
        <p class="text-sm text-gray-500 mb-4">为当前部门设置 {{ currentMonth }} 的业绩目标</p>
        <el-form label-position="top">
          <el-form-item label="目标业绩（万元）">
            <el-input-number v-model="targetForm.targetAmount" :min="0" :precision="1" :step="10" class="w-full" controls-position="right" placeholder="请输入目标金额" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="targetDialogVisible = false" class="!rounded-full px-6">取消</el-button>
        <el-button type="primary" :loading="targetLoading" @click="handleSaveTarget" class="!rounded-full px-6 !bg-[#1a73e8] border-none">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Phone, ChatDotRound, MoreFilled, Download, Plus, Loading, Switch, Top, RefreshRight, Remove, EditPen } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { listTeamMembers, type TeamMember } from '@/api/team/user'
import { checkPermi } from '@/utils/permission'
import { addUser, changeUserStatus } from '@/api/system/user'
import { listDeptTree, type Dept } from '@/api/system/dept'
import { createSession } from '@/api/message/chat'
import { getDeptTarget, saveDeptTarget, getMemberPerformance, type MemberPerformance } from '@/api/team/performance'

const loading = ref(false)
const teamMembers = ref<TeamMember[]>([])
const total = ref(0)
const searchText = ref('')
const selectedUser = ref<TeamMember | null>(null)
const router = useRouter()
const userStore = useUserStore()

/** 判断当前登录用户是否为管理层 (店长/超管) */
const isManager = computed(() => {
  return userStore.permissions.includes('team:performance:query') ||
         userStore.userInfo?.roleCodes?.includes('admin') ||
         userStore.userInfo?.roleCodes?.includes('manager')
})

/** 是否可切换部门（仅超管/拥有用户管理权限的用户） */
const canSwitchDept = computed(() => {
  return userStore.permissions.includes('system:user:query') ||
         userStore.userInfo?.roleCodes?.includes('admin')
})

/** 每个成员状态修改时的 Loading 映射 */
const statusLoadingMap = ref<Record<number, boolean>>({})

/** 快捷修改成员状态 (店长专用) */
const handleStatusChange = async (user: TeamMember, newStatus: number) => {
  if (!user.id) return
  statusLoadingMap.value[user.id] = true
  try {
    await changeUserStatus(user.id, newStatus)
    ElMessage.success(`已成功修改 ${user.realName} 的在职状态`)
    // 重新加载数据，保证 workStatus 等视图计算自动刷新
    await handleQuery()
  } catch (e) {
    console.error('修改状态失败:', e)
    ElMessage.error('修改状态失败，请稍后重试')
  } finally {
    statusLoadingMap.value[user.id] = false
  }
}

const deptTreeData = ref<Dept[]>([])
const chatLoadingMap = ref<Record<number, boolean>>({})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 12,
  deptId: undefined as number | undefined,
  status: undefined as number | undefined
})

/** 当前月份 YYYY-MM */
const currentMonth = `${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}`

/** 部门目标 */
const deptTarget = ref<any>(null)

/** 成员业绩映射 userId -> MemberPerformance */
const performanceMap = ref<Record<number, MemberPerformance>>({})

/** 部门总业绩（万元） */
const totalPerformance = computed(() => {
  return Object.values(performanceMap.value).reduce((sum, p) => sum + p.totalDealAmount, 0)
})

/** 获取成员列表 */
const handleQuery = async () => {
  loading.value = true
  try {
    const res = await listTeamMembers({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      deptId: queryParams.deptId,
      realName: searchText.value || undefined,
      phone: searchText.value || undefined,
      status: queryParams.status
    })
    teamMembers.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
  fetchDeptTargetAndPerformance()
}

/** 获取部门目标与成员业绩 */
const fetchDeptTargetAndPerformance = () => {
  if (!queryParams.deptId) {
    deptTarget.value = null
    performanceMap.value = {}
    return
  }
  getDeptTarget(queryParams.deptId, currentMonth).then(res => {
    deptTarget.value = res.data
  }).catch(e => {
    console.error('获取部门目标失败:', e)
  })
  if (teamMembers.value.length === 0) return
  getMemberPerformance(teamMembers.value.map(m => m.id), currentMonth).then(res => {
    const map: Record<number, MemberPerformance> = {}
    if (res.data) {
      res.data.forEach(p => { map[p.userId] = p })
    }
    performanceMap.value = map
  }).catch(e => {
    console.error('获取成员业绩失败:', e)
  })
}

/** 点击搜索或按回车触发 */
const handleSearch = () => {
  queryParams.pageNum = 1
  handleQuery()
}

/** 进度条颜色 */
const progressColor = (rate: number) => {
  if (rate >= 0.8) return '#34a853'
  if (rate >= 0.5) return '#1a73e8'
  return '#fbbc04'
}

// ========== 操作交互 ==========

const handleViewPerformance = (user: TeamMember) => {
  ElMessage.info(`查看 ${user.realName} 的绩效详情`)
}

/** 从用户菜单树中查找聊天页面的路由路径（优先子节点，避免被父级目录提前拦截） */
const findChatPath = (): string => {
  const search = (items: any[], parentPath = ''): string | null => {
    for (const m of items) {
      const fullPath = parentPath
        ? `${parentPath}${parentPath.endsWith('/') ? '' : '/'}${m.path}`
        : (m.path?.startsWith('/') ? m.path : (m.path ? '/' + m.path : ''))
      // 先搜子节点（子节点路径更精确）
      if (m.children?.length) {
        const found = search(m.children, fullPath)
        if (found) return found
      }
      // 子节点未匹配到，再检查当前节点
      if (m.menuName?.includes('聊天') || m.menuName?.includes('消息')) return fullPath
    }
    return null
  }
  return search(userStore.menuList) || '/message/chat'
}

const handleChat = async (user: TeamMember) => {
  if (!user.id) return
  // 如果点击的就是自己，不需要创建会话，直接跳转
  const currentUserId = userStore.userInfo?.id
  if (currentUserId && Number(currentUserId) === user.id) {
    ElMessage.info('不能与自己聊天')
    return
  }
  chatLoadingMap.value[user.id] = true
  try {
    // 私聊需要把自己也加入 members，后端才能触发去重（size==2）
    const selfId = currentUserId ? Number(currentUserId) : undefined
    const membersPayload: { userType: number; userId: number }[] = [
      { userType: 1, userId: user.id }
    ]
    if (selfId) {
      membersPayload.push({ userType: 1, userId: selfId })
    }
    const res = await createSession({
      sessionType: 1,
      members: membersPayload
    })
    const sessionId = res.data?.id
    if (sessionId) {
      const chatPath = findChatPath()
      router.push({ path: chatPath, query: { sessionId } })
    } else {
      ElMessage.error('创建会话失败，请稍后重试')
    }
  } catch (e) {
    console.error('[handleChat] createSession error:', e)
    ElMessage.error('创建会话失败')
  } finally {
    chatLoadingMap.value[user.id] = false
  }
}

const handleMoreCommand = (cmd: string, user: TeamMember) => {
  switch (cmd) {
    case 'dept':
      selectedUser.value = user
      deptForm.deptId = user.deptId
      deptDialogVisible.value = true
      break
    case 'promote':
      ElMessageBox.confirm(
        `确定将 ${user.realName} 提升为部门负责人吗？`,
        '设为负责人',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
      ).then(() => ElMessage.success(`已提升 ${user.realName} 为部门负责人`))
      break
    case 'handover':
      ElMessage.info(`工作交接功能开发中，即将支持 ${user.realName} 的房源与客户批量转让`)
      break
    case 'remove':
      ElMessageBox.confirm(
        `确定将 ${user.realName} 移出当前部门吗？此操作不会删除账号。`,
        '移除部门',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
      ).then(() => ElMessage.success(`已移除 ${user.realName}`))
      break
  }
}

// ========== 部门对话框 ==========

const deptDialogVisible = ref(false)
const deptForm = reactive({ deptId: undefined as number | undefined })

const findDeptName = (nodes: Dept[], id: number): string | undefined => {
  for (const node of nodes) {
    if (node.id === id) return node.deptName
    if (node.children?.length) {
      const found = findDeptName(node.children, id)
      if (found) return found
    }
  }
  return undefined
}

const handleDeptSubmit = () => {
  const deptName = deptForm.deptId ? findDeptName(deptTreeData.value, deptForm.deptId) : undefined
  ElMessage.success(`${selectedUser.value?.realName} 已调至 ${deptName || '所选部门'}`)
  deptDialogVisible.value = false
}

// ========== 部门目标 ==========

const targetDialogVisible = ref(false)
const targetLoading = ref(false)
const targetForm = reactive({
  id: undefined as number | undefined,
  targetAmount: undefined as number | undefined
})

const handleEditTarget = () => {
  targetForm.id = deptTarget.value?.id
  targetForm.targetAmount = deptTarget.value?.targetAmount ?? undefined
  targetDialogVisible.value = true
}

const handleSaveTarget = async () => {
  if (!targetForm.targetAmount || targetForm.targetAmount <= 0) {
    ElMessage.warning('请输入有效的目标金额')
    return
  }
  targetLoading.value = true
  try {
    await saveDeptTarget({
      id: targetForm.id,
      deptId: queryParams.deptId!,
      targetMonth: currentMonth,
      targetAmount: targetForm.targetAmount
    })
    ElMessage.success('目标保存成功')
    targetDialogVisible.value = false
    await fetchDeptTargetAndPerformance()
  } finally {
    targetLoading.value = false
  }
}

// ========== 新增成员 ==========

const addDialogVisible = ref(false)
const addLoading = ref(false)
const addForm = reactive({
  username: '',
  realName: '',
  phone: ''
})

const handleAdd = () => {
  addForm.username = ''
  addForm.realName = ''
  addForm.phone = ''
  addDialogVisible.value = true
}

const handleAddSubmit = async () => {
  if (!addForm.username || !addForm.realName || !addForm.phone) {
    ElMessage.warning('请完整填写用户信息')
    return
  }
  addLoading.value = true
  try {
    await addUser({
      username: addForm.username,
      realName: addForm.realName,
      phone: addForm.phone
    })
    ElMessage.success('成员添加成功')
    addDialogVisible.value = false
    handleQuery()
  } finally {
    addLoading.value = false
  }
}

/** 加载部门树 */
const fetchDeptTree = async () => {
  try {
    const res = await listDeptTree()
    deptTreeData.value = res.data || []
  } catch {
    deptTreeData.value = []
  }
}

onMounted(() => {
  fetchDeptTree()
  // 非管理员自动锁定为当前用户所属部门
  if (!canSwitchDept.value && userStore.userInfo?.deptId) {
    queryParams.deptId = userStore.userInfo.deptId
  }
  handleQuery()
})
</script>

<style scoped>
.team-user-container {
  height: calc(100vh - 110px);
  overflow-y: auto;
}

/* Google 扁平化输入框 */
:deep(.google-input-flat .el-input__wrapper) {
  border-radius: 12px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.2s;
  padding: 0 16px;
}

:deep(.google-input-flat .el-input__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px #1a73e8 !important;
}

:deep(.google-select-flat .el-select__wrapper) {
  border-radius: 12px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.2s;
}

:deep(.google-select-flat .el-select__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
}

/* Tree Select 扁平化 */
:deep(.google-input-flat .el-tree-select__wrapper) {
  border-radius: 12px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.2s;
}

:deep(.google-input-flat .el-tree-select__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px #1a73e8 !important;
}

/* Radio Group 扁平化胶囊 */
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

/* 进度条圆角 */
:deep(.google-progress .el-progress-bar__inner) {
  border-radius: 4px;
}
:deep(.google-progress .el-progress-bar__outer) {
  background-color: #e8eaed;
}

/* 分页 */
:deep(.team-pagination .el-pager li) {
  border-radius: 10px;
  margin: 0 3px;
  font-weight: 800;
  font-size: 12px;
  color: #5f6368;
  min-width: 32px;
  height: 32px;
}

:deep(.team-pagination .el-pager li.is-active) {
  background-color: #1a73e8;
  color: #fff;
}

:deep(.team-pagination .btn-prev),
:deep(.team-pagination .btn-next) {
  min-width: 32px;
  height: 32px;
}

/* 对话框 */
:deep(.google-dialog .el-dialog__header) {
  padding: 24px 24px 0;
  font-weight: 800;
  font-size: 18px;
}

:deep(.google-dialog .el-dialog__body) {
  padding: 16px 24px;
}

</style>
