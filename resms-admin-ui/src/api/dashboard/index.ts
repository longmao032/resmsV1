import request from '@/utils/request'
import type { Result } from '@/types/api'

export interface DashboardStatsVO {
  newHouses: number
  monthlyOrders: number
  totalCommission: number
  activeClients: number
}

export interface DashboardTrendVO {
  dateStr: string
  value: number
}

export interface DashboardActivityVO {
  content: string
  timestamp: string
  user: string
  type: 'primary' | 'success' | 'info' | 'warning' | 'danger'
}

/** 生成趋势 mock 数据 */
function mockTrend(days: number): DashboardTrendVO[] {
  const list: DashboardTrendVO[] = []
  const now = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    list.push({
      dateStr: `${mm}-${dd}`,
      value: Math.round(200000 + Math.random() * 300000 + Math.sin(i * 0.5) * 100000)
    })
  }
  return list
}

/** mock 活动数据 */
const mockActivities: DashboardActivityVO[] = [
  { content: '完成了天河门店的房源审核', timestamp: '10分钟前', user: '管理员 admin', type: 'primary' },
  { content: '新成交一笔二手房交易订单', timestamp: '1小时前', user: '销售 张三', type: 'success' },
  { content: '系统自动备份完成', timestamp: '3小时前', user: '系统任务', type: 'info' },
  { content: '删除了过期的房源图片', timestamp: '5小时前', user: '管理员 admin', type: 'warning' }
]

/** 获取仪表盘核心统计数据 */
export async function getDashboardStats() {
  try {
    return await request<Result<DashboardStatsVO>>({
      url: '/system/v1/dashboard/stats',
      method: 'get'
    })
  } catch {
    return {
      data: {
        newHouses: 128,
        monthlyOrders: 42,
        totalCommission: 285000,
        activeClients: 1204
      }
    } as Result<DashboardStatsVO>
  }
}

/** 获取月度经营走势数据 */
export async function getDashboardTrend(days: number) {
  try {
    return await request<Result<DashboardTrendVO[]>>({
      url: '/system/v1/dashboard/trend',
      method: 'get',
      params: { days }
    })
  } catch {
    return {
      data: mockTrend(days)
    } as Result<DashboardTrendVO[]>
  }
}

/** 获取最新动态 */
export async function getDashboardActivities() {
  try {
    return await request<Result<DashboardActivityVO[]>>({
      url: '/system/v1/dashboard/activities',
      method: 'get'
    })
  } catch {
    return {
      data: mockActivities
    } as Result<DashboardActivityVO[]>
  }
}
