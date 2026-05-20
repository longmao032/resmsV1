<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Star, StarFilled, House, MapLocation } from '@element-plus/icons-vue'
import { getFavoritesApi, removeFavoriteApi, type FavoriteItem } from '@/api/user'

const router = useRouter()
const favorites = ref<FavoriteItem[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

async function fetchFavorites() {
  loading.value = true
  try {
    const res = await getFavoritesApi({ pageNum: pageNum.value, pageSize: pageSize.value })
    favorites.value = res.records
    total.value = res.total
  } catch (e: any) {
    console.error('获取收藏列表失败', e)
    ElMessage.error(e?.response?.data?.message || e.message || '获取收藏列表失败')
  } finally {
    loading.value = false
  }
}

async function handleRemove(item: FavoriteItem) {
  try {
    await ElMessageBox.confirm('确定取消收藏该房源吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await removeFavoriteApi(item.id)
    ElMessage.success('已取消收藏')
    fetchFavorites()
  } catch {
    // 用户取消
  }
}

function getTypeLabel(type: number) {
  const map: Record<number, string> = { 1: '新房', 2: '二手房', 3: '租房', 4: '楼盘项目' }
  return map[type] || '房源'
}

function getTypeColor(type: number) {
  const map: Record<number, string> = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'danger' }
  return map[type] || 'info'
}

function formatPrice(price: number | null, unit: string) {
  if (price === null || price === undefined) return '待定'
  if (unit === '元/月') return `${price.toLocaleString()}元/月`
  if (unit === '元/套') return `${price}万`
  if (unit === '元/㎡') return `${price.toLocaleString()}元/㎡`
  if (price >= 10000) return `${(price / 10000).toFixed(0)}万`
  return price.toLocaleString()
}

function handlePageChange(page: number) {
  pageNum.value = page
  fetchFavorites()
}

onMounted(fetchFavorites)
</script>

<template>
  <div class="min-h-screen bg-slate-50/50 pb-20">
    <!-- 顶部导航 -->
    <div class="bg-white pt-24 pb-8 px-6 border-b border-slate-100">
      <div class="max-w-5xl mx-auto">
        <el-breadcrumb :separator-icon="ArrowRight" class="mb-4">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/profile' }">个人中心</el-breadcrumb-item>
          <el-breadcrumb-item>我的收藏</el-breadcrumb-item>
        </el-breadcrumb>
        <h1 class="text-2xl font-bold text-slate-800">我的收藏</h1>
        <p class="text-sm text-slate-400 mt-1">共收藏 {{ total }} 套房源</p>
      </div>
    </div>

    <!-- 收藏列表 -->
    <div class="max-w-5xl mx-auto px-6 mt-8">
      <div v-loading="loading" class="flex flex-col gap-4">
        <div v-if="favorites.length === 0 && !loading" class="text-center py-20">
          <el-icon :size="48" class="text-slate-200 mb-4"><Star /></el-icon>
          <p class="text-slate-400">暂无收藏的房源</p>
          <el-button type="primary" class="mt-4" @click="router.push('/new-house')">去看看新房</el-button>
        </div>

        <div
          v-for="item in favorites"
          :key="item.id"
          class="bg-white rounded-2xl shadow-sm hover:shadow-md transition-shadow p-5 flex gap-5 cursor-pointer group"
          @click="item.houseType === 4 ? router.push(`/project/${item.houseId}`) : router.push(`/house/${item.houseId}`)"
        >
          <!-- 封面图 -->
          <div class="w-40 h-28 rounded-xl overflow-hidden bg-slate-100 flex-shrink-0">
            <img v-if="item.coverUrl" :src="item.coverUrl" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-slate-300">
              <el-icon :size="32"><House /></el-icon>
            </div>
          </div>

          <!-- 信息 -->
          <div class="flex-1 flex flex-col justify-between min-w-0">
            <div>
              <div class="flex items-center gap-2 mb-1">
                <h3 class="text-base font-bold text-slate-800 truncate group-hover:text-primary transition-colors">
                  {{ item.projectName }}
                </h3>
                <el-tag :type="getTypeColor(item.houseType)" size="small" effect="plain">
                  {{ getTypeLabel(item.houseType) }}
                </el-tag>
              </div>
              <div class="flex items-center gap-3 text-sm text-slate-400">
                <span class="flex items-center gap-1">
                  <el-icon :size="14"><MapLocation /></el-icon>
                  {{ item.city }} {{ item.district }}
                </span>
                <span>{{ item.layout || '暂无户型' }}</span>
                <span v-if="item.area">{{ item.area }}{{ item.houseType === 4 ? '㎡起' : '㎡' }}</span>
              </div>
            </div>
            <div class="flex items-center justify-between">
              <div class="text-lg font-bold text-red-500">
                {{ formatPrice(item.totalPrice, item.priceUnit) }}
                <span v-if="item.priceUnit === '元/套' && item.unitPrice" class="text-xs font-normal text-slate-400 ml-1">
                  {{ item.unitPrice.toLocaleString() }}元/m²
                </span>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-xs text-slate-300">收藏于 {{ item.favoriteTime.split(' ')[0] }}</span>
                <el-button
                  type="danger"
                  text
                  size="small"
                  @click.stop="handleRemove(item)"
                >
                  <el-icon class="mr-1"><StarFilled /></el-icon>
                  取消收藏
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="mt-8 flex justify-center">
        <el-pagination
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>
