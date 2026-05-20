<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import ProjectCard from '../components/home/ProjectCard.vue'
import { getProjectPageApi, type ProjectPageItem, type ProjectPageParams } from '@/api/house'
import { Search, ArrowRight, MapLocation, PriceTag, House, RefreshLeft, Sort } from '@element-plus/icons-vue'
import groupData from '@/assets/group.json'

const searchQuery = ref('')
const selectedArea = ref('全部')
const sortBy = ref<'default' | 'price' | 'area'>('default')

// 根据 Navbar 选中的城市动态获取区域列表
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
  // 如果选的是省份/直辖市本身，取其下所有区
  const city = province.value === currentCity.value
    ? province.children?.[0]
    : province.children?.find(c => c.value === currentCity.value)
  if (!city?.children) return ['全部']
  return ['全部', ...city.children.map(d => d.label)]
})

// API 数据
const projects = ref<ProjectPageItem[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)

// 从 Navbar 城市选择器获取当前城市（每次 fetch 时实时读取）
function getCity() {
  const city = localStorage.getItem('resms-selected-city') || '广州市'
  return city === '全国' ? undefined : city
}

async function fetchProjects() {
  loading.value = true
  try {
    const params: ProjectPageParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }

    const city = getCity()
    if (city) params.city = city
    if (selectedArea.value !== '全部') params.district = selectedArea.value
    if (searchQuery.value) params.projectName = searchQuery.value

    const res = await getProjectPageApi(params)
    projects.value = res.records
    total.value = res.total
  } catch (e) {
    console.error('获取楼盘列表失败', e)
  } finally {
    loading.value = false
  }
}

// 前端排序（后端暂不支持楼盘排序字段）
const sortedProjects = computed(() => {
  const list = [...projects.value]
  if (sortBy.value === 'price') {
    list.sort((a, b) => (a.avgPrice ?? Infinity) - (b.avgPrice ?? Infinity))
  } else if (sortBy.value === 'area') {
    list.sort((a, b) => (b.maxArea ?? 0) - (a.maxArea ?? 0))
  }
  return list
})

// 传递给 ProjectCard 的数据
const displayProjects = computed(() =>
  sortedProjects.value.map(p => ({
    id: p.id,
    title: p.projectName,
    avgPrice: p.avgPrice ? String(p.avgPrice) : '待定',
    location: `${p.city} ${p.district}`,
    tags: p.tags || [],
    image: p.coverUrl || '',
    layoutSummary: p.layoutSummary || '多种户型',
    areaRange: p.minArea && p.maxArea
      ? (p.minArea === p.maxArea ? `${p.minArea}` : `${p.minArea}-${p.maxArea}`)
      : '多种面积',
    houseCount: p.houseCount || 0,
  }))
)

function resetFilters() {
  selectedArea.value = '全部'
  searchQuery.value = ''
}

function handlePageChange(page: number) {
  pageNum.value = page
  fetchProjects()
}

// 搜索防抖
const debouncedFetch = useDebounceFn(() => {
  pageNum.value = 1
  fetchProjects()
}, 400)

// 监听城市变化（通过 localStorage 轮询）
const cityPoll = ref(currentCity.value)
let cityTimer: ReturnType<typeof setInterval>
onMounted(() => {
  cityTimer = setInterval(() => {
    const c = localStorage.getItem('resms-selected-city') || '广州市'
    if (c !== cityPoll.value) {
      cityPoll.value = c
      selectedArea.value = '全部'
      pageNum.value = 1
      fetchProjects()
    }
  }, 500)
})
onUnmounted(() => clearInterval(cityTimer))

watch(selectedArea, () => {
  pageNum.value = 1
  fetchProjects()
})

watch(searchQuery, () => debouncedFetch())

