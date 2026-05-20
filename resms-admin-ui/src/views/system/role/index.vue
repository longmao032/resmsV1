<template>
  <div class="role-management-container p-6 bg-gray-50 min-h-full">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="!border-none rounded-3xl shadow-sm mb-6 p-2">
      <el-form :inline="true" :model="queryParams" class="flex flex-wrap items-center gap-y-4">
        <el-form-item label="角色名称" class="!mb-0">
          <el-input v-model="queryParams.roleName" placeholder="角色名称" clearable class="google-input-flat !w-48" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="角色代码" class="!mb-0">
          <el-input v-model="queryParams.roleCode" placeholder="角色代码" clearable class="google-input-flat !w-48" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" class="!mb-0">
          <el-select v-model="queryParams.status" placeholder="角色状态" clearable class="google-input-flat !w-32">
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <div class="flex-1"></div>
        <div class="flex gap-2">
          <el-button type="primary" icon="Search" class="!rounded-lg !bg-[#1a73e8] border-none px-6" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" class="!rounded-lg px-6" @click="resetQuery">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never" class="flex-1 !border-none rounded-3xl shadow-sm flex flex-col overflow-hidden">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex gap-3">
            <el-button type="primary" icon="Plus" class="!rounded-lg px-6 !bg-[#1a73e8] border-none" @click="handleAdd" v-hasPermi="['system:role:add']">新增角色</el-button>
            <el-button type="danger" plain icon="Delete" class="!rounded-lg px-6" :disabled="!multipleSelection.length" @click="handleDelete()" v-hasPermi="['system:role:delete']">批量删除</el-button>
          </div>
          <div class="flex gap-2">
            <el-tooltip content="刷新数据" placement="top">
              <el-button icon="Refresh" link class="!p-2 hover:bg-gray-100 rounded-full" @click="handleQuery" />
            </el-tooltip>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="roleList"
        class="google-table-flat"
        height="100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="角色编号" prop="id" width="100" align="center" />
        <el-table-column label="角色名称" prop="roleName" min-width="150" />
        <el-table-column label="角色代码" prop="roleCode" min-width="150">
          <template #default="{ row }">
            <span class="px-2 py-0.5 bg-gray-100 text-gray-500 rounded font-mono text-xs">{{ row.roleCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数据权限" width="160">
          <template #default="{ row }">
            <el-tag size="small" type="info" class="!rounded-full px-3 low-sat-tag">
              {{ getDataScopeLabel(row.dataScope) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <div 
              class="cursor-pointer select-none py-1 px-3 rounded-full transition-all inline-block"
              :class="row.status === 1 ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-500'"
              @click="handleStatusChange(row)"
              v-hasPermi="['system:role:edit']"
            >
              <span class="text-xs font-bold">{{ row.status === 1 ? '正常' : '禁用' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180" align="center" />
        <el-table-column label="操作" align="center" width="140" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-1">
              <el-tooltip v-if="checkPermi(['system:role:edit'])" content="修改角色" placement="top">
                <el-button link icon="Edit" class="!p-2 hover:bg-gray-100 rounded-full transition-colors text-gray-600" @click="handleUpdate(row)" />
              </el-tooltip>
              <el-tooltip v-if="checkPermi(['system:role:delete'])" content="删除角色" placement="top">
                <el-button link icon="Delete" class="!p-2 hover:bg-red-50 rounded-full transition-colors text-red-500" @click="handleDelete(row)" />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="py-4 flex justify-end px-6 border-t border-gray-50">
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
    </el-card>

    <!-- 角色新增/修改抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      size="640px"
      append-to-body
      class="google-drawer"
    >
      <el-form ref="roleFormRef" :model="roleForm" :rules="rules" label-width="100px" class="px-2">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="roleForm.roleName" placeholder="角色名称" class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色代码" prop="roleCode">
              <el-input v-model="roleForm.roleCode" placeholder="角色代码 (如: admin)" class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色状态">
              <el-radio-group v-model="roleForm.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">功能权限 (菜单)</el-divider>
        <el-form-item label-width="40px">
          <div class="flex flex-col gap-2 w-full p-4 border border-gray-100 rounded-2xl bg-gray-50/50">
            <div class="flex gap-4 mb-2">
              <el-checkbox v-model="menuExpand" @change="handleCheckedTreeExpand($event)">展开/折叠</el-checkbox>
              <el-checkbox v-model="menuNodeAll" @change="handleCheckedTreeNodeAll($event)">全选/全不选</el-checkbox>
              <el-checkbox v-model="roleForm.menuCheckStrictly">父子联动</el-checkbox>
            </div>
            <el-scrollbar max-height="300px">
              <el-tree
                ref="menuTreeRef"
                :data="menuOptions"
                show-checkbox
                node-key="id"
                :check-strictly="!roleForm.menuCheckStrictly"
                :props="{ label: 'menuName', children: 'children' }"
                class="bg-transparent"
              />
            </el-scrollbar>
          </div>
        </el-form-item>

        <el-divider content-position="left">数据权限 (范围)</el-divider>
        <el-form-item label="权限范围">
          <el-select v-model="roleForm.dataScope" class="w-full google-input-flat">
            <el-option label="全部数据权限" :value="1" />
            <el-option label="本部门数据权限" :value="2" />
            <el-option label="本部门及以下数据权限" :value="3" />
            <el-option label="仅本人数据权限" :value="4" />
          </el-select>
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="角色描述信息" class="google-input-flat" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3 px-4 py-4 border-t border-gray-50 bg-white/80 backdrop-blur-sm">
          <el-button @click="drawerVisible = false" class="!rounded-lg px-8">取 消</el-button>
          <el-button type="primary" @click="submitForm" class="!rounded-lg px-8 !bg-[#1a73e8] border-none">确 定</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { Plus, Delete, Search, Refresh, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRole, addRole, updateRole, delRole, updateRoleStatus, getRoleMenuIds, type Role } from '@/api/system/role'
import { listMenu } from '@/api/system/menu'
import { checkPermi } from '@/utils/permission'

// --- 状态与参数 ---
const loading = ref(false)
const roleList = ref<Role[]>([])
const total = ref(0)
const multipleSelection = ref<Role[]>([])
const drawerVisible = ref(false)
const drawerTitle = ref('')
const roleFormRef = ref()
const menuTreeRef = ref()

const menuOptions = ref<any[]>([])
const menuExpand = ref(false)
const menuNodeAll = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleName: '',
  roleCode: '',
  status: undefined
})

const roleForm = reactive({
  id: undefined,
  roleName: '',
  roleCode: '',
  status: 1,
  menuIds: [],
  description: '',
  dataScope: 1,
  menuCheckStrictly: false
})

const rules = {
  roleName: [{ required: true, message: '角色名称不能为空', trigger: 'blur' }],
  roleCode: [{ required: true, message: '权限代码不能为空', trigger: 'blur' }]
}

// --- 初始化 ---
onMounted(() => {
  handleQuery()
  getMenuOptions()
})

// --- 方法 ---
const getMenuOptions = async () => {
  const { data } = await listMenu({})
  menuOptions.value = data
}

const handleQuery = async () => {
  loading.value = true
  try {
    const { data } = await listRole(queryParams)
    roleList.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.roleName = ''
  queryParams.roleCode = ''
  queryParams.status = undefined
  handleQuery()
}

const handleSelectionChange = (val: any) => {
  multipleSelection.value = val
}

const handleAdd = () => {
  resetFormFields()
  drawerTitle.value = '添加角色'
  drawerVisible.value = true
  nextTick(() => {
    menuTreeRef.value?.setCheckedKeys([])
  })
}

const handleUpdate = async (row: Role) => {
  resetFormFields()
  Object.assign(roleForm, row)
  drawerTitle.value = '修改角色'
  drawerVisible.value = true
  
  const { data } = await getRoleMenuIds(row.id!)
  nextTick(() => {
    menuTreeRef.value?.setCheckedKeys(data)
  })
}

const handleDelete = (row?: Role) => {
  const ids = row ? [row.id] : multipleSelection.value.map(r => r.id)
  const names = row ? row.roleName : '选中的角色'
  
  ElMessageBox.confirm(`是否确认删除角色 "${names}"?`, '提示', {
    type: 'warning'
  }).then(async () => {
    for (const id of ids) {
      await delRole(id!)
    }
    ElMessage.success('删除成功')
    handleQuery()
  })
}

const handleStatusChange = async (row: Role) => {
  const targetStatus = row.status === 1 ? 0 : 1
  try {
    await updateRoleStatus(row.id!, targetStatus)
    row.status = targetStatus
    ElMessage.success(`${targetStatus === 1 ? '启用' : '禁用'}成功`)
  } catch (error) {
    console.error('修改角色状态失败:', error)
  }
}

const resetFormFields = () => {
  Object.assign(roleForm, {
    id: undefined,
    roleName: '',
    roleCode: '',
    status: 1,
    menuIds: [],
    description: '',
    dataScope: 1,
    menuCheckStrictly: false
  })
  roleFormRef.value?.resetFields()
}

const submitForm = () => {
  roleFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      // 获取选中的菜单ID
      roleForm.menuIds = menuTreeRef.value.getCheckedKeys().concat(menuTreeRef.value.getHalfCheckedKeys())
      
      if (roleForm.id) {
        await updateRole(roleForm as any)
        ElMessage.success('修改成功')
      } else {
        await addRole(roleForm as any)
        ElMessage.success('新增成功')
      }
      drawerVisible.value = false
      handleQuery()
    }
  })
}

const handleCheckedTreeExpand = (value: boolean) => {
  const nodes = menuTreeRef.value.store._getAllNodes()
  nodes.forEach((node: any) => node.expanded = value)
}

const handleCheckedTreeNodeAll = (value: boolean) => {
  menuTreeRef.value.setCheckedNodes(value ? menuOptions.value : [])
}

const getDataScopeLabel = (scope: number) => {
  const map: any = {
    1: '全部数据权限',
    2: '本部门数据权限',
    3: '本部门及以下',
    4: '仅本人数据权限'
  }
  return map[scope] || '未知范围'
}

</script>

<style scoped>
.role-management-container {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

/* Google 扁平化表格 */
.google-table-flat {
  --el-table-header-bg-color: transparent;
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

/* 低饱和度标签 */
.low-sat-tag {
  border: none;
  font-weight: 600;
}

:deep(.el-tag--success.low-sat-tag) {
  background-color: #e6f4ea;
  color: #1e8e3e;
}

:deep(.el-tag--danger.low-sat-tag) {
  background-color: #fce8e6;
  color: #d93025;
}

:deep(.el-tag--info.low-sat-tag) {
  background-color: #f1f3f4;
  color: #5f6368;
}

/* 输入框规格 */
:deep(.google-input-flat .el-input__wrapper) {
  border-radius: 8px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  border: 1px solid transparent;
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

/* Switch 样式优化 */
:deep(.google-switch.el-switch.is-checked .el-switch__core) {
  background-color: #1a73e8;
  border-color: #1a73e8;
}
</style>

<style>
/* 抽屉风格化 */
.google-drawer {
  border-radius: 28px 0 0 28px !important;
}
.google-drawer .el-drawer__header {
  margin-bottom: 12px;
  padding: 24px 24px 0;
}
.google-drawer .el-drawer__title {
  font-size: 22px;
  font-weight: 600;
  color: #202124;
}
</style>
