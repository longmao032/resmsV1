import request from '@/utils/request'
import type { Result } from '@/types/api'

/** 部门月度目标 */
export interface DeptTarget {
  id?: number
  deptId: number
  deptName?: string
  targetMonth: string
  targetAmount: number
  updateTime?: string
}

/** 成员月度业绩 */
export interface MemberPerformance {
  userId: number
  totalDealAmount: number
  totalOrders: number
}

/** 获取部门月度目标 */
export function getDeptTarget(deptId: number, month: string) {
  return request<DeptTarget>({
    url: '/system/v1/dept-targets',
    method: 'get',
    params: { deptId, month }
  })
}

/** 保存部门月度目标 */
export function saveDeptTarget(data: {
  id?: number
  deptId: number
  targetMonth: string
  targetAmount: number
}) {
  return request<boolean>({
    url: '/system/v1/dept-targets',
    method: 'put',
    data
  })
}

/** 获取成员月度业绩 */
export function getMemberPerformance(userIds: number[], month: string) {
  return request<MemberPerformance[]>({
    url: '/finance/v1/reports/member-performance',
    method: 'get',
    params: { userIds: userIds.join(','), month }
  })
}
