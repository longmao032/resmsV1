<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import HouseCard from '../components/home/HouseCard.vue'
import { getHousePageApi, type HousePageItem, type HousePageParams } from '@/api/house'
import { Search, ArrowRight, MapLocation, PriceTag, House, RefreshLeft, Sort, Link, Menu, List } from '@element-plus/icons-vue'
import groupData from '@/assets/group.json'

const searchQuery = ref('')
const selectedPrice = ref('')
const selectedMethod = ref('全部')
const selectedType = ref<string[]>([])
const selectedArea = ref('全部')
const sortBy = ref<'default' | 'price' | 'area'>('default')

const filters = {
  methods: ['全部', '整租', '合租'],
  prices: ['2000元以下', '2000-4000元', '4000-6000元', '6000-8000元', '8000-12000元', '12000元以上'],
  types: ['一室', '二室', '三室', '四室及以上']
}

const currentCity = computed(() => {
  const city = localStorage.getItem('resms-selected-city') || '广州市'
  return city === '全国' ? '' : city
})

const areaList = computed(() => {
  if (!currentCity.value) return ['全部']
  const province = groupData.data.find(p =>
    p.value === currentCity.value || p.children?.some(c => c.value === currentCity.value)
  )
  if (!province) return ['全部']
  const city = province.value === currentCity.value
    ? province.children?.[0]
    : province.children?.find(c => c.value === currentCity.value)
  if (!city?.children) return ['全部']
  return ['全部', ...city.children.map(d => d.label)]
})

// 租金筛选 → API 参数 (元/月 → 分/月)
const priceRanges: Record<string, { min?: number; max?: number }> = {
  '2000元以下': { max: 200_000 },
  '2000-4000元': { min: 200_000, max: 400_000 },
  '4000-6000元': { min: 400_000, max: 600_000 },
  '6000-8000元': { min: 600_000, max: 800_000 },
  '8000-12000元': { min: 800_000, max: 1_200_000 },
  '12000元以上': { min: 1_200_000 },
}

const typeMap: Record<string, string> = {
  '一室': '1室', '二室': '2室', '三室': '3室', '四室及以上': '4室',
}

const houses = ref<HousePageItem[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)

function getCity() {
  const city = localStorage.getItem('resms-selected-city') || '广州市'
  return city === '全国' ? undefined : city
}

async function fetchHouses() {
  loading.value = true
  try {
    const params: HousePageParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      houseType: 3,
    }
    const city = getCity()
    if (city) params.city = city
    if (selectedArea.value !== '全部') params.district = selectedArea.value
    if (selectedPrice.value && priceRanges[selectedPrice.value]) {
      const range = priceRanges[selectedPrice.value]
      if (range.min) params.minRentPriceFen = range.min
      if (range.max) params.maxRentPriceFen = range.max
    }
    if (selectedType.value.length > 0) {
      params.layout = typeMap[selectedType.value[0]] || selectedType.value[0]
    }
    const res = await getHousePageApi(params)
    houses.value = res.records
    total.value = res.total
  } catch (e) {
    console.error('获取租房列表失败', e)
  } finally {
    loading.value = false
  }
}

// 前端排序
const sortedHouses = computed(() => {
  const list = [...houses.value]
  if (sortBy.value === 'price') {
    list.sort((a, b) => a.price - b.price)
  } else if (sortBy.value === 'area') {
    list.sort((a, b) => b.area - a.area)
  }
  return list
})

const displayHouses = computed(() =>
  sortedHouses.value.map(h => ({
    id: h.id,
    title: h.projectName,
    price: String(h.price),
    priceUnit: '元/月',
    location: `${h.city} ${h.district}`,
    type: h.layout,
    area: String(h.area),
    tags: h.tags || [],
    image: h.coverUrl || '',
    houseType: h.houseType,
  }))
)

function resetFilters() {
  selectedArea.value = '全部'
  selectedMethod.value = '全部'
  selectedPrice.value = ''
  selectedType.value = []
  searchQuery.value = ''
}

function handlePageChange(page: number) {
  pageNum.value = page
  fetchHouses()
}

// 搜索防抖
const debouncedFetch = useDebounceFn(() => {
  pageNum.value = 1
  fetchHouses()
}, 400)

// 监听城市变化
const cityPoll = ref(currentCity.value)
let cityTimer: ReturnType<typeof setInterval>
onMounted(() => {
  fetchHouses()
  cityTimer = setInterval(() => {
    const c = localStorage.getItem('resms-selected-city') || '广州市'
    if (c !== cityPoll.value) {
      cityPoll.value = c
      selectedArea.value = '全部'
      pageNum.value = 1
      fetchHouses()
    }
  }, 500)
})
onUnmounted(() => clearInterval(cityTimer))

