import type { RouteRecordRaw } from 'vue-router'
import type { Menu } from '@/api/system/menu'

// 自动导入 views 下的所有 .vue 文件
const modules = import.meta.glob('@/views/**/*.vue')

/**
 * 将后台返回的菜单树转换为动态路由
 */
export function transformMenuToRoutes(menus: Menu[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []

  menus.forEach(menu => {
    // 只有菜单类型才生成路由 (1: 目录, 2: 菜单)
    if (menu.menuType === 3) return

    const route: RouteRecordRaw = {
      path: menu.path,
      name: menu.menuCode,
      component: loadComponent(menu.component),
      meta: {
        title: menu.menuName,
        icon: menu.icon,
        id: menu.id
      }
    }

    if (menu.children && menu.children.length > 0) {
      // 递归处理子路由
      // 如果是目录类型，通常不需要 component 或者使用特定的目录组件
      // 在本系统中，一级目录通常使用 Layout，这里已经在主路由中定义
      route.children = transformMenuToRoutes(menu.children)
    }

    routes.push(route)
  })

  return routes
}

/**
 * 动态加载组件
 */
function loadComponent(component: string) {
  if (component === 'Layout') return () => import('@/layout/index.vue')
  
  // 1. 尝试标准化路径
  const normalizedPath = component.startsWith('/') ? component : `/${component}`
  const fullPath = `/src/views${normalizedPath}.vue`
  
  // 2. 打印调试信息，方便在控制台查看匹配结果
  const availableKeys = Object.keys(modules)
  console.log('[Router] Loading component:', component)
  console.log('[Router] Target path:', fullPath)
  
  // 3. 匹配模块
  if (modules[fullPath]) {
    return modules[fullPath]
  }
  
  // 4. 备选方案：尝试模糊匹配末尾路径
  const fallbackKey = availableKeys.find(key => key.endsWith(`${component}.vue`))
  if (fallbackKey) {
    console.warn(`[Router] Exact match failed, using fallback: ${fallbackKey}`)
    return modules[fallbackKey]
  }

  console.error(`[Router] Component not found: ${fullPath}. Available:`, availableKeys)
  // 返回兜底页面
  return () => import('@/views/dashboard/index.vue')
}
