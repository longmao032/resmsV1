<template>
  <div class="favorite-management-container p-6 bg-gray-50 min-h-full">
    <div class="flex flex-col h-full bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <!-- 头部 -->
      <div class="p-8 border-b border-gray-50 bg-gradient-to-r from-[#1a73e8]/5 to-transparent flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold text-[#202124]">客户收藏关注</h2>
          <p class="text-sm text-gray-500 mt-1">分析客户对不同房源的关注热度，精准把握市场动向</p>
        </div>
      </div>

      <!-- 搜索 -->
      <div class="px-6 pt-4 pb-2">
        <el-form :inline="true" class="flex items-center gap-4">
          <el-form-item label="房源名称" class="!mb-0">
            <el-input v-model="queryParams.houseTitle" placeholder="搜索房源..." clearable class="google-input-flat !w-56"
              @keyup.enter="handleSearch" />
          </el-form-item>
          <el-button type="primary" icon="Search" class="!rounded-lg !bg-[#1a73e8] border-none px-5" @click="handleSearch">搜索</el-button>
          <el-button icon="Refresh" class="!rounded-lg px-5" @click="resetSearch">重置</el-button>
        </el-form>
      </div>

      <!-- 网格列表 -->
      <el-scrollbar class="flex-1 px-6">
        <el-row :gutter="24">
          <el-col :span="8" v-for="item in favoriteList" :key="item.id" class="mb-6">
            <el-card shadow="hover" class="!border-none !rounded-2xl overflow-hidden group hover:shadow-xl transition-all duration-300">
              <div class="relative h-48 overflow-hidden">
                <el-image :src="item.cover" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500">
                  <template #error>
                    <div class="w-full h-full flex items-center justify-center bg-gray-100 text-gray-300">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div class="absolute top-4 right-4">
                  <el-tag v-if="item.isHot" type="danger" effect="dark" size="small" class="!rounded-full border-none px-3 shadow-md">热门关注</el-tag>
                </div>
                <div class="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/60 to-transparent">
                  <div class="text-white font-bold text-lg truncate">{{ item.houseTitle }}</div>
                </div>
              </div>

              <div class="p-5">
                <div class="flex justify-between items-center mb-4">
                  <span v-if="item.houseType === 3" class="text-2xl font-bold text-[#34a853] font-mono">{{ formatPrice(item.price) }}<span class="text-sm font-normal text-gray-500">元/月</span></span>
                  <span v-else class="text-2xl font-bold text-red-500 font-mono">{{ formatPrice(item.price) }}<span class="text-sm font-normal text-gray-500">万</span></span>
                  <div class="flex items-center gap-1 text-gray-400">
                    <el-icon><StarFilled /></el-icon>
                    <span class="text-xs font-bold">{{ item.favCount }} 人收藏</span>
                  </div>
                </div>

                <div class="flex gap-2 mb-4">
                  <el-tag size="small" class="!bg-gray-100 !border-none !text-gray-600 !rounded-md">{{ item.layout || '—' }}</el-tag>
                  <el-tag v-if="item.area" size="small" class="!bg-gray-100 !border-none !text-gray-600 !rounded-md">{{ item.area }}㎡</el-tag>
                </div>

                <div class="pt-4 border-t border-gray-50 flex justify-between items-center text-xs">
                  <div class="text-gray-400">
                    最后收藏: <span class="text-gray-600">{{ item.lastFav }}</span>
                  </div>
                  <el-button v-hasPermi="['trade:favorite:query']" type="primary" link icon="ArrowRight" @click="handleViewPortrait(item)">查看画像</el-button>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col v-if="!loading && favoriteList.length === 0" :span="24">
            <div class="text-center py-20 text-gray-300">
              <el-icon :size="48"><Folder /></el-icon>
              <p class="mt-3 text-sm">暂无收藏数据</p>
            </div>
          </el-col>
        </el-row>
      </el-scrollbar>

      <!-- 分页 -->
      <div class="p-6 border-t border-gray-50 flex justify-between items-center bg-gray-50/50">
        <div class="text-sm text-gray-500">共 {{ total }} 条收藏热度记录</div>
        <el-pagination background layout="prev, pager, next"
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          @current-change="getList"
          class="google-pagination" />
      </div>
    </div>

    <!-- 收藏人群画像抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="selectedHouse ? `收藏画像 - ${selectedHouse.houseTitle}` : '收藏画像'"
      size="560px"
      class="google-drawer"
      destroy-on-close
    >
      <div class="p-2 h-full flex flex-col">
        <template v-if="selectedHouse">
          <!-- 统计看板 -->
          <div class="grid grid-cols-2 gap-4 mb-6">
            <div class="bg-blue-50/50 p-4 rounded-2xl border border-blue-100">
              <div class="text-xs text-blue-600 font-bold mb-1">收藏总人数</div>
              <div class="text-2xl font-bold text-blue-700">{{ fanList.length }}</div>
            </div>
            <div class="bg-purple-50/50 p-4 rounded-2xl border border-purple-100">
              <div class="text-xs text-purple-600 font-bold mb-1">最后收藏</div>
              <div class="text-sm font-bold text-purple-700 mt-1">{{ fanList.length > 0 ? fanList[0].favoriteTime : '—' }}</div>
            </div>
          </div>

          <!-- 收藏客户列表 -->
          <div class="flex items-center gap-2 mb-4">
            <div class="w-1 h-4 bg-[#1a73e8] rounded-full"></div>
            <span class="font-bold text-gray-800">关注客户列表 ({{ fanList.length }})</span>
          </div>

          <div class="flex-1 overflow-hidden">
            <el-scrollbar class="h-full">
              <div class="space-y-3 pr-2">
                <div v-for="fan in fanList" :key="fan.id" class="p-4 rounded-2xl border border-gray-100 hover:bg-gray-50 transition-colors flex items-center gap-3">
                  <el-avatar :size="40" class="!bg-blue-100 !text-blue-600 font-bold">{{ (fan.nickname || '?').charAt(0) }}</el-avatar>
                  <div class="min-w-0 flex-1">
                    <div class="font-bold text-gray-800 truncate">{{ fan.nickname || '匿名用户' }}</div>
                    <div class="text-xs text-gray-400 mt-0.5">{{ fan.favoriteTime }}收藏</div>
                  </div>
                </div>
              </div>
            </el-scrollbar>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Search, Refresh, StarFilled, ArrowRight, User, Picture, Folder } from '@element-plus/icons-vue'
