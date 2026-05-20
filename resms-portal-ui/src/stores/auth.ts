import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, registerApi, logoutApi } from '@/api/auth'
import type { LoginResult } from '@/api/auth'

export interface UserInfo {
  userId: number
  phone: string
  nickname: string
  avatarUrl: string
}

function toUserInfo(res: LoginResult): UserInfo {
  return {
    userId: res.userId,
    phone: res.phone,
    nickname: res.nickname,
    avatarUrl: res.avatarUrl
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  // 从 localStorage 恢复用户信息
  const savedUser = localStorage.getItem('userInfo')
  if (savedUser) {
    try {
      userInfo.value = JSON.parse(savedUser)
    } catch {
      localStorage.removeItem('userInfo')
    }
  }

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  async function login(phone: string, password: string) {
    const res = await loginApi({ phone, password })
    setToken(res.token)
    setUserInfo(toUserInfo(res))
    return res
  }

  async function register(phone: string, password: string, nickname?: string) {
    const res = await registerApi({ phone, password, nickname })
    setToken(res.token)
    setUserInfo(toUserInfo(res))
    return res
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略登出接口错误
    }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    register,
    logout,
    setToken,
    setUserInfo
  }
})
