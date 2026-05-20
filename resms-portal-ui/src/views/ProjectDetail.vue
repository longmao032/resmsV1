<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import HouseCard from '../components/home/HouseCard.vue'
import { getProjectDetailApi, getHousePageApi, type ProjectDetailItem, type HousePageItem } from '@/api/house'
import { addBrowseHistoryApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useInteractionTracker } from '@/composables/useInteractionTracker'
import { ArrowRight, Location, OfficeBuilding, Timer, Grid, House as HouseIcon, Loading } from '@element-plus/icons-vue'

const route = useRoute()
const projectIdRef = computed(() => Number(route.params.id))
const projectId = projectIdRef.value // For existing logic compatible with Number

const authStore = useAuthStore()

// 启用时长追踪
useInteractionTracker(projectIdRef, 2, authStore.isLoggedIn)
const project = ref<ProjectDetailItem | null>(null)
const houses = ref<HousePageItem[]>([])
const loading = ref(true)
const housesLoading = ref(false)

async function fetchProject() {
  loading.value = true
  try {
    project.value = await getProjectDetailApi(projectId)
  } catch (e) {
    console.error('获取楼盘详情失败', e)
  } finally {
    loading.value = false
  }
}

async function fetchHouses() {
  housesLoading.value = true
  try {
    const res = await getHousePageApi({ pageNum: 1, pageSize: 50, projectId })
    houses.value = res.records
  } catch (e) {
    console.error('获取房源列表失败', e)
  } finally {
    housesLoading.value = false
  }
}

function formatPrice(h: HousePageItem): string {
  return h.priceUnit === 1 ? String(h.unitPrice) : String(h.price)
}

function formatPriceUnit(h: HousePageItem): string {
  return h.priceUnit === 1 ? '元/㎡' : '万/套'
}

onMounted(() => {
  fetchProject()
  fetchHouses()
})
</script>

