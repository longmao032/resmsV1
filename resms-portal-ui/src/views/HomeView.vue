<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Hero from '../components/home/Hero.vue'
import HouseCard from '../components/home/HouseCard.vue'
import { getHousePageApi } from '@/api/house'
import type { HousePageItem } from '@/api/house'

const featuredHouses = ref<HousePageItem[]>([])
const loading = ref(true)

/** 将分转换为万元显示 */
function formatPrice(item: HousePageItem): string {
  if (item.price) return String(item.price)
  if (item.totalPriceFen) return String(Math.round(item.totalPriceFen / 1000000))
  if (item.rentPriceFen) return String(Math.round(item.rentPriceFen / 100))
  return '-'
}

/** 拼接地址 */
function formatLocation(item: HousePageItem): string {
  return [item.city, item.district].filter(Boolean).join(' ')
}

onMounted(async () => {
  try {
    const res = await getHousePageApi({ pageNum: 1, pageSize: 8 })
    featuredHouses.value = res.records || []
  } catch {
    // 静默失败，展示空列表
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="min-h-screen">
    <!-- Hero Section -->
    <Hero />

    <!-- Featured Section -->
    <section class="max-w-7xl mx-auto px-6 py-24">
      <div class="flex flex-col md:flex-row justify-between items-end mb-12 gap-6">
        <div>
          <h2 class="text-gray-900 text-4xl font-bold mb-4 tracking-tight">精选优质房源</h2>
          <p class="text-gray-500 text-lg max-w-xl font-light">
            我们为您从成千上万个房源中挑选出最具性价比和设计感的楼盘，开启您的理想生活。
          </p>
        </div>
        <el-button link class="!text-primary !text-lg !font-bold flex items-center gap-2 group">
          查看全部房源
          <el-icon class="group-hover:translate-x-1 transition-transform"><ArrowRight /></el-icon>
        </el-button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        <div v-for="i in 4" :key="i" class="bg-white rounded-2xl overflow-hidden shadow-md border border-gray-100 animate-pulse">
          <div class="aspect-[4/3] bg-gray-200"></div>
          <div class="p-5 space-y-3">
            <div class="h-5 bg-gray-200 rounded w-3/4"></div>
            <div class="h-4 bg-gray-200 rounded w-1/2"></div>
            <div class="h-4 bg-gray-200 rounded w-full"></div>
          </div>
        </div>
      </div>

      <!-- Grid -->
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        <HouseCard
          v-for="house in featuredHouses"
          :key="house.id"
          :id="house.id"
          :title="house.projectName"
          :price="formatPrice(house)"
          :location="formatLocation(house)"
          :type="house.layout"
          :area="String(house.area)"
          :tags="house.tags || []"
          :image="house.coverUrl || ''"
          :house-type="house.houseType"
        />
      </div>
    </section>

    <!-- Services Section -->
    <section class="bg-gray-50 py-24 border-y border-gray-100">
      <div class="max-w-7xl mx-auto px-6 text-center">
        <h2 class="text-gray-900 text-4xl font-bold mb-16 tracking-tight">我们的核心服务</h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-12">
          <div class="flex flex-col items-center group">
            <div class="w-20 h-20 bg-white rounded-3xl shadow-lg flex items-center justify-center mb-6 group-hover:scale-110 group-hover:bg-primary transition-all duration-300">
              <el-icon :size="36" class="text-primary group-hover:text-white"><Service /></el-icon>
            </div>
            <h3 class="text-xl font-bold mb-4">全流程顾问服务</h3>
            <p class="text-gray-500 leading-relaxed">
              从选房、看房到合同签署，我们的专家团队全程为您提供一对一专业指导。
            </p>
          </div>
          <div class="flex flex-col items-center group">
            <div class="w-20 h-20 bg-white rounded-3xl shadow-lg flex items-center justify-center mb-6 group-hover:scale-110 group-hover:bg-primary transition-all duration-300">
              <el-icon :size="36" class="text-primary group-hover:text-white"><Magnet /></el-icon>
            </div>
            <h3 class="text-xl font-bold mb-4">精准算法推荐</h3>
            <p class="text-gray-500 leading-relaxed">
              基于大数据和深度学习，为您实时匹配最符合需求的理想居所。
            </p>
          </div>
          <div class="flex flex-col items-center group">
            <div class="w-20 h-20 bg-white rounded-3xl shadow-lg flex items-center justify-center mb-6 group-hover:scale-110 group-hover:bg-primary transition-all duration-300">
              <el-icon :size="36" class="text-primary group-hover:text-white"><Suitcase /></el-icon>
            </div>
            <h3 class="text-xl font-bold mb-4">一站式权证办理</h3>
            <p class="text-gray-500 leading-relaxed">
              代办过户、贷款、公积金提取等繁杂流程，让您省心省力，快速收房。
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer Simple -->
    <footer class="bg-gray-900 text-white py-20">
      <div class="max-w-7xl mx-auto px-6 grid grid-cols-1 md:grid-cols-4 gap-12 border-b border-white/10 pb-16">
        <div class="col-span-2">
          <div class="flex items-center gap-2 mb-8">
            <div class="w-10 h-10 bg-primary rounded-xl flex items-center justify-center shadow-lg">
              <el-icon :size="24" color="white"><House /></el-icon>
            </div>
            <span class="text-2xl font-bold tracking-tight">RESMS <span class="font-light opacity-80">Portal</span></span>
          </div>
          <p class="text-gray-400 max-w-sm mb-8 leading-relaxed">
            RESMS 房产销售系统是行业领先的数字化交易平台，致力于通过技术创新提升房地产交易效率和用户体验。
          </p>
          <div class="flex gap-4">
          </div>
        </div>
        <div>
          <h4 class="font-bold mb-6 text-lg">快速链接</h4>
          <ul class="text-gray-400 space-y-4">
            <li><a href="#" class="hover:text-primary transition-colors">关于我们</a></li>
            <li><a href="#" class="hover:text-primary transition-colors">加入我们</a></li>
            <li><a href="#" class="hover:text-primary transition-colors">联系我们</a></li>
            <li><a href="#" class="hover:text-primary transition-colors">免责声明</a></li>
          </ul>
        </div>
        <div>
          <h4 class="font-bold mb-6 text-lg">关注我们</h4>
          <div class="flex flex-col gap-4">
             <div class="flex items-center gap-4 text-gray-400">
               <div class="w-10 h-10 bg-white/5 rounded-full flex items-center justify-center">
                 <el-icon><Phone /></el-icon>
               </div>
               <span>400-123-4567</span>
             </div>
             <div class="flex items-center gap-4 text-gray-400">
               <div class="w-10 h-10 bg-white/5 rounded-full flex items-center justify-center">
                 <el-icon><Message /></el-icon>
               </div>
               <span>support@resms.com</span>
             </div>
          </div>
        </div>
      </div>
      <div class="max-w-7xl mx-auto px-6 pt-8 text-gray-500 text-sm text-center">
        © 2026 RESMS Real Estate Management System. All rights reserved.
      </div>
    </footer>
  </main>
</template>
