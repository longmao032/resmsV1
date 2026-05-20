<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowRight, Star, ChatDotRound, Phone, Message, InfoFilled } from '@element-plus/icons-vue'

const router = useRouter()

const features = [
  { icon: Star, label: '海量房源', desc: '覆盖新房、二手房、租房全品类' },
  { icon: ChatDotRound, label: '在线沟通', desc: '与置业顾问即时聊天' },
  { icon: Phone, label: '一键预约', desc: '在线预约实地看房' },
  { icon: Message, label: '专业服务', desc: '专业经纪人一对一服务' },
]

const version = '1.0.0'
const appName = 'xM房产'
</script>

<template>
  <div class="min-h-screen bg-slate-50/50 pb-20">
    <!-- 顶部导航 -->
    <div class="bg-gradient-to-br from-primary to-blue-600 pt-24 pb-20 px-6">
      <div class="max-w-3xl mx-auto">
        <el-breadcrumb :separator-icon="ArrowRight" class="mb-4">
          <el-breadcrumb-item :to="{ path: '/' }" class="!text-white/70">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/profile' }" class="!text-white/70">个人中心</el-breadcrumb-item>
          <el-breadcrumb-item class="!text-white/90">关于我们</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="text-center pt-6">
          <div class="w-20 h-20 rounded-2xl bg-white/20 backdrop-blur-md flex items-center justify-center mx-auto mb-4">
            <span class="text-3xl font-black text-white">xM</span>
          </div>
          <h1 class="text-2xl font-bold text-white">{{ appName }}</h1>
          <p class="text-sm text-white/70 mt-1">版本 {{ version }}</p>
        </div>
      </div>
    </div>

    <!-- 平台介绍 -->
    <div class="max-w-3xl mx-auto px-6 -mt-10">
      <div class="bg-white rounded-[32px] shadow-lg p-6 mb-6">
        <h2 class="text-lg font-bold text-slate-800 mb-4">平台简介</h2>
        <p class="text-sm text-slate-500 leading-7">
          xM房产致力于为用户提供真实、优质的房源信息服务。平台汇集了新房、二手房、租房等多种房源类型，
          覆盖广州、深圳等核心城市，通过线上线下相结合的服务模式，为用户提供全方位的购房、租房体验。
        </p>
      </div>

      <!-- 平台特色 -->
      <div class="bg-white rounded-[32px] shadow-lg p-6 mb-6">
        <h2 class="text-lg font-bold text-slate-800 mb-4">平台特色</h2>
        <div class="grid grid-cols-2 gap-4">
          <div
            v-for="f in features"
            :key="f.label"
            class="flex flex-col items-center gap-3 p-4 rounded-2xl bg-slate-50"
          >
            <div class="w-10 h-10 rounded-xl bg-primary/5 flex items-center justify-center text-primary">
              <el-icon :size="20"><component :is="f.icon" /></el-icon>
            </div>
            <div class="text-center">
              <div class="text-sm font-bold text-slate-700">{{ f.label }}</div>
              <div class="text-xs text-slate-400 mt-1">{{ f.desc }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 联系我们 -->
      <div class="bg-white rounded-[32px] shadow-lg p-6 mb-6">
        <h2 class="text-lg font-bold text-slate-800 mb-4">联系我们</h2>
        <div class="space-y-4 text-sm">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 rounded-lg bg-blue-50 flex items-center justify-center text-blue-500">
              <el-icon :size="16"><Phone /></el-icon>
            </div>
            <div>
              <div class="text-slate-500">客服热线</div>
              <div class="text-slate-800 font-semibold">400-888-8888</div>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 rounded-lg bg-green-50 flex items-center justify-center text-green-500">
              <el-icon :size="16"><Message /></el-icon>
            </div>
            <div>
              <div class="text-slate-500">客服邮箱</div>
              <div class="text-slate-800 font-semibold">support@xmhouse.com</div>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 rounded-lg bg-orange-50 flex items-center justify-center text-orange-500">
              <el-icon :size="16"><InfoFilled /></el-icon>
            </div>
            <div>
              <div class="text-slate-500">工作时间</div>
              <div class="text-slate-800 font-semibold">周一至周日 09:00 - 21:00</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部版权 -->
      <div class="text-center text-xs text-slate-300 pb-6">
        Copyright &copy; {{ new Date().getFullYear() }} xM房产 All Rights Reserved.
      </div>
    </div>
  </div>
</template>