watch([selectedArea, selectedPrice, selectedType], () => {
  pageNum.value = 1
  fetchHouses()
})

watch(searchQuery, () => debouncedFetch())
</script>

<template>
  <div class="pb-20 bg-slate-50/50 min-h-screen">
    <!-- Hero Header -->
    <div class="relative bg-white pt-24 pb-16 border-b border-slate-100 overflow-hidden">
      <div class="absolute top-0 right-0 -mt-24 -mr-24 w-96 h-96 bg-primary/5 rounded-full blur-3xl"></div>
      <div class="absolute bottom-0 left-0 -mb-24 -ml-24 w-72 h-72 bg-blue-400/5 rounded-full blur-3xl"></div>
      
      <div class="max-w-7xl mx-auto px-6 relative z-10">
        <el-breadcrumb :separator-icon="ArrowRight" class="mb-8">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>优质租房</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="flex flex-col lg:flex-row justify-between items-end gap-8">
          <div class="flex-1">
            <h1 class="text-4xl md:text-5xl font-black text-slate-900 mb-4 tracking-tighter">
              租出生活 <span class="text-primary italic">品质</span>
            </h1>
            <p class="text-slate-500 text-lg max-w-2xl font-medium leading-relaxed">
              为您精选全城优质租赁住房，真实房源，租后有保障。
              目前共有 <span class="text-slate-900 font-bold border-b-2 border-primary/30">{{ total.toLocaleString() }}</span> 套活跃房源。
            </p>
          </div>
          
          <div class="w-full lg:w-[480px]">
            <div class="group relative">
              <el-input
                v-model="searchQuery"
                placeholder="搜索区域、地铁站或小区"
                size="large"
                class="house-search-input"
              >
                <template #prefix>
                  <el-icon class="text-primary text-xl"><Search /></el-icon>
                </template>
              </el-input>
              <div class="absolute inset-0 rounded-2xl bg-primary/5 scale-105 opacity-0 group-hover:opacity-100 transition-all -z-10 blur-xl"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Content Area -->
    <div class="max-w-7xl mx-auto px-6 mt-8 flex flex-col gap-8">
      <!-- Horizontal Filters Bar -->
      <section class="bg-white rounded-[32px] shadow-sm border border-slate-100 p-8 space-y-6">
        <!-- Rental Method Row -->
        <div class="flex flex-col md:flex-row md:items-center gap-4 md:gap-8">
          <h3 class="text-xs font-black text-slate-300 uppercase tracking-widest flex items-center gap-2 min-w-[100px]">
            <el-icon class="text-primary"><Link /></el-icon>
            租赁方式
          </h3>
          <div class="flex flex-wrap gap-2 flex-1">
            <button
              v-for="method in filters.methods"
              :key="method"
              @click="selectedMethod = method"
              :class="[
                'px-4 py-1.5 rounded-full text-xs font-bold transition-all border active:scale-95',
                selectedMethod === method 
                  ? 'bg-primary text-white border-primary shadow-md shadow-primary/20' 
                  : 'bg-slate-50 text-slate-500 border-transparent hover:bg-slate-100'
              ]"
            >
              {{ method }}
            </button>
          </div>
          <button 
            @click="resetFilters"
            class="text-xs text-slate-400 hover:text-primary flex items-center gap-1 transition-colors group"
          >
            <el-icon class="group-hover:rotate-180 transition-transform duration-500"><RefreshLeft /></el-icon>
            重置筛选
          </button>
        </div>

        <div class="h-px bg-slate-50 w-full"></div>

        <!-- Area Row -->
        <div class="flex flex-col md:flex-row md:items-center gap-4 md:gap-8">
          <h3 class="text-xs font-black text-slate-300 uppercase tracking-widest flex items-center gap-2 min-w-[100px]">
            <el-icon class="text-primary"><MapLocation /></el-icon>
            区域选择
          </h3>
          <div class="flex flex-wrap gap-2 flex-1">
            <button
              v-for="area in areaList"
              :key="area"
              @click="selectedArea = area"
              :class="[
                'px-4 py-1.5 rounded-full text-xs font-bold transition-all border active:scale-95',
                selectedArea === area 
                  ? 'bg-primary text-white border-primary shadow-md shadow-primary/20' 
                  : 'bg-slate-50 text-slate-500 border-transparent hover:bg-slate-100'
              ]"
            >
              {{ area }}
            </button>
          </div>
        </div>

        <div class="h-px bg-slate-50 w-full"></div>

        <!-- Price Row -->
        <div class="flex flex-col md:flex-row md:items-center gap-4 md:gap-8">
          <h3 class="text-xs font-black text-slate-300 uppercase tracking-widest flex items-center gap-2 min-w-[100px]">
            <el-icon class="text-primary"><PriceTag /></el-icon>
            租金范围
          </h3>
          <div class="flex-1">
            <el-radio-group v-model="selectedPrice" class="horizontal-filter-group">
              <el-radio v-for="p in filters.prices" :key="p" :label="p" size="small">
                {{ p }}
              </el-radio>
            </el-radio-group>
          </div>
        </div>

        <div class="h-px bg-slate-50 w-full"></div>

        <!-- Type Row -->
        <div class="flex flex-col md:flex-row md:items-center gap-4 md:gap-8">
          <h3 class="text-xs font-black text-slate-300 uppercase tracking-widest flex items-center gap-2 min-w-[100px]">
            <el-icon class="text-primary"><House /></el-icon>
            户型要求
          </h3>
          <div class="flex-1">
            <el-checkbox-group v-model="selectedType" class="horizontal-filter-group">
              <el-checkbox v-for="t in filters.types" :key="t" :label="t" size="small">
                {{ t }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
      </section>

      <!-- Results Area -->
      <div class="flex flex-col gap-8">
        <!-- Sort & Controls -->
        <div class="bg-white p-2 rounded-2xl shadow-sm border border-slate-100 flex justify-between items-center pr-2 pl-6">
          <div class="flex gap-4 items-center overflow-x-auto no-scrollbar">
            <span class="text-xs font-black text-slate-300 uppercase tracking-tighter mr-2 flex items-center gap-1">
              <el-icon><Sort /></el-icon> 排序
            </span>
            <div class="flex p-1 bg-slate-50 rounded-xl">
              <button
                v-for="opt in [{ key: 'default', label: '默认排序' }, { key: 'price', label: '租金低' }, { key: 'area', label: '面积大' }]"
                :key="opt.key"
                @click="sortBy = opt.key as typeof sortBy"
                :class="[
                  'px-4 py-1.5 rounded-lg text-xs font-bold transition-all',
                  sortBy === opt.key ? 'bg-white text-primary shadow-sm' : 'text-slate-400 hover:text-slate-600'
                ]"
              >{{ opt.label }}</button>
            </div>
          </div>
          <div class="flex items-center gap-1 bg-slate-50 p-1 rounded-xl">
            <button class="w-8 h-8 rounded-lg flex items-center justify-center bg-white text-primary shadow-sm">
              <el-icon><Menu /></el-icon>
            </button>
            <button class="w-8 h-8 rounded-lg flex items-center justify-center text-slate-400 hover:text-slate-600">
              <el-icon><List /></el-icon>
            </button>
          </div>
        </div>

        <!-- Grid -->
        <div v-loading="loading" class="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-4 gap-8">
          <HouseCard
            v-for="house in displayHouses"
            :key="house.id"
            v-bind="house"
          />
          <div v-if="!loading && displayHouses.length === 0" class="col-span-full text-center py-20 text-gray-400">
            暂无符合条件的房源
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="total > pageSize" class="mt-8 flex justify-center">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="pageNum"
            @current-change="handlePageChange"
            class="!rounded-2xl"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@reference "@/assets/main.css";

