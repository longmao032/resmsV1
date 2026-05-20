import request from '@/utils/request'
import type { Result } from '@/types/api'

export interface ProjectReportVO {
  projectId: number
  projectName: string
  totalIncome: number
  totalCommission: number
  dealCount: number
}

export interface SalesReportVO {
  userId: number
  realName: string
  totalDealAmount: number
  totalActualPaid: number
  totalCommission: number
  totalOrders: number
}

export interface TrendReportVO {
  dateStr: string
  income: number
  expense: number
}

export function getProjectReport() {
  return request<Result<ProjectReportVO[]>>({
    url: '/finance/v1/reports/project',
    method: 'get'
  })
}

export function getSalesReport() {
  return request<Result<SalesReportVO[]>>({
    url: '/finance/v1/reports/sales',
    method: 'get'
  })
}

export function getTrendReport(days = 30) {
  return request<Result<TrendReportVO[]>>({
    url: '/finance/v1/reports/trend',
    method: 'get',
    params: { days }
  })
}

export interface TeamPerformanceVO {
  summary: {
    totalDealAmount: number
    newCustomerCount: number
    totalViewings: number
    targetRate: number
  }
  monthlyTrend: { month: string; value: number }[]
  salesRanking: SalesReportVO[]
  houseTypeDistribution: { name: string; value: number; color: string }[]
  funnel: { customerCount: number; viewingCount: number; dealCount: number }
}

export function getTeamPerformance() {
  return request<Result<TeamPerformanceVO>>({
    url: '/finance/v1/reports/team-performance',
    method: 'get'
  })
}
