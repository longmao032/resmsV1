<template>
  <div class="profile-container min-h-full bg-[#f8f9fa] relative overflow-hidden">
    <!-- 顶部装饰背景 -->
    <div class="absolute top-0 left-0 right-0 h-48 bg-gradient-to-r from-[#1a73e8] to-[#4285f4] opacity-10"></div>
    
    <div class="relative z-10 max-w-6xl mx-auto p-8">
      <!-- 页面标题 -->
      <div class="flex items-center gap-3 mb-8">
        <div class="w-10 h-10 rounded-2xl bg-white shadow-sm flex items-center justify-center">
          <el-icon class="text-[#1a73e8]" :size="20"><User /></el-icon>
        </div>
        <h2 class="text-2xl font-bold text-[#1f1f1f]">个人中心</h2>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-[360px_1fr] gap-8">
        <!-- 左侧：头像 + 摘要信息 -->
        <div class="flex flex-col gap-6">
          <div class="bg-white p-8 rounded-[32px] shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-white/60 backdrop-blur-sm">
            <div class="flex flex-col items-center">
              <div class="relative group mb-6">
                <!-- 头像外环装饰 -->
                <div class="absolute -inset-1 bg-gradient-to-tr from-[#1a73e8] to-[#4285f4] rounded-full opacity-20 group-hover:opacity-40 transition-opacity blur-sm"></div>
                
                <el-avatar 
                  :size="140" 
                  :src="profile.avatar || ''" 
                  class="relative z-10 bg-[#1a73e8] shadow-xl !text-5xl font-bold border-4 border-white"
                >
                  <template v-if="!profile.avatar">{{ userName.charAt(0).toUpperCase() }}</template>
                </el-avatar>
                
                <div
                  class="absolute inset-0 z-20 rounded-full bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center cursor-pointer transition-all duration-300 transform scale-95 group-hover:scale-100"
                  @click="handleAvatarUpload"
                >
                  <el-icon class="text-white text-3xl"><Camera /></el-icon>
                </div>
              </div>
              
              <input ref="avatarInputRef" type="file" accept="image/*" class="hidden" @change="onAvatarChange" />

              <h3 class="text-2xl font-bold text-[#1f1f1f] tracking-tight">{{ profile.nickName || profile.realName || profile.username }}</h3>
              <div class="mt-2 px-4 py-1 bg-blue-50 rounded-full">
                <span class="text-xs font-medium text-[#1a73e8] uppercase tracking-wider">{{ profile.deptName || '未分配部门' }}</span>
              </div>

              <div class="w-full mt-10 space-y-5">
                <div class="flex items-center justify-between text-sm group">
                  <div class="flex items-center gap-3 text-gray-500">
                    <el-icon><User /></el-icon>
                    <span>用户名</span>
                  </div>
                  <span class="font-semibold text-gray-800">{{ profile.username }}</span>
                </div>
                
                <div class="flex items-center justify-between text-sm">
                  <div class="flex items-center gap-3 text-gray-500">
                    <el-icon><CollectionTag /></el-icon>
                    <span>所属角色</span>
                  </div>
                  <div class="flex gap-1">
                    <el-tag 
                      v-for="role in profile.roles" 
                      :key="role.id" 
                      size="small" 
                      class="!rounded-md border-none !bg-gray-100 !text-gray-600"
                    >
                      {{ role.name }}
                    </el-tag>
                    <span v-if="!profile.roles?.length" class="text-gray-400">无</span>
                  </div>
                </div>

                <div class="flex items-center justify-between text-sm">
                  <div class="flex items-center gap-3 text-gray-500">
                    <el-icon><Clock /></el-icon>
                    <span>创建时间</span>
                  </div>
                  <span class="text-gray-600">{{ profile.createTime?.split('T')[0] }}</span>
                </div>
                
                <div class="pt-4 border-t border-gray-50">
                  <div class="flex items-center justify-between text-[11px] text-gray-400">
                    <span>上次登录 IP: {{ profile.lastLoginIp || '未知' }}</span>
                    <span>{{ profile.lastLoginTime?.split('T')[0] || '-' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 安全提示卡片 -->
          <div class="bg-orange-50/50 p-6 rounded-[24px] border border-orange-100">
            <div class="flex gap-3">
              <el-icon class="text-orange-500 mt-0.5"><Warning /></el-icon>
              <div>
                <h4 class="text-sm font-bold text-orange-800 mb-1">账号安全建议</h4>
                <p class="text-xs text-orange-700/70 leading-relaxed">建议定期修改密码并确保手机号已通过验证，以保障账号资产安全。</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：编辑区域 -->
        <div class="bg-white rounded-[32px] shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-white/60 overflow-hidden">
          <el-tabs v-model="activeTab" class="custom-profile-tabs">
            <el-tab-pane label="基本资料" name="base">
              <div class="p-10 max-w-2xl">
                <div class="mb-8">
                  <h3 class="text-lg font-bold text-[#1f1f1f]">基础信息</h3>
                  <p class="text-sm text-gray-400 mt-1">更新您的个人资料，以便同事更好地识别您。</p>
                </div>
                
                <el-form :model="editForm" label-position="top" class="custom-form">
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-x-6">
                    <el-form-item label="真实姓名">
                      <el-input v-model="editForm.realName" placeholder="请输入真实姓名" class="premium-input" />
                    </el-form-item>
                    <el-form-item label="昵称">
                      <el-input v-model="editForm.nickName" placeholder="请输入昵称" class="premium-input" />
                    </el-form-item>
                    <el-form-item label="手机号">
                      <el-input v-model="editForm.phone" placeholder="请输入手机号" class="premium-input">
                        <template #prefix><el-icon><Iphone /></el-icon></template>
                      </el-input>
                    </el-form-item>
                    <el-form-item label="邮箱">
                      <el-input v-model="editForm.email" placeholder="请输入邮箱地址" class="premium-input">
                        <template #prefix><el-icon><Message /></el-icon></template>
                      </el-input>
                    </el-form-item>
                    <el-form-item label="性别">
                      <div class="flex h-12 items-center">
                        <el-radio-group v-model="editForm.sex" class="premium-radio">
                          <el-radio-button :value="1">男</el-radio-button>
                          <el-radio-button :value="2">女</el-radio-button>
                          <el-radio-button :value="0">保密</el-radio-button>
                        </el-radio-group>
                      </div>
                    </el-form-item>
                  </div>
                  
                  <div class="mt-10 pt-8 border-t border-gray-50">
                    <el-button 
                      type="primary" 
                      :loading="saveLoading" 
                      class="!h-12 !px-10 !rounded-2xl !bg-[#1a73e8] hover:shadow-lg hover:shadow-blue-200 transition-all border-none font-bold" 
                      @click="handleSave"
                    >
                      确认保存
                    </el-button>
                  </div>
                </el-form>
              </div>
            </el-tab-pane>

            <el-tab-pane label="安全设置" name="security">
              <div class="p-10 max-w-2xl">
                <div class="mb-8">
                  <h3 class="text-lg font-bold text-[#1f1f1f]">修改登录密码</h3>
                  <p class="text-sm text-gray-400 mt-1">为了您的账号安全，请定期更换复杂程度较高的密码。</p>
                </div>
                
                <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top" class="custom-form">
                  <el-form-item label="当前密码" prop="oldPassword">
                    <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="验证当前登录密码" class="premium-input" />
                  </el-form-item>
                  
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-x-6 mt-4">
                    <el-form-item label="新密码" prop="newPassword">
                      <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="长度不少于6位" class="premium-input" />
                    </el-form-item>
                    <el-form-item label="重复新密码" prop="confirmPassword">
                      <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="确认新密码输入" class="premium-input" />
                    </el-form-item>
                  </div>

                  <div class="mt-10 pt-8 border-t border-gray-50">
                    <el-button 
                      type="primary" 
                      :loading="pwdLoading" 
                      class="!h-12 !px-10 !rounded-2xl !bg-gray-900 border-none font-bold hover:shadow-lg" 
                      @click="handleChangePwd"
                    >
                      立即更新
                    </el-button>
                  </div>
                </el-form>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Camera, User, CollectionTag, Clock, Iphone, Message, Warning } from '@element-plus/icons-vue'
import { getProfile, updateProfile, changePassword } from '@/api/system/user'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const profile = ref<any>({})
const activeTab = ref((route.query.tab as string) || 'base')

// 监听路由参数变化，实现点击“修改密码”时即时切换标签
watch(() => route.query.tab, (newTab) => {
  if (newTab) {
    activeTab.value = newTab as string
  }
})
const saveLoading = ref(false)
const pwdLoading = ref(false)
const avatarInputRef = ref<HTMLInputElement>()

const userName = computed(() => profile.value?.nickName || profile.value?.realName || profile.value?.username || '用户')

const editForm = reactive({
  realName: '',
  nickName: '',
  phone: '',
  email: '',
  sex: 0
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const pwdFormRef = ref()

const fetchProfile = async () => {
  try {
    const res: any = await getProfile()
    profile.value = res.data
    editForm.realName = res.data.realName || ''
    editForm.nickName = res.data.nickName || ''
    editForm.phone = res.data.phone || ''
    editForm.email = res.data.email || ''
    editForm.sex = res.data.sex ?? 0
  } catch {
    // ignore
  }
}

const handleSave = async () => {
  saveLoading.value = true
  try {
    await updateProfile(editForm)
    ElMessage.success('保存成功')
    if (userStore.userInfo) {
      userStore.setUserInfo({ ...userStore.userInfo, ...editForm })
    }
    await fetchProfile()
  } catch {
    // error handled by interceptor
  } finally {
    saveLoading.value = false
  }
}

const handleChangePwd = async () => {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  pwdLoading.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.clearToken()
    router.push('/login')
  } catch {
    // error handled by interceptor
  } finally {
    pwdLoading.value = false
  }
}

const handleAvatarUpload = () => {
  avatarInputRef.value?.click()
}

const onAvatarChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', 'AVATAR')

  try {
    const res: any = await request({
      url: '/v1/common/upload',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 200) {
      const avatarUrl = res.data?.url || res.data
      await updateProfile({ avatar: avatarUrl })
      if (userStore.userInfo) {
        userStore.setUserInfo({ ...userStore.userInfo, avatar: avatarUrl })
      }
      await fetchProfile()
      ElMessage.success('头像更新成功')
    }
  } catch {
    ElMessage.error('头像上传失败')
  }
}

onMounted(fetchProfile)
</script>

<style scoped>
.profile-container {
  height: calc(100vh - 64px);
  overflow-y: auto;
}

/* Tabs 样式优化 */
:deep(.custom-profile-tabs .el-tabs__header) {
  margin: 0;
  padding: 0 40px;
  background-color: #fafbfc;
  border-bottom: 1px solid #f1f3f4;
}

:deep(.custom-profile-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.custom-profile-tabs .el-tabs__item) {
  height: 64px;
  line-height: 64px;
  font-weight: 600;
  color: #5f6368;
  transition: all 0.3s;
}

:deep(.custom-profile-tabs .el-tabs__item.is-active) {
  color: #1a73e8;
}

:deep(.custom-profile-tabs .el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px 3px 0 0;
  background-color: #1a73e8;
}

/* Input 样式优化 */
:deep(.premium-input .el-input__wrapper) {
  border-radius: 14px;
  background-color: #f8f9fa;
  box-shadow: none !important;
  border: 1px solid #e8eaed;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 6px 16px !important;
}

:deep(.premium-input .el-input__wrapper:hover) {
  border-color: #dadce0;
  background-color: #f1f3f4;
}

:deep(.premium-input .el-input__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
  box-shadow: 0 0 0 4px rgba(26, 115, 232, 0.1) !important;
}

:deep(.premium-input .el-input__inner) {
  height: 36px;
  color: #202124;
  font-weight: 500;
}

/* Radio 样式优化 */
:deep(.premium-radio .el-radio-button__inner) {
  border-radius: 12px !important;
  border: 1px solid #e8eaed !important;
  margin-right: 8px;
  background-color: #fff;
  color: #5f6368;
  transition: all 0.2s;
  height: 40px;
  display: flex;
  align-items: center;
}

:deep(.premium-radio .el-radio-button:last-child .el-radio-button__inner) {
  margin-right: 0;
}

:deep(.premium-radio .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #1a73e8 !important;
  border-color: #1a73e8 !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(26, 115, 232, 0.2) !important;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #3c4043;
  margin-bottom: 8px !important;
  padding-left: 4px;
}
</style>
