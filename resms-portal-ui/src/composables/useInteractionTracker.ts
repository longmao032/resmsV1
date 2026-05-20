import { ref, onMounted, onUnmounted, watch, type Ref, isRef } from 'vue'
import { addBrowseHistoryApi } from '@/api/user'

/**
 * 交互时长追踪钩子
 * @param resourceId 资源ID (响应式或普通数值)
 * @param resourceType 资源类型 (1=房源, 2=项目)
 * @param isLoggedIn 是否已登录
 */
export function useInteractionTracker(resourceId: Ref<number> | number, resourceType: number, isLoggedIn: boolean) {
  const duration = ref(0)
  let timer: number | null = null
  let heartbeat: number | null = null

  // 获取当前 ID 的工具函数
  const getResourceId = () => (isRef(resourceId) ? resourceId.value : resourceId)

  // 发送心跳数据
  const sendHeartbeat = async (id?: number) => {
    const targetId = id || getResourceId()
    if (!isLoggedIn || duration.value <= 0 || !targetId) return
    
    try {
      await addBrowseHistoryApi({
        resourceId: targetId,
        resourceType,
        duration: duration.value
      })
    } catch (e) {
      console.error('[Interaction] Heartbeat failed:', e)
    }
  }

  // 开始计时
  const startTimer = () => {
    if (timer) return
    timer = window.setInterval(() => {
      duration.value++
    }, 1000)
  }

  // 停止计时
  const stopTimer = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  // 处理可见性变化
  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      startTimer()
    } else {
      stopTimer()
      sendHeartbeat() // 切换走时立即同步
    }
  }

  // 监听 ID 变化 (针对路由跳转但不销毁组件的情况)
  if (isRef(resourceId)) {
    watch(resourceId, (newId, oldId) => {
      if (oldId) {
        sendHeartbeat(oldId) // 为旧房源保存最后一次时长
      }
      duration.value = 0 // 重置计时
      if (isLoggedIn) {
        sendHeartbeat(newId) // 为新房源初始化记录
      }
    })
  }

  onMounted(() => {
    if (!isLoggedIn) return
    
    startTimer()
    document.addEventListener('visibilitychange', handleVisibilityChange)
    heartbeat = window.setInterval(() => sendHeartbeat(), 15000)
    
    // 初始进入也发送一次以确保记录存在（也可以由外部业务逻辑触发）
    sendHeartbeat()
  })

  onUnmounted(() => {
    if (!isLoggedIn) return

    stopTimer()
    if (heartbeat) {
      clearInterval(heartbeat)
    }
    
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    sendHeartbeat()
  })

  return {
    duration
  }
}
