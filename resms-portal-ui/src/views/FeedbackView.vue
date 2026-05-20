<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Message, ChatDotRound, EditPen, Upload, Star } from '@element-plus/icons-vue'
import { submitFeedbackApi } from '@/api/user'

const router = useRouter()
const submitting = ref(false)
const submitted = ref(false)

const feedbackForm = reactive({
  type: '',
  content: '',
  contact: '',
})

const feedbackTypes = [
  { label: '功能建议', value: 'feature' },
  { label: '内容纠错', value: 'bug' },
  { label: '使用体验', value: 'ux' },
  { label: '投诉与举报', value: 'complaint' },
  { label: '其他', value: 'other' },
]

async function handleSubmit() {
  if (!feedbackForm.type) {
    ElMessage.warning('请选择反馈类型')
    return
  }
  if (!feedbackForm.content.trim()) {
    ElMessage.warning('请填写反馈内容')
    return
  }
  if (feedbackForm.content.length < 10) {
    ElMessage.warning('反馈内容至少10个字符')
    return
  }
  submitting.value = true
  try {
    await submitFeedbackApi({
      type: feedbackForm.type,
      content: feedbackForm.content,
      contact: feedbackForm.contact || undefined,
    })
    submitted.value = true
    ElMessage.success('感谢您的反馈！')
  } catch (e) {
    console.error('提交反馈失败', e)
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  feedbackForm.type = ''
  feedbackForm.content = ''
  feedbackForm.contact = ''
  submitted.value = false
}
</script>

<template>
  <div class="min-h-screen bg-slate-50/50 pb-20">
    <!-- 顶部导航 -->
    <div class="bg-white pt-24 pb-8 px-6 border-b border-slate-100">
      <div class="max-w-3xl mx-auto">
        <el-breadcrumb :separator-icon="ArrowRight" class="mb-4">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/profile' }">个人中心</el-breadcrumb-item>
          <el-breadcrumb-item>意见反馈</el-breadcrumb-item>
        </el-breadcrumb>
        <h1 class="text-2xl font-bold text-slate-800">意见反馈</h1>
        <p class="text-sm text-slate-400 mt-1">您的建议是我们改进的动力</p>
      </div>
    </div>

    <div class="max-w-3xl mx-auto px-6 mt-8">
      <!-- 提交成功 -->
      <div v-if="submitted" class="bg-white rounded-2xl shadow-sm p-12 text-center">
        <div class="w-20 h-20 rounded-full bg-green-50 flex items-center justify-center mx-auto mb-6">
          <el-icon :size="40" class="text-green-500"><Star /></el-icon>
        </div>
        <h2 class="text-2xl font-bold text-slate-800 mb-2">感谢您的反馈！</h2>
        <p class="text-slate-400 mb-8">我们会在 3 个工作日内给您回复</p>
        <div class="flex justify-center gap-4">
          <el-button plain @click="handleReset">继续反馈</el-button>
          <el-button type="primary" @click="router.push('/profile')">返回个人中心</el-button>
        </div>
      </div>

      <!-- 反馈表单 -->
      <div v-else class="bg-white rounded-2xl shadow-sm p-6 space-y-6">
        <!-- 反馈类型 -->
        <div>
          <label class="block text-sm font-bold text-slate-700 mb-3">反馈类型 <span class="text-red-400">*</span></label>
          <div class="grid grid-cols-3 gap-3">
            <div
              v-for="ft in feedbackTypes"
              :key="ft.value"
              class="py-3 px-4 rounded-xl border-2 text-sm font-semibold text-center cursor-pointer transition-all"
              :class="feedbackForm.type === ft.value
                ? 'border-primary bg-primary/5 text-primary'
                : 'border-slate-100 text-slate-500 hover:border-slate-200 hover:bg-slate-50'"
              @click="feedbackForm.type = ft.value"
            >
              <el-icon class="mr-1" :size="14"><Message /></el-icon>
              {{ ft.label }}
            </div>
          </div>
        </div>

        <!-- 反馈内容 -->
        <div>
          <label class="block text-sm font-bold text-slate-700 mb-3">反馈内容 <span class="text-red-400">*</span></label>
          <el-input
            v-model="feedbackForm.content"
            type="textarea"
            :rows="6"
            maxlength="500"
            show-word-limit
            placeholder="请详细描述您的问题或建议（至少10个字符）"
            class="!rounded-xl"
          />
        </div>

        <!-- 联系方式 -->
        <div>
          <label class="block text-sm font-bold text-slate-700 mb-3">联系方式 <span class="text-slate-300 text-xs">（选填，方便我们与您联系）</span></label>
          <el-input
            v-model="feedbackForm.contact"
            placeholder="手机号 / 邮箱 / 微信号"
            maxlength="50"
            class="!rounded-xl"
          />
        </div>

        <!-- 提交按钮 -->
        <el-button
          type="primary"
          class="w-full !h-12 !rounded-2xl !font-bold !text-base"
          :loading="submitting"
          @click="handleSubmit"
        >
          <el-icon class="mr-2"><EditPen /></el-icon>
          提交反馈
        </el-button>
      </div>
    </div>
  </div>
</template>
