import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getFavoritesApi, addFavoriteApi, removeFavoriteApi, type FavoriteItem } from '@/api/user'
import { ElMessage } from 'element-plus'

export const useFavoritesStore = defineStore('favorites', () => {
  // 存储房源 ID 到 收藏记录 ID 的映射
  const houseFavorites = ref<Map<number, number>>(new Map())
  // 存储楼盘 ID 到 收藏记录 ID 的映射
  const projectFavorites = ref<Map<number, number>>(new Map())
  
  const loading = ref(false)

  async function fetchFavorites() {
    loading.value = true
    try {
      // 这里的 pageSize 设大一些以便在本地做状态同步
      const res = await getFavoritesApi({ pageNum: 1, pageSize: 1000 })
      
      const newHouseMap = new Map<number, number>()
      const newProjectMap = new Map<number, number>()
      
      res.records.forEach((item: FavoriteItem) => {
        // 后端 FavoriteItem 结构中 houseId 对应房源
        // 如果是楼盘收藏，可能是在 type 字段区分
        if (item.houseType === 1 || item.houseType === 2 || item.houseType === 3) {
          newHouseMap.set(item.houseId, item.id)
        } else {
          // 假设 4 或其他是楼盘
          newProjectMap.set(item.houseId, item.id)
        }
      })
      
      houseFavorites.value = newHouseMap
      projectFavorites.value = newProjectMap
    } catch (e) {
      console.error('初始化收藏状态失败', e)
    } finally {
      loading.value = false
    }
  }

  function isHouseFavorited(houseId: number) {
    return houseFavorites.value.has(houseId)
  }

  function isProjectFavorited(projectId: number) {
    return projectFavorites.value.has(projectId)
  }

  async function toggleHouseFavorite(houseId: number, houseType: number) {
    const favoriteId = houseFavorites.value.get(houseId)
    
    try {
      if (favoriteId) {
        await removeFavoriteApi(favoriteId)
        houseFavorites.value.delete(houseId)
        ElMessage.success('已取消收藏')
      } else {
        await addFavoriteApi({ targetId: houseId, targetType: 1 })
        // 重新获取以同步收藏记录的 ID
        await fetchFavorites()
        ElMessage.success('收藏成功')
      }
    } catch (e: any) {
      if (e.response?.status === 401) {
        ElMessage.warning('请先登录后收藏')
      } else {
        ElMessage.error('操作失败，请重试')
      }
    }
  }

  async function toggleProjectFavorite(projectId: number) {
    const favoriteId = projectFavorites.value.get(projectId)
    
    try {
      if (favoriteId) {
        await removeFavoriteApi(favoriteId)
        projectFavorites.value.delete(projectId)
        ElMessage.success('已取消收藏')
      } else {
        // 楼盘项目 targetType 为 2
        await addFavoriteApi({ targetId: projectId, targetType: 2 })
        await fetchFavorites()
        ElMessage.success('收藏成功')
      }
    } catch (e: any) {
      if (e.response?.status === 401) {
        ElMessage.warning('请先登录后收藏')
      } else {
        ElMessage.error('操作失败，请重试')
      }
    }
  }

  return {
    houseFavorites,
    projectFavorites,
    loading,
    fetchFavorites,
    isHouseFavorited,
    isProjectFavorited,
    toggleHouseFavorite,
    toggleProjectFavorite
  }
})
