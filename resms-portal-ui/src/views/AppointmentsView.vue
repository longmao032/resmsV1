<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Calendar, Clock, Location, User } from '@element-plus/icons-vue'
import { getAppointmentsApi, cancelAppointmentApi, type AppointmentItem } from '@/api/user'

const router = useRouter()
const appointments = ref<AppointmentItem[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const activeTab = ref(-1) // -1 表示全部

const statusTabs = [
  { label: '全部', value: -1 },
  { label: '待处理', value: 1 },
  { label: '已完成', value: 2 },
  { label: '已取消', value: 3 },
]

const statusMap: Record<number, { label: string; type: string; color: string }> = {
  1: { label: '待处理', type: 'warning', color: '#f59e0b' },
  2: { label: '已完成', type: 'success', color: '#10b981' },
  3: { label: '已取消', type: 'info', color: '#94a3b8' },
}

async function fetchAppointments() {
  loading.value = true
  try {
    const params: Record<string, number> = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (activeTab.value !== -1) params.status = activeTab.value
    const res = await getAppointmentsApi(params)
    appointments.value = res.records
    total.value = res.total
  } catch (e) {
    console.error('获取预约记录失败', e)
  } finally {
    loading.value = false
  }
}

async function handleCancel(item: AppointmentItem) {
  try {
    await ElMessageBox.confirm('确定取消该预约吗？取消后需要重新预约。', '取消预约', {
      confirmButtonText: '确定取消',
      cancelButtonText: '暂不取消',
      type: 'warning'
    })
    await cancelAppointmentApi(item.id)
    ElMessage.success('预约已取消')
    fetchAppointments()
  } catch {
    // 用户取消
  }
}

function canCancel(status: number) {
  return status === 1
}

function handleTabChange(tab: number) {
  activeTab.value = tab
  pageNum.value = 1
  fetchAppointments()
}

function handlePageChange(page: number) {
  pageNum.value = page
  fetchAppointments()
}

onMounted(fetchAppointments)
</script>

<template>
  <div class="min-h-screen bg-slate-50/50 pb-20">
    <!-- 顶部导航 -->
    <div class="relative bg-slate-900 pt-24 pb-20 px-6 overflow-hidden">
      <!-- 背景装饰 -->
      <div class="absolute top-0 left-0 w-full h-full opacity-20 pointer-events-none">
        <div class="absolute -top-24 -left-24 w-96 h-96 bg-blue-500 rounded-full blur-[100px]"></div>
        <div class="absolute -bottom-24 -right-24 w-96 h-96 bg-indigo-500 rounded-full blur-[100px]"></div>
      </div>
      
      <div class="max-w-5xl mx-auto relative z-10">
        <el-breadcrumb :separator-icon="ArrowRight" class="appointment-breadcrumb mb-6">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/profile' }">个人中心</el-breadcrumb-item>
          <el-breadcrumb-item>预约记录</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="flex items-end justify-between">
          <div>
            <h1 class="text-4xl font-black text-white mb-2">预约记录</h1>
            <p class="text-slate-400 font-medium">高效管理您的看房计划，专属顾问随时待命</p>
          </div>
          <div class="hidden md:flex items-center gap-4 text-white/80 text-sm">
            <div class="bg-white/10 backdrop-blur-md px-4 py-2 rounded-2xl border border-white/10">
              <span class="text-slate-400 mr-2">总预约</span>
              <span class="font-black text-white">{{ total }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab 筛选 -->
    <div class="max-w-5xl mx-auto px-6 -mt-10 relative z-20">
      <div class="bg-white/80 backdrop-blur-xl rounded-[2rem] p-2 flex gap-1 shadow-xl shadow-slate-200/50 border border-white/20">
        <div
          v-for="tab in statusTabs"
          :key="tab.value"
          class="flex-1 text-center py-3.5 rounded-2xl text-sm font-bold cursor-pointer transition-all duration-300"
          :class="activeTab === tab.value
            ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/30 scale-[1.02]'
            : 'text-slate-500 hover:bg-slate-50 hover:text-slate-800'"
          @click="handleTabChange(tab.value)"
        >
          {{ tab.label }}
        </div>
      </div>
    </div>

    <!-- 预约列表 -->
    <div class="max-w-5xl mx-auto px-6 mt-6">
      <div v-loading="loading" class="flex flex-col gap-6">
        <div v-if="appointments.length === 0 && !loading" class="text-center py-20 bg-white rounded-[2rem] border border-dashed border-slate-200">
          <el-icon :size="64" class="text-slate-200 mb-4"><Calendar /></el-icon>
          <h3 class="text-xl font-bold text-slate-400">暂无预约记录</h3>
          <p class="text-slate-300 text-sm mt-1">发现心仪好房，开启看房之旅</p>
          <el-button type="primary" class="mt-8 !rounded-xl font-bold h-12 px-8" @click="router.push('/new-house')">去看看房源</el-button>
        </div>

        <div
          v-for="item in appointments"
          :key="item.id"
          class="bg-white rounded-[2rem] shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 p-8 border border-slate-100 relative overflow-hidden group"
        >
          <!-- 状态边框装饰 -->
          <div 
            class="absolute top-0 left-0 w-1.5 h-full transition-all duration-300 group-hover:w-2"
            :style="{ backgroundColor: statusMap[item.status]?.color }"
          ></div>

          <div class="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <!-- 左侧信息 -->
            <div class="flex gap-6 items-start flex-1">
              <div
                class="w-20 h-20 rounded-3xl bg-slate-50 flex items-center justify-center text-blue-600 flex-shrink-0 cursor-pointer hover:bg-blue-50 transition-colors border border-slate-100 shadow-inner"
                @click="router.push(`/house/${item.houseId}`)"
              >
                <el-icon :size="32" class="opacity-80"><Calendar /></el-icon>
              </div>
              
              <div class="flex flex-col gap-2.5 flex-1">
                <div class="flex flex-wrap items-center gap-3">
                  <h3
                    class="text-xl font-black text-slate-900 cursor-pointer hover:text-blue-600 transition-colors"
                    @click="router.push(`/house/${item.houseId}`)"
                  >
                    {{ item.houseTitle }}
                  </h3>
                  <div 
                    class="px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider"
                    :style="{ 
                      backgroundColor: statusMap[item.status]?.color + '15',
                      color: statusMap[item.status]?.color,
                      border: `1px solid ${statusMap[item.status]?.color}30`
                    }"
                  >
                    {{ statusMap[item.status]?.label }}
                  </div>
                </div>

                <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-2 mt-2">
                  <div class="flex items-center gap-2 text-slate-500">
                    <div class="w-8 h-8 rounded-lg bg-slate-50 flex items-center justify-center">
                      <el-icon :size="14" class="text-blue-500"><Clock /></el-icon>
                    </div>
                    <div class="flex flex-col">
                      <span class="text-[10px] font-bold text-slate-400 uppercase tracking-tighter">预约时间</span>
                      <span class="text-sm font-black text-slate-700">{{ item.followDate }}</span>
                    </div>
                  </div>
                  <div class="flex items-center gap-2 text-slate-500">
                    <div class="w-8 h-8 rounded-lg bg-slate-50 flex items-center justify-center">
                      <el-icon :size="14" class="text-blue-500"><User /></el-icon>
                    </div>
                    <div class="flex flex-col">
                      <span class="text-[10px] font-bold text-slate-400 uppercase tracking-tighter">专属顾问</span>
                      <span class="text-sm font-black text-slate-700">{{ item.salesName }}</span>
                    </div>
                  </div>
                </div>

                <div v-if="item.houseAddress" class="flex items-center gap-2 mt-2">
                  <el-icon :size="14" class="text-slate-400"><Location /></el-icon>
                  <span class="text-sm font-medium text-slate-400">{{ item.houseAddress }}</span>
                </div>

                <div v-if="item.content" class="mt-4 p-4 bg-slate-50 rounded-2xl border border-slate-100 relative">
                  <div class="absolute -top-2 left-4 px-2 bg-slate-50 text-[10px] font-bold text-slate-400 uppercase tracking-widest">需求备注</div>
                  <p class="text-sm font-medium text-slate-600 leading-relaxed">{{ item.content }}</p>
                </div>
              </div>
            </div>

            <!-- 右侧操作 -->
            <div class="flex flex-row md:flex-col items-center md:items-end justify-between md:justify-center gap-4 border-t md:border-t-0 pt-4 md:pt-0 mt-2 md:mt-0">
              <div class="text-right">
                <div class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-1">提交于</div>
                <div class="text-sm font-bold text-slate-900">{{ item.createTime.split(' ')[0] }}</div>
              </div>
              <el-button
                v-if="canCancel(item.status)"
                type="danger"
                size="large"
                class="!rounded-2xl !px-8 font-black shadow-lg shadow-red-500/10 hover:shadow-red-500/20 transition-all active:scale-95"
                @click="handleCancel(item)"
              >
                取消预约
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="mt-12 flex justify-center">
        <el-pagination
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          background
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.appointment-breadcrumb :deep(.el-breadcrumb__inner) {
  color: rgba(255, 255, 255, 0.6) !important;
  font-weight: 600;
}
.appointment-breadcrumb :deep(.el-breadcrumb__inner.is-link:hover) {
  color: #fff !important;
}
.appointment-breadcrumb :deep(.el-breadcrumb__separator) {
  color: rgba(255, 255, 255, 0.4) !important;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #2563eb !important;
  border-radius: 8px;
}
</style>
