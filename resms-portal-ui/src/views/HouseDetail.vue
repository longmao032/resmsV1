<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getHouseDetailApi, getHousePageApi, type HouseDetail, type HousePageItem } from '@/api/house'
import { createSessionApi } from '@/api/chat'
import { addBrowseHistoryApi, bookAppointmentApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useInteractionTracker } from '@/composables/useInteractionTracker'
import ChatDialog from '@/components/chat/ChatDialog.vue'
import { ArrowRight, Location, OfficeBuilding, House, FullScreen, Compass, ChatDotRound, Share, Warning, Calendar, Close } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const houseId = computed(() => Number(route.params.id))

// 启用时长追踪 (传入响应式 ID)
useInteractionTracker(houseId, 1, authStore.isLoggedIn)

const detail = ref<HouseDetail | null>(null)
const loading = ref(true)
const loadError = ref(false)
const relatedHouses = ref<HousePageItem[]>([])
const relatedLoading = ref(false)

async function fetchDetail() {
  loading.value = true
  loadError.value = false
  try {
    detail.value = await getHouseDetailApi(houseId.value)
    fetchRelated()
  } catch (e) {
    console.error('获取房源详情失败', e)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function fetchRelated() {
  if (!detail.value) return
  relatedLoading.value = true
  try {
    const res = await getHousePageApi({
      pageNum: 1,
      pageSize: 4,
      houseType: detail.value.house.houseType,
      city: detail.value.house.city,
    })
    relatedHouses.value = res.records.filter(h => h.id !== houseId.value).slice(0, 3)
  } catch (e) {
    console.error('获取相关房源失败', e)
  } finally {
    relatedLoading.value = false
  }
}

function formatRelatedPrice(h: HousePageItem): string {
  return h.priceUnit === 1 ? String(h.unitPrice) : String(h.price)
}

function formatRelatedPriceUnit(h: HousePageItem): string {
  if (h.priceUnit === 2) return '万'
  if (h.priceUnit === 3) return '元/月'
  if (h.priceUnit === 1) return '元/㎡'
  return ''
}

function goToHouse(id: number) {
  router.push(`/house/${id}`)
}

const displayPrice = computed(() => {
  if (!detail.value) return '-'
  return detail.value.price ?? detail.value.house.price
})

const currentHouseContext = computed(() => {
  if (!detail.value) return null
  return {
    id: houseId.value,
    projectName: detail.value.house.projectName,
    coverUrl: detail.value.house.coverUrl,
    layout: detail.value.house.layout,
    area: detail.value.house.area,
    price: displayPrice.value,
    priceUnitText: priceUnitText.value || '',
  }
})

const priceUnitText = computed(() => {
  if (!detail.value) return ''
  const unit = detail.value.priceUnit ?? detail.value.house.priceUnit
  if (unit === 2) return '万'
  if (unit === 3) return '元/月'
  if (unit === 1) return '元/㎡'
  return ''
})

const priceLabel = computed(() => {
  if (!detail.value) return ''
  const unit = detail.value.priceUnit ?? detail.value.house.priceUnit
  if (unit === 2) return '总价'
  if (unit === 3) return '月租'
  if (unit === 1) return '单价'
  return ''
})

const houseTypeLabel: Record<number, string> = { 1: '新房', 2: '二手房', 3: '租房' }

function shareHouse() {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(window.location.href)
    ElMessage.success('链接已复制到剪贴板')
  }
}

// ---- Chat ----
const chatVisible = ref(false)
const chatSessionId = ref<number | null>(null)
const chatTargetName = ref('')

async function openChat() {
  if (!authStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!detail.value?.house.salesId) {
    ElMessage.info('暂无顾问信息')
    return
  }
  try {
    const session = await createSessionApi({
      sessionType: 3, // 3 = 销售与C端客户，确保复用已有会话
      members: [
        { userType: 1, userId: detail.value.house.salesId },
        { userType: 2, userId: authStore.userInfo!.userId },
      ],
    })
    chatSessionId.value = session.id
    chatTargetName.value = detail.value.salesName || detail.value.house.salesName || '专属顾问'
    chatVisible.value = true
  } catch (e) {
    ElMessage.error('创建会话失败')
  }
}

function formatTime(time?: string): string {
  if (!time) return '-'
  const date = new Date(time)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  if (isToday) return `今天 ${hours}:${minutes}`
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}月${day}日 ${hours}:${minutes}`
}

// 处理图片列表，如果没有图片则使用封面图
const allImages = computed(() => {
  if (detail.value?.images && detail.value.images.length > 0) {
    return detail.value.images.map(img => img.url)
  }
  if (detail.value?.house.coverUrl) {
    return [detail.value.house.coverUrl]
  }
  return []
})

// 预约看房相关
const appointmentVisible = ref(false)
const submitting = ref(false)
const appointmentForm = ref({
  viewTime: '',
  remark: ''
})

const openAppointment = () => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再进行预约')
    return
  }
  appointmentVisible.value = true
}

const handleAppointment = async () => {
  if (!appointmentForm.value.viewTime) {
    ElMessage.warning('请选择预约看房时间')
    return
  }
  
  submitting.value = true
  try {
    await bookAppointmentApi({
      houseId: houseId.value,
      viewTime: appointmentForm.value.viewTime,
      remark: appointmentForm.value.remark
    })
    ElMessage.success('预约申请已提交，专属顾问将尽快与您联系')
    appointmentVisible.value = false
    appointmentForm.value = { viewTime: '', remark: '' }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || e.message || '预约失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => fetchDetail())

watch(() => route.params.id, (newId) => {
  if (newId) fetchDetail()
})
</script>

<template>
  <div class="min-h-screen bg-[#f8f9fa] pb-20">
    <!-- Navbar Placeholder -->
    <div class="h-16"></div>

    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-6">
      <!-- Breadcrumb -->
      <el-breadcrumb :separator-icon="ArrowRight" class="mb-6 opacity-70 hover:opacity-100 transition-opacity">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="detail" :to="{ path: detail.house.houseType === 1 ? '/new-house' : detail.house.houseType === 2 ? '/second-hand' : '/rent' }">
          {{ houseTypeLabel[detail.house.houseType] || '房源' }}
        </el-breadcrumb-item>
        <el-breadcrumb-item v-if="detail">{{ detail.house.projectName }}</el-breadcrumb-item>
      </el-breadcrumb>

      <!-- Skeleton Loading -->
      <div v-if="loading" class="space-y-8">
        <el-skeleton animated>
          <template #template>
            <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
              <el-skeleton-item variant="image" class="lg:col-span-2 h-[450px] rounded-3xl" />
              <div class="space-y-4">
                <el-skeleton-item variant="h1" class="w-2/3 h-10" />
                <el-skeleton-item variant="text" class="w-full h-6" />
                <el-skeleton-item variant="rect" class="w-full h-32 rounded-2xl" />
                <el-skeleton-item variant="rect" class="w-full h-48 rounded-2xl" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>

      <template v-else-if="detail">
        <!-- Main Content Grid -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          <!-- Left: Gallery & Info -->
          <div class="lg:col-span-2 space-y-8">
            <!-- Gallery -->
            <div class="relative group">
              <div v-if="allImages.length" class="h-[300px] md:h-[450px] rounded-3xl overflow-hidden shadow-2xl shadow-blue-500/10">
                <el-carousel height="100%" class="h-full w-full bg-slate-100">
                  <el-carousel-item v-for="(img, index) in allImages" :key="index" class="h-full w-full">
                    <el-image 
                      :src="img" 
                      fit="cover" 
                      class="w-full h-full cursor-pointer"
                      :preview-src-list="allImages"
                      :initial-index="index"
                      :preview-teleported="true"
                    />
                  </el-carousel-item>
                </el-carousel>
              </div>
              <!-- No Image Fallback -->
              <div v-else class="h-[450px] bg-slate-100 rounded-3xl flex flex-col items-center justify-center text-slate-400 border-2 border-dashed border-slate-200">
                <el-icon :size="48" class="mb-4"><OfficeBuilding /></el-icon>
                <p class="font-medium">暂无房源图片</p>
              </div>
            </div>

            <!-- Header Info -->
            <div class="bg-white rounded-[2rem] p-8 shadow-sm border border-slate-100">
              <div class="flex flex-wrap items-center gap-3 mb-6">
                <span class="bg-blue-600 text-white text-[10px] uppercase tracking-widest px-2.5 py-1 rounded-lg font-black shadow-lg shadow-blue-500/20">
                  {{ houseTypeLabel[detail.house.houseType] }}
                </span>
                <span v-for="tag in detail.house.tags" :key="tag" class="bg-slate-100 text-slate-600 text-xs px-3 py-1 rounded-full font-bold">
                  {{ tag }}
                </span>
              </div>

              <h1 class="text-3xl md:text-5xl font-black text-slate-900 leading-tight mb-4">
                {{ detail.house.projectName }}
              </h1>

              <div class="flex items-center text-slate-500 gap-6">
                <div class="flex items-center gap-1.5 font-medium">
                  <el-icon class="text-blue-500"><Location /></el-icon>
                  {{ detail.house.city }} · {{ detail.house.district }}
                </div>
                <div class="flex items-center gap-1.5 font-medium">
                  <el-icon class="text-blue-500"><Share /></el-icon>
                  房源编号: {{ detail.house.houseNo || '未分配' }}
                </div>
              </div>

              <div class="grid grid-cols-2 md:grid-cols-4 gap-6 mt-10">
                <div class="space-y-1">
                  <div class="text-slate-400 text-xs font-bold uppercase tracking-wider">售价/租金</div>
                  <div class="flex items-baseline gap-1">
                    <span class="text-3xl font-black text-blue-600">{{ displayPrice }}</span>
                    <span class="text-sm font-bold text-blue-500">{{ priceUnitText }}</span>
                  </div>
                </div>
                <div class="space-y-1">
                  <div class="text-slate-400 text-xs font-bold uppercase tracking-wider">房型</div>
                  <div class="flex items-baseline gap-1">
                    <span class="text-3xl font-black text-slate-800">{{ detail.house.layout }}</span>
                  </div>
                </div>
                <div class="space-y-1">
                  <div class="text-slate-400 text-xs font-bold uppercase tracking-wider">面积</div>
                  <div class="flex items-baseline gap-1">
                    <span class="text-3xl font-black text-slate-800">{{ detail.house.area }}</span>
                    <span class="text-sm font-bold text-slate-500">㎡</span>
                  </div>
                </div>
                <div class="space-y-1">
                  <div class="text-slate-400 text-xs font-bold uppercase tracking-wider">朝向</div>
                  <div class="flex items-baseline gap-1">
                    <span class="text-3xl font-black text-slate-800">{{ detail.house.orientation || '-' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Details Section -->
            <div class="bg-white rounded-[2rem] p-8 shadow-sm border border-slate-100">
              <h2 class="text-xl font-black text-slate-900 mb-8 flex items-center gap-3">
                <div class="w-1.5 h-6 bg-blue-600 rounded-full"></div>
                核心参数
              </h2>
              
              <div class="grid grid-cols-1 md:grid-cols-2 gap-y-6 gap-x-12">
                <div v-for="item in [
                  { label: '小区名称', value: detail.house.projectName, icon: OfficeBuilding },
                  { label: '建筑面积', value: detail.house.area + '㎡', icon: FullScreen },
                  { label: '所在楼层', value: detail.house.floor ? `${detail.house.floor}/${detail.house.totalFloor}层` : '-', icon: House },
                  { label: '房屋朝向', value: detail.house.orientation || '-', icon: Compass },
                  { label: '装修情况', value: detail.house.decoration || '-', icon: House },
                  { label: '楼栋编号', value: detail.house.buildingNo || '-', icon: OfficeBuilding },
                  { label: '单元号', value: detail.house.unitNo || '-', icon: OfficeBuilding },
                  { label: '房号', value: detail.house.roomNo || '-', icon: House },
                ]" :key="item.label" class="flex items-center justify-between py-3 border-b border-slate-50">
                  <div class="flex items-center gap-3">
                    <el-icon class="text-slate-300" :size="18"><component :is="item.icon" /></el-icon>
                    <span class="text-slate-500 font-medium">{{ item.label }}</span>
                  </div>
                  <span class="font-black text-slate-800">{{ item.value }}</span>
                </div>
              </div>
            </div>

            <!-- Description -->
            <div v-if="detail.house.description" class="bg-white rounded-[2rem] p-8 shadow-sm border border-slate-100">
              <h2 class="text-xl font-black text-slate-900 mb-6 flex items-center gap-3">
                <div class="w-1.5 h-6 bg-blue-600 rounded-full"></div>
                房源亮点
              </h2>
              <p class="text-slate-600 leading-relaxed text-lg font-medium whitespace-pre-line">
                {{ detail.house.description }}
              </p>
            </div>

            <!-- Extend Info -->
            <div v-if="detail.newHouseExtend || detail.secondHouseExtend || detail.rentHouseExtend" class="bg-blue-600 rounded-[2rem] p-8 shadow-xl shadow-blue-600/20 text-white">
              <h2 class="text-xl font-black mb-8 flex items-center gap-3">
                <div class="w-1.5 h-6 bg-white/40 rounded-full"></div>
                附加信息
              </h2>
              
              <div class="grid grid-cols-2 md:grid-cols-3 gap-8">
                <template v-if="detail.newHouseExtend">
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">备案价</div>
                    <div class="text-lg font-black">{{ detail.newHouseExtend.recordPrice || '-' }} <span class="text-xs">元/㎡</span></div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">产权年限</div>
                    <div class="text-lg font-black">{{ detail.newHouseExtend.propertyRightYears || '-' }} <span class="text-xs">年</span></div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">交房日期</div>
                    <div class="text-lg font-black">{{ detail.newHouseExtend.estimatedDeliveryDate || '-' }}</div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">交付标准</div>
                    <div class="text-lg font-black">{{ detail.newHouseExtend.deliveryStandard || '-' }}</div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">得房率</div>
                    <div class="text-lg font-black">{{ detail.newHouseExtend.actualAreaRate ? detail.newHouseExtend.actualAreaRate + '%' : '-' }}</div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">梯户比</div>
                    <div class="text-lg font-black">{{ detail.newHouseExtend.elevatorRatio || '-' }}</div>
                  </div>
                </template>
                <template v-if="detail.secondHouseExtend">
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">建筑年代</div>
                    <div class="text-lg font-black">{{ detail.secondHouseExtend.buildYear || '-' }} <span class="text-xs">年</span></div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">房屋用途</div>
                    <div class="text-lg font-black">{{ detail.secondHouseExtend.houseUsage || '-' }}</div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">唯一住房</div>
                    <div class="text-lg font-black">{{ detail.secondHouseExtend.isOnlyHouse === 1 ? '是' : '否' }}</div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">满二</div>
                    <div class="text-lg font-black">{{ detail.secondHouseExtend.isFullTwo === 1 ? '是' : '否' }}</div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">满五唯一</div>
                    <div class="text-lg font-black">{{ detail.secondHouseExtend.isFullFive === 1 ? '是' : '否' }}</div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">抵押状态</div>
                    <div class="text-lg font-black">{{ detail.secondHouseExtend.mortgageStatus === 1 ? '有抵押' : '无抵押' }}</div>
                  </div>
                </template>
                <template v-if="detail.rentHouseExtend">
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">出租方式</div>
                    <div class="text-lg font-black">{{ detail.rentHouseExtend.rentType === 1 ? '整租' : '合租' }}</div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">押金方式</div>
                    <div class="text-lg font-black">{{ detail.rentHouseExtend.depositMethod || '-' }}</div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">押金金额</div>
                    <div class="text-lg font-black">{{ detail.rentHouseExtend.depositAmount || '-' }} <span class="text-xs">元</span></div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">可入住日期</div>
                    <div class="text-lg font-black">{{ detail.rentHouseExtend.checkInDate || '-' }}</div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">最短租期</div>
                    <div class="text-lg font-black">{{ detail.rentHouseExtend.minLeasePeriod ? detail.rentHouseExtend.minLeasePeriod + '个月' : '-' }}</div>
                  </div>
                  <div class="space-y-1">
                    <div class="text-blue-100/60 text-xs font-bold uppercase">支持短租</div>
                    <div class="text-lg font-black">{{ detail.rentHouseExtend.supportShortRent === 1 ? '是' : '否' }}</div>
                  </div>
                </template>
              </div>
            </div>
          </div>

          <!-- Right: Sidebar -->
          <div class="space-y-6">
            <!-- Contact Card -->
            <div class="sticky top-24 bg-white rounded-[2rem] p-8 shadow-xl shadow-slate-200/50 border border-slate-100">
              <div class="text-center mb-8">
                <div class="inline-flex items-center justify-center w-20 h-20 bg-blue-50 rounded-3xl mb-4 relative">
                  <el-avatar v-if="detail.salesAvatar" :src="detail.salesAvatar" class="!w-full !h-full rounded-3xl shadow-sm bg-blue-50" />
                  <div v-else class="text-3xl font-black text-blue-600">{{ (detail.salesName || detail.house.salesName || '管').charAt(0) }}</div>
                  <div class="absolute -bottom-1 -right-1 w-6 h-6 bg-green-500 border-4 border-white rounded-full"></div>
                </div>
                <h3 class="text-xl font-black text-slate-900">{{ detail.salesName || detail.house.salesName || '专属顾问' }}</h3>
                <p class="text-slate-400 text-sm font-medium mt-1">竭诚为您提供专业找房服务</p>
              </div>

              <div class="space-y-4">
                <button
                  class="w-full bg-blue-600 hover:bg-blue-700 text-white font-black py-4 rounded-2xl flex items-center justify-center gap-3 transition-all active:scale-95 shadow-lg shadow-blue-500/25"
                  @click="openChat"
                >
                  <el-icon><ChatDotRound /></el-icon>
                  立即咨询
                </button>
                <button
                  class="w-full bg-indigo-50 text-indigo-600 hover:bg-indigo-100 font-black py-4 rounded-2xl flex items-center justify-center gap-3 transition-all active:scale-95"
                  @click="openAppointment"
                >
                  <el-icon><Calendar /></el-icon>
                  预约看房
                </button>
                <button
                  class="w-full bg-slate-900 hover:bg-slate-800 text-white font-black py-4 rounded-2xl flex items-center justify-center gap-3 transition-all active:scale-95"
                  @click="shareHouse"
                >
                  <el-icon><Share /></el-icon>
                  分享房源
                </button>
              </div>

              <div class="mt-8 pt-8 border-t border-slate-50">
                <div class="flex items-center justify-between text-sm mb-4">
                  <span class="text-slate-400 font-medium">房源状态</span>
                  <span :class="detail.house.status === 1 ? 'text-green-600' : 'text-slate-500'" class="font-black">
                    {{ detail.house.status === 1 ? '在售' : detail.house.status === 2 ? '已预订' : detail.house.status === 3 ? '已成交' : '下架' }}
                  </span>
                </div>
                <div class="flex items-center justify-between text-sm mb-4">
                  <span class="text-slate-400 font-medium">房源编号</span>
                  <span class="text-slate-900 font-black">{{ detail.house.houseNo || '未分配' }}</span>
                </div>
                <div class="flex items-center justify-between text-sm">
                  <span class="text-slate-400 font-medium">更新时间</span>
                  <span class="text-slate-900 font-black">{{ formatTime(detail.house.updateTime) }}</span>
                </div>
              </div>
            </div>

            <!-- Safety Tips -->
            <div class="bg-amber-50 rounded-2xl p-6 border border-amber-100">
              <div class="flex gap-3">
                <el-icon class="text-amber-500 mt-1" :size="20"><Warning /></el-icon>
                <div>
                  <h4 class="text-amber-900 font-black text-sm mb-1">温馨提示</h4>
                  <p class="text-amber-700/80 text-xs leading-relaxed">
                    请实地考察房源信息，确认权属及相关证明后再签署合同。如有疑问请及时联系。
                  </p>
                </div>
              </div>
            </div>
          </div>

        </div>
      </template>

      <!-- Error State -->
      <div v-else-if="loadError" class="py-40 text-center">
        <div class="text-6xl mb-6">⚠️</div>
        <h2 class="text-2xl font-black text-slate-900 mb-2">加载失败</h2>
        <p class="text-slate-400 font-medium mb-8">网络异常或服务器错误，请稍后重试</p>
        <button @click="fetchDetail" class="inline-flex items-center gap-2 bg-blue-600 text-white font-black px-6 py-3 rounded-2xl hover:bg-blue-700 transition-colors">
          重新加载
        </button>
      </div>

      <!-- Not Found State -->
      <div v-else class="py-40 text-center">
        <div class="text-6xl mb-6">🏜️</div>
        <h2 class="text-2xl font-black text-slate-900 mb-2">房源已失效</h2>
        <p class="text-slate-400 font-medium mb-8">该房源可能已下架或暂未公开，看看其他优质房源吧</p>
        <router-link to="/" class="inline-flex items-center gap-2 text-blue-600 font-black hover:gap-3 transition-all">
          返回首页 <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>

      <!-- Related Houses -->
      <div v-if="detail && relatedHouses.length > 0" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-12">
        <h2 class="text-xl font-black text-slate-900 mb-6 flex items-center gap-3">
          <div class="w-1.5 h-6 bg-blue-600 rounded-full"></div>
          相关房源推荐
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div
            v-for="h in relatedHouses"
            :key="h.id"
            class="bg-white rounded-2xl overflow-hidden shadow-sm border border-slate-100 hover:shadow-lg transition-all cursor-pointer group"
            @click="goToHouse(h.id)"
          >
            <div class="aspect-[4/3] overflow-hidden bg-slate-100">
              <img
                v-if="h.coverUrl"
                :src="h.coverUrl"
                :alt="h.projectName"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              />
              <div v-else class="w-full h-full flex items-center justify-center text-slate-300">
                <el-icon :size="48"><OfficeBuilding /></el-icon>
              </div>
            </div>
            <div class="p-5">
              <h3 class="font-black text-slate-800 line-clamp-1 group-hover:text-blue-600 transition-colors mb-2">
                {{ h.projectName }}
              </h3>
              <div class="flex items-center text-slate-400 text-xs mb-3 gap-1">
                <el-icon><Location /></el-icon>
                {{ h.city }} · {{ h.district }}
              </div>
              <div class="flex items-center justify-between">
                <div class="flex items-baseline gap-1">
                  <span class="text-xl font-black text-blue-600">{{ formatRelatedPrice(h) }}</span>
                  <span class="text-xs font-bold text-blue-500">{{ formatRelatedPriceUnit(h) }}</span>
                </div>
                <span class="text-xs text-slate-400 font-medium">{{ h.layout }} · {{ h.area }}㎡</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Chat Dialog -->
    <ChatDialog
      v-model:visible="chatVisible"
      :session-id="chatSessionId"
      :target-name="chatTargetName"
      :target-user-id="detail?.house.salesId"
      :house-context="currentHouseContext"
    />
    <!-- Appointment Dialog -->
    <el-dialog
      v-model="appointmentVisible"
      width="440px"
      class="appointment-premium-dialog"
      :show-close="false"
      align-center
    >
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center text-white shadow-lg shadow-blue-500/30">
              <el-icon :size="20"><Calendar /></el-icon>
            </div>
            <div>
              <h3 class="text-lg font-black text-slate-900">预约看房</h3>
              <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Schedule a Viewing</p>
            </div>
          </div>
          <button @click="appointmentVisible = false" class="w-8 h-8 rounded-full hover:bg-slate-100 flex items-center justify-center text-slate-400 transition-colors">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </template>

      <div class="space-y-6 pt-2">
        <!-- House Mini Preview -->
        <div v-if="detail" class="bg-slate-50 rounded-2xl p-4 flex gap-4 border border-slate-100">
          <el-image :src="allImages[0]" fit="cover" class="w-20 h-20 rounded-xl shadow-sm flex-shrink-0" />
          <div class="flex flex-col justify-center min-w-0">
            <h4 class="font-black text-slate-900 truncate">{{ detail.house.projectName }}</h4>
            <p class="text-xs font-bold text-slate-400 mt-0.5">{{ detail.house.layout }} · {{ detail.house.area }}㎡</p>
            <div class="flex items-baseline gap-1 mt-1">
              <span class="text-sm font-black text-blue-600">{{ displayPrice }}</span>
              <span class="text-[10px] font-bold text-blue-500">{{ priceUnitText }}</span>
            </div>
          </div>
        </div>

        <div class="space-y-4">
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <label class="text-sm font-black text-slate-700">预约时间</label>
              <span class="text-[10px] font-bold text-blue-500 bg-blue-50 px-2 py-0.5 rounded-full">必填项</span>
            </div>
            <el-date-picker
              v-model="appointmentForm.viewTime"
              type="datetime"
              placeholder="请选择看房日期和时间"
              class="!w-full appointment-picker"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </div>

          <div class="space-y-2">
            <label class="text-sm font-black text-slate-700">备注需求</label>
            <el-input
              v-model="appointmentForm.remark"
              type="textarea"
              placeholder="您可以填写具体的需求，如：想看高层、周末下午方便等"
              :rows="3"
              class="appointment-input"
            />
          </div>
        </div>

        <div class="bg-indigo-50/50 rounded-2xl p-4 border border-indigo-100/50">
          <div class="flex gap-3">
            <el-icon class="text-indigo-500 mt-0.5"><Warning /></el-icon>
            <p class="text-xs text-indigo-700/80 font-medium leading-relaxed">
              提交后，您的专属顾问 <span class="font-black text-indigo-700">{{ detail?.salesName || detail?.house.salesName || '置业顾问' }}</span> 将在 1 小时内与您电话确认。
            </p>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex gap-3 pt-2 pb-2">
          <el-button 
            type="primary" 
            :loading="submitting" 
            @click="handleAppointment" 
            class="w-full !h-14 !rounded-2xl font-black text-base shadow-xl shadow-blue-500/30 hover:shadow-blue-500/40 transition-all active:scale-[0.98]"
          >
            确认提交预约
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
:deep(.el-breadcrumb__inner.is-link) {
  font-weight: 700;
  color: inherit;
}

:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--el-text-color-primary);
  font-weight: 800;
}

/* 隐藏滚动条但保留滚动功能 */
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.appointment-premium-dialog :deep(.el-dialog) {
  border-radius: 2.5rem;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
}

.appointment-premium-dialog :deep(.el-dialog__header) {
  padding: 2rem 2rem 1.5rem;
  margin-right: 0;
  border-bottom: 1px solid #f1f5f9;
}

.appointment-premium-dialog :deep(.el-dialog__body) {
  padding: 2rem;
}

.appointment-premium-dialog :deep(.el-dialog__footer) {
  padding: 0 2rem 2rem;
  border-top: none;
}

.appointment-picker :deep(.el-input__wrapper) {
  border-radius: 1rem;
  height: 3.5rem;
  box-shadow: 0 0 0 1px #e2e8f0 inset !important;
  transition: all 0.3s;
}

.appointment-picker :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #2563eb inset !important;
}

.appointment-input :deep(.el-textarea__inner) {
  border-radius: 1rem;
  padding: 1rem;
  box-shadow: 0 0 0 1px #e2e8f0 inset !important;
  border: none;
  transition: all 0.3s;
}

.appointment-input :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px #2563eb inset !important;
}
</style>
