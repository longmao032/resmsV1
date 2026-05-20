<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Plus } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getUserProfileApi, updateUserProfileApi, updatePasswordApi, type UserProfile } from '@/api/user'
import { uploadFileApi } from '@/api/common'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const profile = ref<UserProfile | null>(null)
const loading = ref(false)
const saving = ref(false)
const changingPassword = ref(false)

// 基本信息表单
const profileForm = reactive({
  nickname: '',
  gender: 0,
  email: ''
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordFormRef = ref<FormInstance>()
const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { min: 6, message: '密码长度不少于6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function fetchProfile() {
  loading.value = true
  try {
    profile.value = await getUserProfileApi()
    profileForm.nickname = profile.value.nickname
    profileForm.gender = profile.value.gender
    profileForm.email = profile.value.email
  } catch (e: any) {
    console.error('获取用户信息失败', e)
    ElMessage.error(e?.message || '获取用户信息失败')
  } finally {
    loading.value = false
  }
}

async function handleSaveProfile() {
  if (!profileForm.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    await updateUserProfileApi({
      nickname: profileForm.nickname,
      gender: profileForm.gender,
      email: profileForm.email
    })
    // 同步更新 auth store 中的昵称
    if (authStore.userInfo) {
      authStore.setUserInfo({
        ...authStore.userInfo,
        nickname: profileForm.nickname
      })
    }
    // 刷新最新资料
    profile.value = await getUserProfileApi()
    ElMessage.success('资料更新成功')
  } catch (e: any) {
    console.error('更新资料失败', e)
    ElMessage.error(e?.message || '更新失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    changingPassword.value = true
    try {
      await updatePasswordApi({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      ElMessage.success('密码修改成功，请重新登录')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      await authStore.logout()
      router.push('/login')
    } catch (e: any) {
      console.error('修改密码失败', e)
      ElMessage.error(e?.message || '密码修改失败，请检查原密码是否正确')
    } finally {
      changingPassword.value = false
    }
  })
}

const uploadingAvatar = ref(false)

function beforeAvatarUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

async function handleAvatarUpload(options: any) {
  uploadingAvatar.value = true
  try {
    const file = options.file as File
    // 1. 上传文件到服务器
    const { url } = await uploadFileApi(file, 'AVATAR')
    // 2. 更新用户资料中的头像地址
    await updateUserProfileApi({ avatarUrl: url })
    // 3. 同步更新本地状态
    if (authStore.userInfo) {
      authStore.setUserInfo({ ...authStore.userInfo, avatarUrl: url })
    }
    if (profile.value) {
      profile.value.avatarUrl = url
    }
    ElMessage.success('头像更新成功')
  } catch (e) {
    console.error('头像上传失败', e)
    ElMessage.error('头像更新失败，请重试')
  } finally {
    uploadingAvatar.value = false
  }
}

onMounted(fetchProfile)
</script>

<template>
  <div class="min-h-screen bg-slate-50/50 pb-20">
    <!-- 顶部导航 -->
    <div class="bg-white pt-24 pb-8 px-6 border-b border-slate-100">
      <div class="max-w-3xl mx-auto">
        <el-breadcrumb :separator-icon="ArrowRight" class="mb-4">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/profile' }">个人中心</el-breadcrumb-item>
          <el-breadcrumb-item>账号设置</el-breadcrumb-item>
        </el-breadcrumb>
        <h1 class="text-2xl font-bold text-slate-800">账号设置</h1>
      </div>
    </div>

    <div class="max-w-3xl mx-auto px-6 mt-8 space-y-6">
      <!-- 头像修改 -->
      <div class="bg-white rounded-2xl shadow-sm p-6">
        <h2 class="text-lg font-bold text-slate-800 mb-6">头像</h2>
        <div class="flex items-center gap-6">
          <div class="w-24 h-24 rounded-2xl overflow-hidden bg-slate-100 border-2 border-slate-100">
            <img
              :src="profile?.avatarUrl || authStore.userInfo?.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'"
              class="w-full h-full object-cover"
            />
          </div>
          <div>
            <el-upload
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :http-request="handleAvatarUpload"
              accept="image/*"
            >
              <el-button type="primary" plain class="!rounded-xl" :loading="uploadingAvatar">
                <el-icon class="mr-1"><Plus /></el-icon>
                更换头像
              </el-button>
            </el-upload>
            <p class="text-xs text-slate-400 mt-2">支持 JPG、PNG 格式，不超过 2MB</p>
          </div>
        </div>
      </div>

      <!-- 基本信息 -->
      <div class="bg-white rounded-2xl shadow-sm p-6">
        <h2 class="text-lg font-bold text-slate-800 mb-6">基本信息</h2>
        <el-form label-position="top" class="max-w-md">
          <el-form-item label="昵称">
            <el-input
              v-model="profileForm.nickname"
              placeholder="请输入昵称"
              maxlength="20"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input
              :model-value="profile?.phone || authStore.userInfo?.phone || ''"
              disabled
            />
            <p class="text-xs text-slate-400 mt-1">手机号暂不支持修改</p>
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="profileForm.gender">
              <el-radio :value="0">未设置</el-radio>
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input
              v-model="profileForm.email"
              placeholder="请输入邮箱"
              type="email"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" class="!px-8 !rounded-xl" @click="handleSaveProfile">
              保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 修改密码 -->
      <div class="bg-white rounded-2xl shadow-sm p-6">
        <h2 class="text-lg font-bold text-slate-800 mb-6">修改密码</h2>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          class="max-w-md"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入原密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码（不少于6位）"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="changingPassword" class="!px-8 !rounded-xl" @click="handleChangePassword">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>
