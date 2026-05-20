import request from '@/utils/request'

// 查询房源收藏热度列表
export function listFavorite(query: any) {
  return request({
    url: '/system/trade/v1/favorites',
    method: 'get',
    params: query
  })
}

// 查询收藏该房源的粉丝列表
export function listFavoriteFans(houseId: number) {
  return request({
    url: `/system/trade/v1/favorites/house/${houseId}/fans`,
    method: 'get'
  })
}
