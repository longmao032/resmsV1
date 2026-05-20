<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Location, Star, StarFilled } from '@element-plus/icons-vue'
import { useFavoritesStore } from '@/stores/favorites'
import { computed } from 'vue'

const props = defineProps<{
  id?: number
  title: string
  price: string
  priceUnit?: string
  location: string
  tags: string[]
  image: string
  area: string
  type: string
  houseType?: number // 1-新房 2-二手房 3-租房
}>()

const router = useRouter()
const favoritesStore = useFavoritesStore()

const isFavorited = computed(() => props.id ? favoritesStore.isHouseFavorited(props.id) : false)

function goToDetail() {
  if (props.id) {
    router.push(`/house/${props.id}`)
  }
}

async function handleFavorite(e: Event) {
  e.stopPropagation()
  if (props.id) {
    // 默认作为新房处理，如果没传 houseType
    await favoritesStore.toggleHouseFavorite(props.id, props.houseType || 1)
  }
}
</script>

<template>
  <div
    class="group bg-white rounded-2xl overflow-hidden shadow-md hover:shadow-xl transition-all duration-500 cursor-pointer border border-gray-100 flex flex-col"
    @click="goToDetail"
  >
    <!-- Image Area -->
    <div class="relative aspect-[4/3] overflow-hidden">
      <img
        :src="image"
        class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700"
        :alt="title"
      />
      <!-- Floating Badge -->
      <div class="absolute top-4 left-4 bg-primary text-white text-xs font-bold px-3 py-1.5 rounded-full shadow-lg">
        精选推荐
      </div>
      <!-- Favorite Button -->
      <button
        class="absolute top-4 right-4 w-10 h-10 rounded-full flex items-center justify-center transition-all duration-300 z-10"
        :class="isFavorited ? 'bg-red-500 text-white shadow-lg shadow-red-200' : 'bg-white/60 backdrop-blur-md text-gray-600 hover:bg-white hover:scale-110'"
        @click="handleFavorite"
      >
        <el-icon :size="20">
          <component :is="isFavorited ? StarFilled : Star" />
        </el-icon>
      </button>
      <!-- Price Overlay -->
      <div class="absolute bottom-4 left-4 flex items-baseline gap-1 bg-black/40 backdrop-blur-sm px-3 py-1 rounded-lg text-white">
        <span class="text-xl font-bold">{{ price }}</span>
        <span class="text-xs opacity-80">{{ priceUnit || '万/套' }}</span>
      </div>
    </div>

    <!-- Content Area -->
    <div class="p-5 flex-1 flex flex-col">
      <h3 class="text-lg font-bold text-gray-800 line-clamp-1 group-hover:text-primary transition-colors mb-2">
        {{ title }}
      </h3>

      <div class="flex items-center text-gray-500 text-xs mb-3 gap-1">
        <el-icon><Location /></el-icon>
        <span>{{ location }}</span>
      </div>

      <div class="flex items-center gap-4 text-sm text-gray-600 mb-4 font-medium">
        <span class="flex items-center gap-1">{{ type }}</span>
        <span class="w-1 h-1 bg-gray-300 rounded-full"></span>
        <span>{{ area }}㎡</span>
      </div>

      <div class="flex flex-wrap gap-2 mt-auto">
        <span
          v-for="tag in tags"
          :key="tag"
          class="bg-gray-100 text-gray-600 text-[10px] px-2 py-1 rounded-md font-bold uppercase tracking-wider"
        >
          {{ tag }}
        </span>
      </div>
    </div>
  </div>
</template>
