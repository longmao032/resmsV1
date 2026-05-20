<template>
  <div class="user-management-container p-6 bg-gray-50 min-h-full">
    <!-- 统一包装容器 -->
    <div class="flex h-full bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <!-- 左侧部门树 (物理分割) -->
      <div class="dept-sidebar w-72 shrink-0 border-r border-gray-100 flex flex-col py-6">
        <div class="px-6 mb-4 flex items-center gap-2">
          <el-icon class="text-[#1a73e8]"><OfficeBuilding /></el-icon>
          <span class="font-bold text-[#1f1f1f]">组织架构</span>
        </div>
        
        <div class="px-6 mb-4">
          <el-input
            v-model="deptFilter"
            placeholder="搜索部门"
            prefix-icon="Search"
            class="google-input-flat"
            clearable
          />
        </div>
        
        <el-scrollbar class="flex-1 px-2">
          <el-tree
            ref="deptTreeRef"
            :data="deptData"
            :props="defaultProps"
            node-key="id"
            default-expand-all
            :filter-node-method="filterDeptNode"
            highlight-current
            @node-click="handleDeptClick"
            class="dept-tree-flat"
          >
            <template #default="{ node, data }">
              <span class="flex items-center gap-2 text-sm">
                <el-icon v-if="data.children && data.children.length > 0" class="text-blue-400/80"><Folder /></el-icon>
                <el-icon v-else class="text-gray-400/80"><UserFilled /></el-icon>
                <span>{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </el-scrollbar>
      </div>

      <!-- 右侧列表区域 -->
      <div class="flex-1 flex flex-col min-w-0">
        <!-- 搜索区域 -->
        <div class="p-6 border-b border-gray-50">
          <el-form :inline="true" :model="queryParams" class="flex flex-wrap gap-x-8 gap-y-4">
            <el-form-item label="用户名" class="!mb-0">
              <el-input v-model="queryParams.username" placeholder="用户名" clearable class="google-input-flat !w-44" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="手机号" class="!mb-0">
              <el-input v-model="queryParams.phone" placeholder="手机号" clearable class="google-input-flat !w-44" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="状态" class="!mb-0">
              <el-select v-model="queryParams.status" placeholder="状态" clearable class="google-input-flat !w-32">
                <el-option label="正常" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
            <div class="flex gap-2 ml-auto">
              <el-button type="primary" icon="Search" class="!rounded-lg !bg-[#1a73e8] border-none px-6" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" class="!rounded-lg px-6" @click="resetQuery">重置</el-button>
            </div>
          </el-form>
        </div>

        <!-- 操作栏 -->
        <div class="px-6 py-4 flex items-center justify-between">
          <div class="flex gap-3">
            <el-button type="primary" icon="Plus" class="!rounded-lg px-5 !bg-[#1a73e8] border-none" @click="handleAdd" v-hasPermi="['system:user:add']">新增用户</el-button>
            <el-button plain icon="Delete" class="!rounded-lg px-5" :disabled="!multipleSelection.length" @click="handleDelete" v-hasPermi="['system:user:delete']">批量删除</el-button>
          </div>
          <div class="flex gap-1">
            <el-tooltip content="刷新数据" placement="top" effect="dark">
              <el-button icon="Refresh" link class="!p-2 hover:bg-gray-100 rounded-full" @click="handleQuery" />
            </el-tooltip>
          </div>
        </div>

        <!-- 表格区域 -->
        <div class="flex-1 px-6 pb-6 overflow-hidden">
          <el-table
            v-loading="loading"
            :data="userList"
            row-key="id"
            height="100%"
            class="google-table-flat"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column label="编号" prop="id" width="80" align="center" />
            <el-table-column label="用户" min-width="150">
              <template #default="{ row }">
                <div class="flex items-center gap-3">
                  <el-avatar :size="32" :src="row.avatar" class="!bg-blue-100 text-blue-600">
                    {{ row.realName ? row.realName.charAt(0) : row.username.charAt(0) }}
                  </el-avatar>
                  <div class="flex flex-col">
                    <span class="font-medium text-[#1f1f1f]">{{ row.nickName || row.username }}</span>
                    <span class="text-xs text-gray-400">{{ row.realName }}（{{ row.username }}）</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="性别" width="70" align="center">
              <template #default="{ row }">
                <span>{{ row.sex === 1 ? '男' : row.sex === 2 ? '女' : '未知' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="部门" prop="deptName" min-width="120" />
            <el-table-column label="角色" min-width="180">
              <template #default="{ row }">
                <div class="flex flex-wrap gap-1">
                  <el-tag v-for="role in (row.roles.length > 1 ? row.roles.filter((r: any) => r.id !== 6) : row.roles)" :key="role.id" size="small" class="!rounded-full px-3" effect="light" type="info">
                    {{ role.name }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="手机号" prop="phone" width="120" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <div 
                  class="cursor-pointer select-none py-1 px-3 rounded-full transition-all inline-block"
                  :class="row.status === 1 ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-500'"
                  @click="handleStatusChange(row)"
                  v-hasPermi="['system:user:edit']"
                >
                  <span class="text-xs font-bold">{{ row.status === 1 ? '正常' : '禁用' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="最后登录" min-width="180" align="center">
              <template #default="{ row }">
                <div class="flex flex-col text-xs text-gray-400">
                  <span>{{ row.lastLoginTime || '-' }}</span>
                  <span>{{ row.lastLoginIp || '' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" prop="createTime" width="170" align="center" />
            <el-table-column label="操作" width="160" fixed="right" align="center">
              <template #default="{ row }">
                 <div class="flex items-center justify-center gap-1">
                  <el-tooltip v-if="checkPermi(['system:user:edit'])" content="修改用户" placement="top" effect="dark">
                    <el-button link icon="Edit" class="!p-2 hover:bg-gray-100 rounded-full transition-colors text-gray-600" @click="handleUpdate(row)" />
                  </el-tooltip>
                  <el-tooltip v-if="checkPermi(['system:user:password'])" content="重置密码" placement="top" effect="dark">
                    <el-button link icon="Key" class="!p-2 hover:bg-gray-100 rounded-full transition-colors text-gray-600" @click="handleResetPwd(row)" />
                  </el-tooltip>
                  <el-tooltip v-if="checkPermi(['system:user:delete'])" content="删除用户" placement="top" effect="dark">
                    <el-button link icon="Delete" class="!p-2 hover:bg-red-50 rounded-full transition-colors text-red-500" @click="handleDelete(row)" />
                  </el-tooltip>
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
            :page-sizes="[10, 20, 30, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            background
            class="google-pagination"
            @size-change="handleQuery"
            @current-change="handleQuery"
          />
        </div>
      </div>
    </div>

    <!-- 用户表单弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body class="google-dialog">
      <el-form ref="userFormRef" :model="userForm" :rules="rules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="!!userForm.id" class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="userForm.realName" placeholder="请输入真实姓名" class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickName">
              <el-input v-model="userForm.nickName" placeholder="请输入昵称" class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="userForm.phone" placeholder="请输入手机号" class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱" prop="email">
              <el-input v-model="userForm.email" placeholder="请输入电子邮箱" class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="!userForm.id">
            <el-form-item label="登录密码" prop="password">
              <el-input v-model="userForm.password" type="password" placeholder="密码" show-password class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="sex">
              <el-select v-model="userForm.sex" placeholder="请选择性别" class="w-full google-input-flat">
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
                <el-option label="未知" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="deptId">
              <el-tree-select
                v-model="userForm.deptId"
                :data="deptData"
                :props="{ label: 'deptName', value: 'id' }"
                value-key="id"
                placeholder="请选择部门"
                check-strictly
                class="w-full google-input-flat"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="userForm.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色分配" prop="roleIds">
              <el-select v-model="userForm.roleIds" multiple placeholder="请分配角色" class="w-full google-input-flat">
                <el-option v-for="role in allRoles" :key="role.id" :label="role.roleName" :value="role.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="userForm.remark" type="textarea" placeholder="备注内容" :rows="3" class="google-input-flat" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3 px-4 pb-4">
          <el-button @click="dialogVisible = false" class="!rounded-lg px-6">取 消</el-button>
          <el-button type="primary" @click="submitForm" class="!rounded-lg px-6 !bg-[#1a73e8] border-none">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, nextTick } from 'vue'
import { OfficeBuilding, Search, Folder, UserFilled, Plus, Delete, Download, Refresh, Edit, Key } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUser, addUser, updateUser, delUser, changeUserStatus, resetUserPwd, type User } from '@/api/system/user'
import { listAllRoles } from '@/api/system/role'
import { listDeptTree } from '@/api/system/dept'
import { checkPermi } from '@/utils/permission'

// --- 状态与参数 ---
const deptFilter = ref('')
const deptTreeRef = ref()
const loading = ref(false)
const total = ref(0)
const userList = ref<User[]>([])
const deptData = ref<any[]>([])
const allRoles = ref<any[]>([])
const multipleSelection = ref<User[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const userFormRef = ref()

const defaultProps = {
  children: 'children',
  label: 'deptName'
}

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  phone: '',
  deptId: undefined,
  status: undefined
})

const userForm = reactive({
  id: undefined,
  username: '',
  password: '',
  realName: '',
  nickName: '',
  phone: '',
  email: '',
  deptId: undefined,
  roleIds: [],
  remark: '',
  status: 1,
  sex: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

// --- 初始化 ---
onMounted(() => {
  handleQuery()
  getDeptTree()
  getAllRoles()
})

// --- 方法 ---
const getDeptTree = async () => {
  const { data } = await listDeptTree()
  deptData.value = data
}

const getAllRoles = async () => {
  const { data } = await listAllRoles()
  allRoles.value = data
}

const handleQuery = async () => {
  loading.value = true
  try {
    const { data } = await listUser(queryParams)
    userList.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.username = ''
  queryParams.phone = ''
  queryParams.deptId = undefined
  queryParams.status = undefined
  handleQuery()
}

watch(deptFilter, (val) => {
  deptTreeRef.value?.filter(val)
})

const filterDeptNode = (value: string, data: any) => {
  if (!value) return true
  return data.deptName.includes(value)
}

const handleDeptClick = (data: any) => {
  queryParams.deptId = data.id
  handleQuery()
}

const handleSelectionChange = (val: any) => {
  multipleSelection.value = val
}

const handleAdd = () => {
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
  nextTick(() => {
    userFormRef.value?.resetFields()
    Object.assign(userForm, {
      id: undefined,
      username: '',
      password: '',
      realName: '',
      nickName: '',
      phone: '',
      email: '',
      deptId: queryParams.deptId,
      roleIds: [],
      remark: '',
      status: 1,
      sex: 1
    })
  })
}

const handleUpdate = (row: User) => {
  dialogTitle.value = '修改用户'
  dialogVisible.value = true
  nextTick(() => {
    userFormRef.value?.resetFields()
    Object.assign(userForm, {
      ...row,
      nickName: row.nickName || '',
      roleIds: row.roles ? row.roles.map(r => r.id) : []
    })
  })
}

const handleDelete = (row?: User) => {
  const ids = row ? [row.id] : multipleSelection.value.map(u => u.id)
  const names = row ? row.username : '选中的用户'
  
  ElMessageBox.confirm(`确定要删除用户 "${names}" 吗？`, '警告', {
    type: 'warning'
  }).then(async () => {
    for (const id of ids) {
      await delUser(id!)
    }
    ElMessage.success('删除成功')
    handleQuery()
  })
}

const handleResetPwd = (row: User) => {
  ElMessageBox.prompt(`请输入用户 "${row.username}" 的新密码`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^.{6,20}$/,
    inputErrorMessage: '密码长度在 6 到 20 个字符之间'
  }).then(async ({ value }) => {
    await resetUserPwd(row.id!, value)
    ElMessage.success('密码重置成功')
  })
}

const handleStatusChange = async (row: User) => {
  const targetStatus = row.status === 1 ? 0 : 1
  try {
    await changeUserStatus(row.id!, targetStatus)
    row.status = targetStatus
    ElMessage.success(`${targetStatus === 1 ? '启用' : '禁用'}成功`)
  } catch (error) {
    console.error('修改状态失败:', error)
  }
}

const submitForm = () => {
  userFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      if (userForm.id) {
        await updateUser(userForm as any)
        ElMessage.success('修改成功')
      } else {
        await addUser(userForm as any)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      handleQuery()
    }
  })
}

</script>

<style scoped>
.user-management-container {
  height: calc(100vh - 120px);
}

/* 扁平化树形 */
.dept-tree-flat {
  --el-tree-node-content-height: 44px;
}

:deep(.dept-tree-flat .el-tree-node__content) {
  margin: 0 16px;
  border-radius: 8px;
  padding-left: 8px !important;
}

:deep(.dept-tree-flat .el-tree-node.is-current > .el-tree-node__content) {
  background-color: #f1f3f4 !important;
  color: #1a73e8 !important;
  font-weight: 600;
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

/* 分页器 */
:deep(.google-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #1a73e8 !important;
}

/* 表单 Label 样式 */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #3c4043;
}

/* Switch 样式优化 */
:deep(.google-switch.el-switch.is-checked .el-switch__core) {
  background-color: #1a73e8;
  border-color: #1a73e8;
}
</style>

<style>
/* 全局弹窗优化 */
.google-dialog {
  border-radius: 24px !important;
  box-shadow: 0 12px 32px 4px rgba(0,0,0,0.1) !important;
}

.google-dialog .el-dialog__header {
  padding: 24px 24px 8px;
}

.google-dialog .el-dialog__title {
  font-size: 22px;
  font-weight: 600;
  color: #202124;
}
</style>