.house-search-input :deep(.el-input__wrapper) {
  @apply !rounded-2xl !border-none !shadow-2xl !px-6 !py-4 transition-all duration-300;
  background: white;
}

.house-search-input :deep(.el-input__wrapper.is-focus) {
  @apply ring-4 ring-primary/10;
}

.horizontal-filter-group {
  @apply !flex !flex-wrap !gap-x-8 !gap-y-3 !w-full;
}

:deep(.horizontal-filter-group .el-radio), 
:deep(.horizontal-filter-group .el-checkbox) {
  @apply !mr-0;
}

:deep(.el-radio__label), :deep(.el-checkbox__label) {
  @apply !text-slate-600 !font-bold !text-xs;
}

:deep(.el-radio__input.is-checked + .el-radio__label), 
:deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  @apply !text-primary;
}

/* Pagination Customization */
:deep(.el-pagination.is-background .el-pager li:not(.is-active)) {
  @apply !bg-white !rounded-xl !border !border-slate-100 !text-slate-400 !font-bold hover:!text-primary transition-colors;
}

:deep(.el-pagination.is-background .el-pager li.is-active) {
  @apply !bg-primary !rounded-xl !font-bold !shadow-md shadow-primary/20;
}

:deep(.el-pagination.is-background .btn-prev), 
:deep(.el-pagination.is-background .btn-next) {
  @apply !bg-white !rounded-xl !border !border-slate-100 !text-slate-400 hover:!text-primary transition-colors;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
