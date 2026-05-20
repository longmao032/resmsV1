<template>
  <div class="dept-management-container p-6 bg-gray-50/50 min-h-full">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="!border-none rounded-[24px] shadow-sm mb-6">
      <el-form :inline="true" :model="queryParams" class="search-form -mb-4">
        <el-form-item label="部门名称">
          <el-input 
            v-model="queryParams.deptName" 
            placeholder="请输入部门名称" 
            clearable 
            class="google-input"
            @keyup.enter="handleQuery" 
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="部门状态" clearable style="width: 160px" class="google-input">
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" class="!rounded-full px-5" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" class="!rounded-full px-5" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据列表 -->
    <el-card shadow="never" class="!border-none rounded-[24px] shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex gap-3">
            <el-button type="primary" icon="Plus" class="!rounded-full px-5" @click="handleAdd()" v-hasPermi="['system:dept:add']">新增部门</el-button>
            <el-button icon="Sort" class="!rounded-full" plain @click="isExpandAll = !isExpandAll">展开/折叠</el-button>
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
        v-if="refreshTable"
        v-loading="loading"
        :data="deptList"
        row-key="id"
        :default-expand-all="isExpandAll"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        class="dept-table"
      >
        <el-table-column prop="deptName" label="部门名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-icon v-if="row.deptType === 1" class="text-blue-600"><OfficeBuilding /></el-icon>
              <el-icon v-else-if="row.deptType === 3" class="text-orange-500"><Shop /></el-icon>
              <el-icon v-else class="text-gray-400"><Folder /></el-icon>
              <span class="font-medium text-[#1f1f1f]">{{ row.deptName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deptCode" label="部门编码" width="120" align="center" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.deptType === 1 ? 'danger' : row.deptType === 3 ? 'warning' : 'info'" size="small">
              {{ row.deptType === 1 ? '公司' : row.deptType === 3 ? '门店' : '部门' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" class="!rounded-md" effect="light">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="130" align="center" />

        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-1">
              <el-tooltip content="部门成员" placement="top" effect="dark">
                <el-button 
                  link 
                  type="success" 
                  icon="User" 
                  class="!p-2 hover:bg-green-50 rounded-full transition-colors"
                  @click="handleMembers(row)"
                />
              </el-tooltip>
              <el-tooltip v-if="checkPermi(['system:dept:add'])" content="新增下级" placement="top" effect="dark">
                <el-button 
                  link 
                  type="primary" 
                  icon="Plus" 
                  class="!p-2 hover:bg-blue-50 rounded-full transition-colors"
                  @click="handleAdd(row)"
                />
              </el-tooltip>
              <el-tooltip v-if="checkPermi(['system:dept:edit'])" content="修改部门" placement="top" effect="dark">
                <el-button 
                  link 
                  type="primary" 
                  icon="Edit" 
                  class="!p-2 hover:bg-blue-50 rounded-full transition-colors"
                  @click="handleUpdate(row)"
                />
              </el-tooltip>
              <el-tooltip v-if="row.parentId !== 0 && checkPermi(['system:dept:delete'])" content="删除部门" placement="top" effect="dark">
                <el-button 
                  link 
                  type="danger" 
                  icon="Delete" 
                  class="!p-2 hover:bg-red-50 rounded-full transition-colors"
                  @click="handleDelete(row)"
                />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 部门表单弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body class="google-dialog">
      <el-form ref="deptFormRef" :model="deptForm" :rules="rules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="24" v-if="deptForm.parentId !== 0">
            <el-form-item label="上级部门" prop="parentId">
              <el-tree-select
                v-model="deptForm.parentId"
                :data="deptOptions"
                :props="{ label: 'deptName', value: 'id', children: 'children' }"
                value-key="id"
                placeholder="选择上级部门"
                check-strictly
                class="w-full google-input"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="deptForm.deptName" placeholder="请输入部门名称" class="google-input" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门编码" prop="deptCode">
              <el-input v-model="deptForm.deptCode" placeholder="请输入部门编码" class="google-input" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门类型" prop="deptType">
              <el-select v-model="deptForm.deptType" placeholder="请选择" class="w-full google-input">
                <el-option label="公司" :value="1" />
                <el-option label="部门" :value="2" />
                <el-option label="门店" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="sortOrder">
              <el-input-number v-model="deptForm.sortOrder" :min="0" class="!w-full" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="deptForm.phone" placeholder="请输入部门电话" class="google-input" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="deptForm.email" placeholder="请输入邮箱" class="google-input" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门状态">
              <el-radio-group v-model="deptForm.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
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

    <!-- 部门成员管理抽屉 -->
    <el-drawer
      v-model="memberDrawer.visible"
      :title="memberDrawer.title"
      size="600px"
      class="google-drawer"
    >
      <div class="p-4 pt-0">
        <div class="flex items-center justify-between mb-4">
          <span class="text-sm text-gray-500">当前部门共有 {{ memberList.length }} 名成员</span>
        </div>
        
        <el-table :data="memberList" class="member-table" v-loading="memberDrawer.loading">
          <el-table-column label="姓名" min-width="120">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <el-avatar :size="32" class="!bg-blue-100 !text-blue-600 font-bold">
                  {{ row.realName ? row.realName.substring(0, 1) : '?' }}
                </el-avatar>
                <div>
                  <div class="font-medium text-gray-900">{{ row.realName }}</div>
                  <div class="text-xs text-gray-500">{{ row.username }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="角色" width="120">
             <template #default="{ row }">
               <el-tag v-for="role in row.roles" :key="role.id" size="small" type="info" class="mr-1">
                 {{ role.name }}
               </el-tag>
             </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" round>
                {{ row.status === 1 ? '在职' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { OfficeBuilding, Search, Folder, Plus, Edit, Delete, Refresh, Sort, User, Shop } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDeptTree, addDept, updateDept, delDept, type Dept } from '@/api/system/dept'
import { listUser } from '@/api/system/user'
import { checkPermi } from '@/utils/permission'

// --- 状态与参数 ---
const loading = ref(false)
const refreshTable = ref(true)
const isExpandAll = ref(true)
const deptList = ref<Dept[]>([])
const deptOptions = ref<Dept[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const deptFormRef = ref()

// 成员管理状态
const memberDrawer = reactive({
  visible: false,
  title: '',
  loading: false,
  currentDeptId: undefined as number | undefined
})
const memberList = ref<any[]>([])

const queryParams = reactive({
  deptName: '',
  status: undefined
})

const deptForm = reactive({
  id: undefined,
  parentId: 0,
  deptName: '',
  deptCode: '',
  sortOrder: 0,
  leaderId: undefined,
  deptType: 2,
  phone: '',
  email: '',
  status: 1
})

const rules = {
  deptName: [{ required: true, message: '部门名称不能为空', trigger: 'blur' }],
  deptCode: [{ required: true, message: '部门编码不能为空', trigger: 'blur' }],
  parentId: [{ required: true, message: '上级部门不能为空', trigger: 'blur' }]
}

// --- 初始化 ---
onMounted(() => {
  handleQuery()
})

// --- 方法 ---
const handleQuery = async () => {
  loading.value = true
  try {
    const { data } = await listDeptTree(queryParams)
    deptList.value = data
    deptOptions.value = data
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.deptName = ''
  queryParams.status = undefined
  handleQuery()
}

const handleAdd = (row?: Dept) => {
  dialogTitle.value = '新增部门'
  dialogVisible.value = true
  nextTick(() => {
    deptFormRef.value?.resetFields()
    Object.assign(deptForm, {
      id: undefined,
      parentId: row ? row.id : 0,
      deptName: '',
      deptCode: '',
      sortOrder: 0,
      leaderId: undefined,
      deptType: 2,
      phone: '',
      email: '',
      status: 1
    })
  })
}

const handleUpdate = (row: Dept) => {
  dialogTitle.value = '修改部门'
  dialogVisible.value = true
  nextTick(() => {
    deptFormRef.value?.resetFields()
    Object.assign(deptForm, { ...row })
  })
}

const handleDelete = (row: Dept) => {
  ElMessageBox.confirm(`确定要删除部门 "${row.deptName}" 吗？`, '警告', {
    type: 'warning'
  }).then(async () => {
    await delDept(row.id!)
    ElMessage.success('删除成功')
    handleQuery()
  })
}

const submitForm = () => {
  deptFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      if (deptForm.id) {
        await updateDept(deptForm as any)
        ElMessage.success('修改成功')
      } else {
        await addDept(deptForm as any)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      handleQuery()
    }
  })
}

// --- 成员管理方法 ---
const handleMembers = async (row: Dept) => {
  memberDrawer.title = `部门成员 - ${row.deptName}`
  memberDrawer.currentDeptId = row.id
  memberDrawer.visible = true
  memberDrawer.loading = true
  try {
    const { data } = await listUser({ deptId: row.id, pageSize: 1000 })
    memberList.value = data.records
  } finally {
    memberDrawer.loading = false
  }
}

</script>

<style scoped>
.dept-management-container {
  height: calc(100vh - 120px);
}

/* 表格样式优化 */
.dept-table {
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

/* 抽屉样式 */
.google-drawer {
  border-top-left-radius: 28px !important;
  border-bottom-left-radius: 28px !important;
}

.google-drawer .el-drawer__header {
  margin-bottom: 20px;
  padding: 24px 24px 0;
}

.google-drawer .el-drawer__title {
  font-size: 20px;
  font-weight: 600;
  color: #1f1f1f;
}

.member-table {
  --el-table-header-bg-color: #f8f9fa;
}
</style>
