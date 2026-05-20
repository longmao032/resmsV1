<template>
  <div class="menu-management-container p-6 bg-gray-50 min-h-full">
    <!-- 顶部操作与搜索区 (全宽紧凑型) -->
    <el-card shadow="never" class="!border-none rounded-[24px] shadow-sm mb-6 search-card">
      <div class="flex items-center justify-between">
        <el-form :inline="true" :model="queryParams" class="flex items-center gap-x-6 -mb-4">
          <el-form-item label="菜单名称">
            <el-input 
              v-model="queryParams.menuName" 
              placeholder="搜索名称" 
              clearable 
              class="google-input-flat !w-44"
              @keyup.enter="handleQuery" 
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="状态" clearable class="google-input-flat !w-28">
              <el-option label="正常" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" class="!rounded-lg !bg-[#1a73e8] border-none px-5" @click="handleQuery" v-hasPermi="['system:menu:query']">查询</el-button>
            <el-button icon="Refresh" class="!rounded-lg px-5" @click="resetQuery" v-hasPermi="['system:menu:query']">重置</el-button>
          </el-form-item>
        </el-form>
        <div class="flex gap-2">
          <el-button type="primary" icon="Plus" class="!rounded-lg !bg-[#1a73e8] border-none px-5" @click="handleAdd" v-hasPermi="['system:menu:add']">新增菜单</el-button>
          <el-tooltip content="展开/折叠" placement="top" effect="dark">
            <el-button icon="Sort" class="!rounded-lg" plain @click="toggleExpandAll" />
          </el-tooltip>
        </div>
      </div>
    </el-card>

    <!-- 树形数据表格 -->
    <el-card shadow="never" class="!border-none rounded-[24px] shadow-sm overflow-hidden p-2">
      <el-table
        v-if="refreshTable"
        v-loading="loading"
        :data="menuList"
        row-key="id"
        :default-expand-all="isExpandAll"
        :indent="24"
        class="google-table-flat"
        height="calc(100vh - 220px)"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-icon v-if="row.icon" class="text-gray-500"><component :is="row.icon" /></el-icon>
              <span class="font-medium text-[#1f1f1f]">{{ row.menuName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.menuType === 1" size="small" class="!rounded-md border-none" effect="light">目录</el-tag>
            <el-tag v-else-if="row.menuType === 2" size="small" type="success" class="!rounded-md border-none" effect="light">菜单</el-tag>
            <el-tag v-else size="small" type="info" class="!rounded-md border-none" effect="light">按钮</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="menuCode" label="权限标识" min-width="160">
          <template #default="{ row }">
            <span v-if="row.menuCode" class="px-2 py-0.5 bg-gray-100 text-gray-500 rounded font-mono text-xs">
              {{ row.menuCode }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="component" label="组件路径" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-gray-400 text-xs">{{ row.component }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="可见" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.visible === 1 ? 'primary' : 'info'" size="small" effect="plain" class="!border-none">
              {{ row.visible === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-1.5">
              <span :class="['w-1.5 h-1.5 rounded-full', row.status === 1 ? 'bg-green-500' : 'bg-gray-300']"></span>
              <span :class="['text-xs', row.status === 1 ? 'text-green-600 font-medium' : 'text-gray-400']">
                {{ row.status === 1 ? '正常' : '停用' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="160" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-1">
              <el-tooltip v-if="checkPermi(['system:menu:add'])" content="新增下级" placement="top" effect="dark" :show-after="200">
                <el-button 
                  link 
                  icon="Plus" 
                  class="!p-2 hover:bg-gray-100 rounded-full transition-colors text-gray-600" 
                  @click="handleAdd(row)"
                />
              </el-tooltip>
              <el-tooltip v-if="checkPermi(['system:menu:edit'])" content="修改菜单" placement="top" effect="dark" :show-after="200">
                <el-button 
                  link 
                  icon="Edit" 
                  class="!p-2 hover:bg-gray-100 rounded-full transition-colors text-gray-600" 
                  @click="handleUpdate(row)"
                />
              </el-tooltip>
              <el-tooltip v-if="checkPermi(['system:menu:delete'])" content="删除菜单" placement="top" effect="dark" :show-after="200">
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
      </el-table>
    </el-card>

    <!-- 弹窗部分保持一致 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" append-to-body class="google-dialog">
      <el-form ref="menuFormRef" :model="menuForm" :rules="rules" label-width="100px" class="py-4 px-2">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="上级菜单">
              <el-tree-select
                v-model="menuForm.parentId"
                :data="menuOptions"
                :props="{ label: 'menuName', value: 'id', children: 'children' }"
                value-key="id"
                placeholder="选择上级菜单"
                check-strictly
                class="w-full google-input-flat"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="菜单类型" prop="menuType">
              <el-radio-group v-model="menuForm.menuType" class="google-radio-group">
                <el-radio-button :value="1">目录</el-radio-button>
                <el-radio-button :value="2">菜单</el-radio-button>
                <el-radio-button :value="3">按钮</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="menuForm.menuType !== 3">
            <el-form-item label="菜单图标" prop="icon">
              <el-input v-model="menuForm.icon" placeholder="图标名称" class="google-input-flat">
                <template #prefix>
                  <el-icon v-if="menuForm.icon"><component :is="menuForm.icon" /></el-icon>
                  <el-icon v-else><Search /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="menuForm.menuName" placeholder="请输入内容" class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="sortOrder">
              <el-input-number v-model="menuForm.sortOrder" controls-position="right" :min="0" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="menuForm.menuType !== 3">
            <el-form-item label="路由地址" prop="path">
              <el-input v-model="menuForm.path" placeholder="请输入内容" class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="menuForm.menuType === 2">
            <el-form-item label="组件路径" prop="component">
              <el-input v-model="menuForm.component" placeholder="请输入内容" class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="menuForm.menuType !== 3">
            <el-form-item label="权限标识" prop="menuCode">
              <el-input v-model="menuForm.menuCode" placeholder="权限标识 (如: system:user:list)" class="google-input-flat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示状态">
              <el-radio-group v-model="menuForm.visible">
                <el-radio :value="1">显示</el-radio>
                <el-radio :value="0">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单状态">
              <el-radio-group v-model="menuForm.status">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue'
import { 
  OfficeBuilding, Search, Folder, UserFilled, Plus, Edit, Delete, Refresh, Sort, 
  Setting, User, Operation, Menu as MenuIcon
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMenu, addMenu, updateMenu, deleteMenu, type Menu } from '@/api/system/menu'
import { checkPermi } from '@/utils/permission'

// --- 状态与参数 ---
const loading = ref(false)
const menuList = ref<Menu[]>([])
const menuOptions = ref<any[]>([])
const isExpandAll = ref(false)
const refreshTable = ref(true)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const menuFormRef = ref()

const queryParams = reactive({
  menuName: '',
  status: undefined
})

const menuForm = reactive<Menu>({
  id: undefined,
  parentId: 0,
  menuName: '',
  icon: '',
  menuType: 1,
  sortOrder: 0,
  path: '',
  component: '',
  menuCode: '',
  visible: 1,
  status: 1
})

const rules = {
  menuName: [{ required: true, message: '菜单名称不能为空', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '菜单顺序不能为空', trigger: 'blur' }],
  path: [{ required: true, message: '路由地址不能为空', trigger: 'blur' }]
}

// --- 方法 ---

/** 查询菜单列表 */
const getList = async () => {
  loading.value = true
  try {
    const { data } = await listMenu(queryParams)
    menuList.value = data
  } catch (error) {
    console.error('获取菜单列表失败:', error)
  } finally {
    loading.value = false
  }
}

/** 查询菜单下拉树结构 */
const getTreeSelect = async () => {
  try {
    const { data } = await listMenu()
    const menu: any = { id: 0, menuName: '主类目', children: data }
    menuOptions.value = [menu]
  } catch (error) {
    console.error('获取菜单树失败:', error)
  }
}

const handleQuery = () => {
  getList()
}

const resetQuery = () => {
  queryParams.menuName = ''
  queryParams.status = undefined
  handleQuery()
}

const toggleExpandAll = () => {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  nextTick(() => {
    refreshTable.value = true
  })
}

const handleAdd = (row?: any) => {
  resetForm()
  getTreeSelect()
  if (row != null && row.id) {
    menuForm.parentId = row.id
  } else {
    menuForm.parentId = 0
  }
  dialogTitle.value = '添加菜单'
  dialogVisible.value = true
}

const handleUpdate = async (row: any) => {
  resetForm()
  await getTreeSelect()
  Object.assign(menuForm, row)
  dialogTitle.value = '修改菜单'
  dialogVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`是否确认删除 "${row.menuName}" ?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteMenu(row.id)
      ElMessage.success('删除成功')
      getList()
    } catch (error) {
      console.error('删除失败:', error)
    }
  })
}

const resetForm = () => {
  Object.assign(menuForm, {
    id: undefined,
    parentId: 0,
    menuName: '',
    icon: '',
    menuType: 1,
    sortOrder: 0,
    path: '',
    component: '',
    menuCode: '',
    visible: 1,
    status: 1
  })
  if (menuFormRef.value) {
    menuFormRef.value.resetFields()
  }
}

const submitForm = () => {
  menuFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (menuForm.id !== undefined) {
          await updateMenu(menuForm)
          ElMessage.success('修改成功')
        } else {
          await addMenu(menuForm)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        getList()
      } catch (error) {
        console.error('提交失败:', error)
      }
    }
  })
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
/* 扁平化 Google 表格 */
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

:deep(.el-table__row td) {
  border-bottom: 1px solid #f1f3f4 !important;
}

/* 输入框扁平化规格 */
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

:deep(.google-radio-group .el-radio-button__inner) {
  border-radius: 20px !important;
  margin-right: 8px;
  border: 1px solid #dcdfe6 !important;
  box-shadow: none !important;
}

:deep(.google-radio-group .el-radio-button:first-child .el-radio-button__inner) {
  border-left: 1px solid #dcdfe6 !important;
}

:deep(.google-radio-group .el-radio-button.is-active .el-radio-button__inner) {
  background-color: #1a73e8 !important;
  border-color: #1a73e8 !important;
}
</style>

<style>
/* 全局弹窗样式优化 */
.google-dialog {
  border-radius: 24px !important;
  box-shadow: 0 12px 32px 4px rgba(0,0,0,0.1) !important;
}
.google-dialog .el-dialog__title {
  font-size: 22px;
  font-weight: 600;
}

/* Tooltip 风格 */
.el-popper.is-dark {
  background: #3c4043 !important;
  border: none !important;
}
</style>
