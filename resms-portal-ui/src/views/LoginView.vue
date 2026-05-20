<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import loginBanner from '@/assets/login_banner.png'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isLogin = ref(true)
const loading = ref(false)

const loginForm = reactive({
  phone: '',
  password: ''
})

const registerForm = reactive({
  phone: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

async function handleLogin() {
  if (!loginForm.phone || !loginForm.password) {
    ElMessage.warning('请输入手机号和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login(loginForm.phone, loginForm.password)
    ElMessage.success('欢迎回来')
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.phone || !registerForm.password) {
    ElMessage.warning('请输入手机号和密码')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码长度不能少于6位')
    return
  }
  loading.value = true
  try {
    await authStore.register(registerForm.phone, registerForm.password, registerForm.nickname || undefined)
    ElMessage.success('账号注册成功')
    isLogin.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex bg-white font-sans">
    <!-- Left: Decorative Side (Hidden on mobile) -->
    <div class="hidden lg:flex lg:w-1/2 relative overflow-hidden bg-gray-900">
      <img
        :src="loginBanner"
        class="absolute inset-0 w-full h-full object-cover opacity-80 scale-105 hover:scale-100 transition-transform duration-[10s]"
        alt="Login background"
      />
      <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent"></div>
      
      <!-- Marketing Content -->
      <div class="relative z-10 p-20 flex flex-col justify-end h-full text-white">
        <div class="mb-8">
          <div class="w-16 h-16 bg-primary rounded-2xl flex items-center justify-center mb-6 shadow-2xl">
            <el-icon :size="32" color="white"><House /></el-icon>
          </div>
          <h1 class="text-5xl font-bold mb-4 tracking-tight">寻找您的<br/>理想之所</h1>
          <p class="text-xl text-gray-300 font-light max-w-md">
            RESMS Portal 为您提供最真实的房源信息，让家近在咫尺。
          </p>
        </div>
        
        <div class="flex gap-12 border-t border-white/20 pt-10">
          <div>
            <div class="text-3xl font-bold text-primary">1.2M+</div>
            <div class="text-sm text-gray-400 mt-1 uppercase tracking-widest">真实房源</div>
          </div>
          <div>
            <div class="text-3xl font-bold text-primary">50k+</div>
            <div class="text-sm text-gray-400 mt-1 uppercase tracking-widest">金牌经纪人</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Form Area -->
    <div class="w-full lg:w-1/2 flex flex-col items-center justify-center p-8 sm:p-16 lg:p-24 relative">
      <!-- Mobile Logo -->
      <div class="lg:hidden flex items-center gap-3 mb-12" @click="router.push('/')">
        <div class="w-10 h-10 bg-primary rounded-xl flex items-center justify-center shadow-lg">
          <el-icon :size="24" color="white"><House /></el-icon>
        </div>
        <span class="text-2xl font-bold text-gray-900 tracking-tight">RESMS</span>
      </div>

      <div class="w-full max-w-md">
        <!-- Heading -->
        <div class="mb-12">
          <h2 class="text-4xl font-black text-gray-900 mb-3">
            {{ isLogin ? '欢迎回来' : '开启新征程' }}
          </h2>
          <p class="text-gray-500 font-medium">
            {{ isLogin ? '请输入您的凭据以访问您的帐户' : '请填写以下信息完成注册' }}
          </p>
        </div>

        <!-- Form Switcher -->
        <div class="flex p-1 bg-gray-100 rounded-2xl mb-8">
          <button
            @click="isLogin = true"
            :class="[
              'flex-1 py-3 text-sm font-bold rounded-xl transition-all duration-300',
              isLogin ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-400 hover:text-gray-600'
            ]"
          >
            登录
          </button>
          <button
            @click="isLogin = false"
            :class="[
              'flex-1 py-3 text-sm font-bold rounded-xl transition-all duration-300',
              !isLogin ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-400 hover:text-gray-600'
            ]"
          >
            注册
          </button>
        </div>

        <!-- Transition Group for smooth switching -->
        <transition name="fade-slide" mode="out-in">
          <!-- Login Form -->
          <div v-if="isLogin" key="login" class="space-y-6">
            <div class="space-y-2">
              <label class="text-sm font-bold text-gray-700 ml-1">手机号码</label>
              <el-input
                v-model="loginForm.phone"
                placeholder="请输入手机号"
                size="large"
                class="custom-input"
                prefix-icon="Phone"
                maxlength="11"
              />
            </div>
            <div class="space-y-2">
              <label class="text-sm font-bold text-gray-700 ml-1">密码</label>
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                class="custom-input"
                prefix-icon="Lock"
                show-password
              />
            </div>
            <el-button
              type="primary"
              size="large"
              class="w-full !h-14 !rounded-2xl !text-lg !font-black !shadow-xl hover:!translate-y-[-2px] active:!translate-y-[0] transition-all"
              :loading="loading"
              @click="handleLogin"
            >
              登录账户
            </el-button>
            <div class="flex justify-center mt-4">
              <a href="#" class="text-xs font-bold text-gray-400 hover:text-primary transition-colors">忘记密码？找回您的账户</a>
            </div>
          </div>

          <!-- Register Form -->
          <div v-else key="register" class="space-y-5">
            <div class="space-y-1">
              <label class="text-sm font-bold text-gray-700 ml-1">手机号码</label>
              <el-input
                v-model="registerForm.phone"
                placeholder="请输入手机号"
                size="large"
                class="custom-input"
                prefix-icon="Phone"
                maxlength="11"
              />
            </div>
            <div class="space-y-1">
              <label class="text-sm font-bold text-gray-700 ml-1">昵称</label>
              <el-input
                v-model="registerForm.nickname"
                placeholder="您想如何被称呼"
                size="large"
                class="custom-input"
                prefix-icon="User"
              />
            </div>
            <div class="space-y-1">
              <label class="text-sm font-bold text-gray-700 ml-1">设置密码</label>
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="至少6位安全密码"
                size="large"
                class="custom-input"
                prefix-icon="Lock"
                show-password
              />
            </div>
            <div class="space-y-1">
              <label class="text-sm font-bold text-gray-700 ml-1">确认密码</label>
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次确认您的密码"
                size="large"
                class="custom-input"
                prefix-icon="Lock"
                show-password
              />
            </div>
            <el-button
              type="primary"
              size="large"
              class="w-full !h-14 !rounded-2xl !text-lg !font-black !shadow-xl hover:!translate-y-[-2px] active:!translate-y-[0] transition-all"
              :loading="loading"
              @click="handleRegister"
            >
              立即注册
            </el-button>
          </div>
        </transition>

        <!-- Social / Alternative -->
        <div class="mt-12">
          <div class="relative flex items-center justify-center mb-8">
            <div class="absolute inset-0 flex items-center">
              <div class="w-full border-t border-gray-100"></div>
            </div>
            <span class="relative bg-white px-4 text-xs font-bold text-gray-400 uppercase tracking-widest">
              其他方式登录
            </span>
          </div>
          
          <div class="grid grid-cols-2 gap-4">
            <button class="flex items-center justify-center gap-3 py-3 border border-gray-100 rounded-xl hover:bg-gray-50 transition-colors font-bold text-sm text-gray-600">
              <img src="https://img.icons8.com/color/48/weixing.png" class="w-5 h-5" alt="WeChat"/>
              微信登录
            </button>
            <button class="flex items-center justify-center gap-3 py-3 border border-gray-100 rounded-xl hover:bg-gray-50 transition-colors font-bold text-sm text-gray-600">
              <img src="https://img.icons8.com/color/48/qq.png" class="w-5 h-5" alt="QQ"/>
              QQ登录
            </button>
          </div>
        </div>

        <p class="mt-10 text-center text-xs text-gray-400 leading-relaxed">
          登录或注册即表示您同意我们的 <a href="#" class="text-primary hover:underline">服务协议</a> 和 <a href="#" class="text-primary hover:underline">隐私政策</a>
        </p>
      </div>
      
      <!-- Back to Home -->
      <button 
        class="absolute top-10 right-10 text-sm font-bold text-gray-400 hover:text-primary transition-colors flex items-center gap-2"
        @click="router.push('/')"
      >
        返回首页 <el-icon><ArrowRight /></el-icon>
      </button>
    </div>
  </div>
</template>

<style scoped>
@reference "@/assets/main.css";

.custom-input :deep(.el-input__wrapper) {
  @apply !rounded-xl !bg-gray-50 !border-none !shadow-none !px-4 !py-3;
  transition: all 0.3s ease;
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  @apply !bg-white !ring-2 !ring-primary/20;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.4s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* Custom font styles to match modern UI */
h2 {
  letter-spacing: -0.02em;
}
</style>
