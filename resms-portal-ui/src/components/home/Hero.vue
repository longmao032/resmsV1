<script setup lang="ts">
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

const searchQuery = ref('')
const selectedType = ref('buy')

const houseTypes = [
  { label: '买房', value: 'buy' },
  { label: '租房', value: 'rent' }
]
</script>

<template>
  <div class="relative h-[85vh] w-full overflow-hidden flex items-center justify-center">
    <!-- Background Image with Overlay -->
    <div class="absolute inset-0 z-0">
      <img
        src="@/assets/hero-banner.png"
        class="w-full h-full object-cover scale-105"
        alt="Hero Banner"
      />
      <div class="absolute inset-0 bg-black/40 backdrop-brightness-75"></div>
    </div>

    <!-- Content -->
    <div class="relative z-10 w-full max-w-5xl px-6 flex flex-col items-center text-center">
      <h1 class="text-white text-5xl md:text-7xl font-bold mb-10 tracking-tight animate-fade-in-up">
        发现理想的 <span class="text-accent underline decoration-4 underline-offset-8">家</span>
      </h1>
      <p class="text-white/90 text-xl md:text-2xl mb-16 max-w-3xl font-light leading-relaxed">
        RESMS 为您提供数百万套优质房源，精准搜索，真实信息，让您的选房之旅更轻松。
      </p>

      <!-- Search Box -->
      <div class="w-full bg-white/95 backdrop-blur-2xl p-3 rounded-2xl md:rounded-[3rem] shadow-2xl flex flex-col md:flex-row gap-3">
        <!-- Tabs -->
        <div class="flex p-1 bg-gray-100 rounded-xl md:rounded-[2rem] self-start md:self-center">
          <button
            v-for="type in houseTypes"
            :key="type.value"
            @click="selectedType = type.value"
            :class="[
              'px-6 py-2 rounded-lg md:rounded-[1.8rem] text-sm font-bold transition-all',
              selectedType === type.value ? 'bg-white text-primary shadow-sm' : 'text-gray-500 hover:text-primary'
            ]"
          >
            {{ type.label }}
          </button>
        </div>

        <!-- Search Input -->
        <div class="flex-1 flex items-center px-4">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="请输入区域、楼盘名或商圈"
            class="w-full bg-transparent border-none outline-none text-gray-800 placeholder:text-gray-400 py-4 text-lg font-medium"
          />
        </div>

        <!-- Action Button -->
        <button
          class="bg-primary hover:bg-primary/90 text-white px-10 py-4 rounded-xl md:rounded-[2rem] font-bold text-lg flex items-center justify-center gap-2 transition-all shadow-lg active:scale-95"
        >
          <el-icon :size="20"><Search /></el-icon>
          开始搜索
        </button>
      </div>
      
      <!-- Stats / Quick Links -->
      <div class="mt-12 flex gap-8 text-white/90 font-medium">
        <div class="flex flex-col items-center">
          <span class="text-2xl font-bold">50,000+</span>
          <span class="text-xs opacity-70 uppercase tracking-widest">在售房源</span>
        </div>
        <div class="w-px h-10 bg-white/20 self-center"></div>
        <div class="flex flex-col items-center">
          <span class="text-2xl font-bold">12,000+</span>
          <span class="text-xs opacity-70 uppercase tracking-widest">认证顾问</span>
        </div>
        <div class="w-px h-10 bg-white/20 self-center"></div>
        <div class="flex flex-col items-center">
          <span class="text-2xl font-bold">100%</span>
          <span class="text-xs opacity-70 uppercase tracking-widest">真实信息</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in-up {
  animation: fade-in-up 0.8s ease-out forwards;
}
</style>
