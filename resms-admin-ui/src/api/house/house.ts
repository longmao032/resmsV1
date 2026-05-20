import request from '@/utils/request'
import type { Result, PageResult } from '@/types/api'

/**
 * 房源接口定义
 */
export interface House {
  id?: number;
  houseNo?: string;
  projectId: number;
  projectName?: string;
  houseType: number;
  buildingNo: string;
  unitNo: string;
  roomNo: string;
  area: number;
  layout: string;
  floor: number;
  totalFloor: number;
  orientation: string;
  decoration: string;
  tags: string[];
  price: number;
  priceUnit: number;
  unitPrice?: number;
  description: string;
  salesId: number;
  salesName?: string;
  status: number;
  coverUrl?: string;
  longitude?: number;
  latitude?: number;
  updateTime?: string;
}

/**
 * 分页查询房源
 */
export function listHouse(query: any) {
  return request<PageResult<House>>({
    url: '/system/house/v1/houses/page',
    method: 'get',
    params: query
  })
}

/**
 * 获取房源详情
 */
export function getHouse(id: number) {
  return request<Result<any>>({
    url: `/system/house/v1/houses/${id}`,
    method: 'get'
  })
}

/**
 * 保存房源 (处理扩展字段转换)
 */
export function saveHouse(data: any) {
  const payload = { ...data };
  
  // 处理扩展字段映射
  if (data.extend) {
    const extendData = { ...data.extend };
    // 确保数据类型安全，移除可能为空的字段或执行必要转换
    if (data.houseType === 1) {
      payload.newHouseExtend = extendData;
    } else if (data.houseType === 2) {
      payload.secondHouseExtend = extendData;
    } else if (data.houseType === 3) {
      payload.rentHouseExtend = extendData;
    }
    delete payload.extend;
  }
  
  // 处理坐标映射
  if (data.coordinate) {
    payload.longitude = data.coordinate.lng;
    payload.latitude = data.coordinate.lat;
    delete payload.coordinate;
  }

  return request<Result<boolean>>({
    url: '/system/house/v1/houses',
    method: 'post',
    data: payload
  })
}

/**
 * 房源审核
 */
export function auditHouse(data: { id: number, auditStatus: number, reason: string }) {
  return request<Result<boolean>>({
    url: '/system/house/v1/houses/audit',
    method: 'post',
    data
  })
}

/**
 * 导出房源
 */
export function exportHouse(query: any) {
  return request({
    url: '/system/house/v1/houses/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}

/**
 * 房源统计概览（顶部看板）
 */
export interface HouseStats {
  onlineCount: number
  onlineTrend: number
  monthlyNewCount: number
  monthlyNewTrend: number
  pendingReviewCount: number
  pendingReviewTrend: number
  avgDealPrice: number
  avgDealPriceTrend: number
}

export function getHouseStats() {
  return request<Result<HouseStats>>({
    url: '/system/house/v1/houses/statistics',
    method: 'get'
  })
}