import { listFavorite, listFavoriteFans } from '@/api/trade/favorite'

// --- 状态管理 ---
const loading = ref(false)
const favoriteList = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 12,
  houseTitle: ''
})

const drawerVisible = ref(false)
const selectedHouse = ref<any | null>(null)
const fanList = ref<any[]>([])

// 获取房源收藏热度榜
const getList = async () => {
  loading.value = true
  try {
    const response: any = await listFavorite(queryParams)
    favoriteList.value = response.data.records
    total.value = response.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  getList()
}

const resetSearch = () => {
  queryParams.houseTitle = ''
  handleSearch()
}

// 格式化价格：去掉多余的小数尾数
const formatPrice = (val: number | string) => {
  if (val == null) return '—'
  const n = Number(val)
  return Number.isInteger(n) ? n.toString() : n.toFixed(2).replace(/\.?0+$/, '')
}

// 查看收藏粉丝画像
const handleViewPortrait = async (item: any) => {
  selectedHouse.value = item
  drawerVisible.value = true
  fanList.value = []
  try {
    const response: any = await listFavoriteFans(item.id)
    fanList.value = response.data || []
  } catch {
    fanList.value = []
  }
}

// 初始加载
getList()
</script>

<style scoped>
.favorite-management-container {
  height: calc(100vh - 110px);
}

/* 分页器 */
:deep(.google-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #1a73e8 !important;
}

/* Drawer 样式 */
:deep(.google-drawer) {
  border-radius: 28px 0 0 28px;
}

:deep(.google-drawer .el-drawer__header) {
  padding: 24px 32px 16px;
  margin-bottom: 0;
  border-bottom: 1px solid #f1f3f4;
}

:deep(.google-drawer .el-drawer__title) {
  font-size: 20px;
  font-weight: bold;
  color: #202124;
}

:deep(.google-drawer .el-drawer__body) {
  padding: 24px 32px;
}

:deep(.el-timeline-item__tail) {
  border-left: 2px dashed #e8eaed;
}
</style>
