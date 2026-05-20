import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'
import { useUserStore } from '@/store/user'
import { transformMenuToRoutes } from '@/utils/router'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '控制台' }
      },
      {
        path: 'system/user/profile',
        name: 'Profile',
        component: () => import('@/views/system/user/profile/index.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '/404',
    component: () => import('@/views/dashboard/index.vue'), // 临时
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 白名单
const whiteList = ['/login', '/404']
// 动态路由加载状态通过 userStore.menuList 判断

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.token

  if (token) {
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      // 检查是否已加载动态路由（通过判断菜单列表是否为空）
      if (userStore.menuList.length === 0) {
        try {
          // 1. 获取用户菜单
          const menus = await userStore.fetchMenus()
          
          // 2. 转换并添加动态路由
          const dynamicRoutes = transformMenuToRoutes(menus)
          dynamicRoutes.forEach(route => {
            // 直接添加顶级动态路由（自带 Layout）
            router.addRoute(route)
          })

          // isRoutesLoaded = true
          // 确保路由已添加完成
          next({ ...to, replace: true })
        } catch (error) {
          console.error('加载动态路由失败:', error)
          userStore.clearToken()
          next(`/login?redirect=${to.path}`)
        }
      } else {
        next()
      }
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
    }
  }
})

export default router
