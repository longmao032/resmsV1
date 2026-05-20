<template>
  <div class="bg-white rounded-2xl p-4 flex flex-col gap-3 min-w-[240px] max-w-[280px] border border-slate-100 text-slate-800 shadow-xs select-none">
    <div class="text-[11px] font-bold text-slate-400 flex items-center gap-1 select-none">
      <span>🏠 咨询房源</span>
    </div>

    <img
      v-if="data.coverUrl"
      :src="data.coverUrl"
      class="w-full h-32 object-cover rounded-xl shadow-xs"
      alt="房源图片"
    />

    <div
      v-else
      class="w-full h-32 bg-slate-100 rounded-xl flex items-center justify-center text-slate-300 text-2xl"
    >
      🏠
    </div>

    <div class="flex flex-col gap-1 text-left">
      <div class="font-extrabold text-[14px] text-slate-900 truncate">
        {{ data.projectName || '未知项目' }}
      </div>

      <div v-if="data.district" class="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5">
        <span>📍</span>
        <span>{{ data.district }}</span>
      </div>

      <div class="text-[12px] text-slate-500 flex items-center gap-1.5 mt-0.5">
        <span class="font-medium text-slate-400">售价/租金：</span>
        <span class="font-black text-rose-650">{{ data.price || '暂无报价' }}</span>
      </div>

      <div class="text-[12px] text-slate-500 flex items-center gap-1.5">
        <span class="font-medium text-slate-400">户型/面积：</span>
        <span class="font-semibold text-slate-700">
          {{ layoutDisplay }}
        </span>
      </div>

      <div v-if="data.sellingPoint" class="text-[11px] text-amber-700 bg-amber-50/60 px-2.5 py-2 rounded-lg mt-1 leading-relaxed flex items-start gap-1">
        <span class="shrink-0 mt-px">💡</span>
        <span>{{ data.sellingPoint }}</span>
      </div>
    </div>

    <a
      v-if="data.houseId"
      :href="`/house/${data.houseId}`"
      target="_blank"
      class="mt-1 text-center text-xs font-black text-indigo-650 hover:text-indigo-750 bg-indigo-50 hover:bg-indigo-100/80 py-2.5 rounded-xl border border-indigo-100/60 transition-all flex items-center justify-center cursor-pointer select-none"
    >
      点击查看房源详情
    </a>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { HouseItem } from '@/api/ai'

const props = defineProps<{
  data: HouseItem
}>()

const layoutDisplay = computed(() => {
  const parts = [props.data.layout, props.data.area ? `${props.data.area}㎡` : '']
  return parts.filter(Boolean).join(' · ') || '暂无户型信息'
})
</script>