onMounted(() => {
  fetchProjects()
})
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
          <el-breadcrumb-item>新房楼盘</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="flex flex-col lg:flex-row justify-between items-end gap-8">
          <div class="flex-1">
            <h1 class="text-4xl md:text-5xl font-black text-slate-900 mb-4 tracking-tighter">
              发现理想 <span class="text-primary italic">居所</span>
            </h1>
            <p class="text-slate-500 text-lg max-w-2xl font-medium leading-relaxed">
              为您精选最优质的新开发楼盘，从核心地段到宜居新城，开启您的家园新篇章。
              目前共有 <span class="text-slate-900 font-bold border-b-2 border-primary/30">{{ total.toLocaleString()
                }}</span> 个活跃项目在售。
            </p>
          </div>

          <div class="w-full lg:w-[480px]">
            <div class="group relative">
              <el-input v-model="searchQuery" placeholder="搜索楼盘、开发商或地段" size="large" class="house-search-input">
                <template #prefix>
                  <el-icon class="text-primary text-xl">
                    <Search />
                  </el-icon>
                </template>
              </el-input>
              <div
                class="absolute inset-0 rounded-2xl bg-primary/5 scale-105 opacity-0 group-hover:opacity-100 transition-all -z-10 blur-xl">
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Content Area -->
    <div class="max-w-7xl mx-auto px-6 mt-8 flex flex-col gap-8">
      <!-- Horizontal Filters Bar -->
      <section class="bg-white rounded-[32px] shadow-sm border border-slate-100 p-8">
        <div class="flex flex-col md:flex-row md:items-center gap-4 md:gap-8">
          <h3 class="text-xs font-black text-slate-300 uppercase tracking-widest flex items-center gap-2 min-w-[100px]">
            <el-icon class="text-primary">
              <MapLocation />
            </el-icon>
            区域选择
          </h3>
          <div class="flex flex-wrap gap-2 flex-1">
            <button v-for="area in areaList" :key="area" @click="selectedArea = area" :class="[
              'px-4 py-1.5 rounded-full text-xs font-bold transition-all border active:scale-95',
              selectedArea === area
                ? 'bg-primary text-white border-primary shadow-md shadow-primary/20'
                : 'bg-slate-50 text-slate-500 border-transparent hover:bg-slate-100'
            ]">
              {{ area }}
            </button>
          </div>
          <button @click="resetFilters"
            class="text-xs text-slate-400 hover:text-primary flex items-center gap-1 transition-colors group">
            <el-icon class="group-hover:rotate-180 transition-transform duration-500">
              <RefreshLeft />
            </el-icon>
            重置筛选
          </button>
        </div>
      </section>

      <!-- Results Area -->
      <div class="flex flex-col gap-8">
        <!-- Sort & View Controls -->
        <div
          class="bg-white p-2 rounded-2xl shadow-sm border border-slate-100 flex justify-between items-center pr-2 pl-6">
          <div class="flex gap-4 items-center overflow-x-auto no-scrollbar">
            <span class="text-xs font-black text-slate-300 uppercase tracking-tighter mr-2 flex items-center gap-1">
              <el-icon>
                <Sort />
              </el-icon> 排序
            </span>
            <div class="flex p-1 bg-slate-50 rounded-xl">
              <button
                v-for="opt in [{ key: 'default', label: '默认排序' }, { key: 'price', label: '低单价' }, { key: 'area', label: '面积大' }]"
                :key="opt.key" @click="sortBy = opt.key as typeof sortBy" :class="[
                  'px-4 py-1.5 rounded-lg text-xs font-bold transition-all',
                  sortBy === opt.key ? 'bg-white text-primary shadow-sm' : 'text-slate-400 hover:text-slate-600'
                ]">{{ opt.label }}</button>
            </div>
          </div>
          <div class="flex gap-1 bg-slate-50 p-1 rounded-xl">
            <button class="w-8 h-8 rounded-lg flex items-center justify-center bg-white text-primary shadow-sm">
              <el-icon>
                <Menu />
              </el-icon>
            </button>
            <button class="w-8 h-8 rounded-lg flex items-center justify-center text-slate-400 hover:text-slate-600">
              <el-icon>
                <List />
              </el-icon>
            </button>
          </div>
        </div>

        <!-- Grid -->
        <div v-loading="loading" class="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-4 gap-8">
          <ProjectCard v-for="project in displayProjects" :key="project.id" v-bind="project" />
          <div v-if="!loading && displayProjects.length === 0" class="col-span-full text-center py-20 text-gray-400">
            暂无符合条件的楼盘
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="total > pageSize" class="mt-8 flex justify-center">
          <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize"
            :current-page="pageNum" @current-change="handlePageChange" class="!rounded-2xl" />
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

:deep(.el-radio__label),
:deep(.el-checkbox__label) {
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
