<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/system/auth'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const loginFormRef = ref()

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        const { data } = await login(loginForm)
        userStore.setToken(data.token)
        userStore.setUserInfo(data.user)
        ElMessage.success('登录成功')
        router.push('/')
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<template>
  <div class="login-container flex items-center justify-center min-h-screen bg-[#f8fafc]">
    <div class="login-box w-[420px] bg-white p-10 rounded-2xl shadow-[var(--elevation-2)] transition-shadow hover:shadow-[var(--elevation-3)]">
      <div class="text-center mb-10">
        <h1 class="text-3xl font-bold text-[#1a73e8] tracking-tight">RESMS</h1>
        <p class="text-gray-400 mt-2 text-sm">房产销售管理系统 · 管理后台</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        size="large"
        label-position="top"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            :prefix-icon="User"
            class="material-input"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            class="material-input"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="flex items-center justify-between mb-6 text-sm">
          <el-checkbox label="记住我" />
          <a href="#" class="text-[#1a73e8] hover:underline">忘记密码？</a>
        </div>

        <el-button
          type="primary"
          class="w-full !h-12 !text-base !rounded-lg !bg-[#1a73e8] !border-none shadow-md hover:shadow-lg transition-all"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <div class="mt-10 text-center text-gray-400 text-xs">
        &copy; 2026 RESMS. All rights reserved.
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  background-image: radial-gradient(#e2e8f0 1px, transparent 1px);
  background-size: 20px 20px;
}

.material-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border-bottom: 2px solid #e2e8f0;
  border-radius: 0;
  padding: 0;
  transition: border-color 0.3s;
}

.material-input :deep(.el-input__wrapper.is-focus) {
  border-bottom-color: #1a73e8;
}

.material-input :deep(.el-input__inner) {
  height: 48px;
}
</style>