<template>
  <div class="pb-20 bg-slate-50/50 min-h-screen">
    <!-- Loading -->
    <div v-if="loading" class="pt-32 flex justify-center">
      <el-icon class="is-loading text-primary" :size="32"><Loading /></el-icon>
    </div>

    <template v-else-if="project">
      <!-- Hero -->
      <div class="relative bg-white pt-24 pb-12 border-b border-slate-100 overflow-hidden">
        <div class="absolute top-0 right-0 -mt-24 -mr-24 w-96 h-96 bg-primary/5 rounded-full blur-3xl"></div>
        <div class="max-w-7xl mx-auto px-6 relative z-10">
          <el-breadcrumb :separator-icon="ArrowRight" class="mb-8">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item :to="{ path: '/new-house' }">新房楼盘</el-breadcrumb-item>
            <el-breadcrumb-item>{{ project.projectName }}</el-breadcrumb-item>
          </el-breadcrumb>

          <div class="flex flex-col lg:flex-row gap-8">
            <!-- Cover Image -->
            <div class="w-full lg:w-[480px] aspect-[4/3] rounded-2xl overflow-hidden shadow-lg flex-shrink-0">
              <img v-if="project.coverUrl" :src="project.coverUrl" :alt="project.projectName" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full bg-slate-100 flex items-center justify-center text-slate-400">暂无图片</div>
            </div>

            <!-- Info -->
            <div class="flex-1 flex flex-col justify-between py-2">
              <div>
                <h1 class="text-3xl md:text-4xl font-black text-slate-900 mb-3">{{ project.projectName }}</h1>
                <div class="flex items-center text-slate-500 text-sm mb-4 gap-1">
                  <el-icon><Location /></el-icon>
                  {{ project.city }} {{ project.district }} {{ project.address }}
                </div>
                <div class="flex flex-wrap gap-2 mb-6">
                  <span v-for="tag in project.tags" :key="tag" class="bg-primary/10 text-primary text-xs px-3 py-1 rounded-full font-bold">{{ tag }}</span>
                </div>
              </div>

              <!-- Stats Grid -->
              <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div class="bg-slate-50 rounded-xl p-4 text-center">
                  <div class="text-2xl font-black text-primary">{{ project.avgPrice ?? '待定' }}</div>
                  <div class="text-xs text-slate-400 mt-1">均价（元/㎡）</div>
                </div>
                <div class="bg-slate-50 rounded-xl p-4 text-center">
                  <div class="text-2xl font-black text-slate-700">{{ project.layoutSummary ?? '-' }}</div>
                  <div class="text-xs text-slate-400 mt-1">主力户型</div>
                </div>
                <div class="bg-slate-50 rounded-xl p-4 text-center">
                  <div class="text-2xl font-black text-slate-700">
                    {{ project.minArea && project.maxArea ? (project.minArea === project.maxArea ? project.minArea : `${project.minArea}-${project.maxArea}`) : '-' }}
                  </div>
                  <div class="text-xs text-slate-400 mt-1">面积（㎡）</div>
                </div>
                <div class="bg-slate-50 rounded-xl p-4 text-center">
                  <div class="text-2xl font-black text-green-600">{{ project.houseCount }}</div>
                  <div class="text-xs text-slate-400 mt-1">在售套数</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Project Details -->
      <div class="max-w-7xl mx-auto px-6 mt-8">
        <section class="bg-white rounded-2xl shadow-sm border border-slate-100 p-8 mb-8">
          <h2 class="text-lg font-bold text-slate-800 mb-6 flex items-center gap-2">
            <el-icon class="text-primary"><OfficeBuilding /></el-icon>
            楼盘信息
          </h2>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-y-4 gap-x-8 text-sm">
            <div><span class="text-slate-400">开发商</span><div class="font-medium text-slate-700 mt-1">{{ project.developer || '-' }}</div></div>
            <div><span class="text-slate-400">物业公司</span><div class="font-medium text-slate-700 mt-1">{{ project.propertyCompany || '-' }}</div></div>
            <div><span class="text-slate-400">总户数</span><div class="font-medium text-slate-700 mt-1">{{ project.totalHouseholds ?? '-' }}</div></div>
            <div><span class="text-slate-400">物业费</span><div class="font-medium text-slate-700 mt-1">{{ project.propertyFee ? `${project.propertyFee}元/㎡/月` : '-' }}</div></div>
            <div><span class="text-slate-400">容积率</span><div class="font-medium text-slate-700 mt-1">{{ project.plotRatio ?? '-' }}</div></div>
            <div><span class="text-slate-400">绿化率</span><div class="font-medium text-slate-700 mt-1">{{ project.greeningRate ? `${project.greeningRate}%` : '-' }}</div></div>
          </div>
        </section>

        <!-- House List -->
        <section class="bg-white rounded-2xl shadow-sm border border-slate-100 p-8">
          <h2 class="text-lg font-bold text-slate-800 mb-6 flex items-center gap-2">
            <el-icon class="text-primary"><HouseIcon /></el-icon>
            在售房源（{{ project.houseCount }} 套）
          </h2>
          <div v-loading="housesLoading" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
            <HouseCard
              v-for="h in houses"
              :key="h.id"
              :id="h.id"
              :title="h.projectName"
              :price="formatPrice(h)"
              :price-unit="formatPriceUnit(h)"
              :location="`${h.city} ${h.district}`"
              :type="h.layout"
              :area="String(h.area)"
              :tags="h.tags || []"
              :image="h.coverUrl || ''"
            />
            <div v-if="!housesLoading && houses.length === 0" class="col-span-full text-center py-12 text-gray-400">
              暂无在售房源
            </div>
          </div>
        </section>
      </div>
    </template>

    <!-- Not Found -->
    <div v-else class="pt-32 text-center text-gray-400">
      楼盘不存在
    </div>
  </div>
</template>
