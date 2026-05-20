import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getNav, type Menu } from '@/api/system/menu'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('resms_token') || '')
  const userInfo = ref<any>(JSON.parse(localStorage.getItem('resms_user_info') || 'null'))
  const menuList = ref<Menu[]>([])

  const permissions = ref<string[]>([])

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('resms_token', newToken)
  }

  function clearToken() {
    token.value = ''
    menuList.value = []
    permissions.value = []
    userInfo.value = null
    localStorage.removeItem('resms_token')
    localStorage.removeItem('resms_user_info')
  }

  function setUserInfo(info: any) {
    userInfo.value = info
    localStorage.setItem('resms_user_info', info ? JSON.stringify(info) : '')
  }

  function setPermissions(perms: string[]) {
    permissions.value = perms
  }

  async function fetchMenus() {
    const { data } = await getNav()
    menuList.value = data
    
    // 递归提取所有按钮权限代码
    const perms: string[] = []
    const extractPerms = (menus: Menu[]) => {
      menus.forEach(menu => {
        if (menu.menuType === 3 && menu.menuCode) {
          perms.push(menu.menuCode)
        }
        if (menu.children && menu.children.length > 0) {
          extractPerms(menu.children)
        }
      })
    }
    extractPerms(data)
    permissions.value = perms
    
    return data
  }

  return {
    token,
    userInfo,
    menuList,
    permissions,
    setToken,
    clearToken,
    setUserInfo,
    setPermissions,
    fetchMenus
  }
})
