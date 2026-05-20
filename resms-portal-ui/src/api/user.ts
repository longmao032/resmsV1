import request from '@/utils/request'

// ---- 类型定义 ----

export interface UserProfile {
  userId: number
  phone: string
  nickname: string
  avatarUrl: string
  gender: number // 0-未知 1-男 2-女
  email: string
  createTime: string
  favoriteCount: number
  appointmentCount: number
  browseCount: number
}

export interface UpdateProfileDTO {
  nickname?: string
  avatarUrl?: string
  gender?: number
  email?: string
}

export interface UpdatePasswordDTO {
  oldPassword: string
  newPassword: string
}

export interface FavoriteItem {
  id: number
  houseId: number
  houseNo: string
  projectName: string
  houseType: number // 1-新房 2-二手房 3-租房
  coverUrl: string
  city: string
  district: string
  area: number
  layout: string
  totalPrice: number
  priceUnit: string
  unitPrice: number
  favoriteTime: string
}

export interface AppointmentItem {
  id: number
  houseId: number
  houseTitle: string
  houseAddress: string
  followDate: string
  salesId: number
  salesName: string
  status: number // 1-待处理 2-已完成 3-已取消
  content: string
  createTime: string
}

export interface BrowseHistoryItem {
  id: number
  resourceType: number // 1=房源, 2=项目
  resourceId: number
  resourceTitle: string
  resourceCover: string
  city: string
  district: string
  layout: string
  area: number
  priceDesc: string
  actionType: string // view=浏览, call=电话, visit=带看, chat=咨询
  duration: number
  viewTime: string
}

export interface FeedbackSubmit {
  type: string
  content: string
  contact?: string
  images?: string[]
}

export interface PageParams {
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ---- Mock 数据 ----

const mockProfile: UserProfile = {
  userId: 1001,
  phone: '13800138000',
  nickname: '房产达人',
  avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
  gender: 1,
  email: 'user@example.com',
  createTime: '2025-06-15 10:30:00',
  favoriteCount: 4,
  appointmentCount: 4,
  browseCount: 4
}

const mockFavorites: FavoriteItem[] = [
  {
    id: 1,
    houseId: 101,
    houseNo: 'NF-20250001',
    projectName: '翡翠湾花园',
    houseType: 1,
    coverUrl: '',
    city: '广州市',
    district: '天河区',
    area: 128,
    layout: '3室2厅2卫',
    totalPrice: 5800000,
    priceUnit: '元/套',
    unitPrice: 45312,
    favoriteTime: '2026-05-10 14:20:00'
  },
  {
    id: 2,
    houseId: 102,
    houseNo: 'ES-20250042',
    projectName: '万科城市花园',
    houseType: 2,
    coverUrl: '',
    city: '广州市',
    district: '海珠区',
    area: 95,
    layout: '2室2厅1卫',
    totalPrice: 3200000,
    priceUnit: '元/套',
    unitPrice: 33684,
    favoriteTime: '2026-05-08 09:15:00'
  },
  {
    id: 3,
    houseId: 103,
    houseNo: 'ZF-20250108',
    projectName: '保利天悦',
    houseType: 3,
    coverUrl: '',
    city: '广州市',
    district: '番禺区',
    area: 85,
    layout: '2室1厅1卫',
    totalPrice: 4500,
    priceUnit: '元/月',
    unitPrice: 53,
    favoriteTime: '2026-05-05 16:40:00'
  },
  {
    id: 4,
    houseId: 104,
    houseNo: 'NF-20250019',
    projectName: '华润城润府',
    houseType: 1,
    coverUrl: '',
    city: '深圳市',
    district: '南山区',
    area: 143,
    layout: '4室2厅2卫',
    totalPrice: 12000000,
    priceUnit: '元/套',
    unitPrice: 83916,
    favoriteTime: '2026-05-01 11:30:00'
  }
]

const mockAppointments: AppointmentItem[] = [
  {
    id: 1,
    projectId: 201,
    projectName: '翡翠湾花园',
    projectCoverUrl: '',
    appointmentTime: '2026-05-20 14:00',
    salesName: '李经理',
    salesPhone: '13900139001',
    status: 0,
    remark: '希望了解三居室户型',
    createTime: '2026-05-12 09:00:00'
  },
  {
    id: 2,
    projectId: 202,
    projectName: '万科城市花园',
    projectCoverUrl: '',
    appointmentTime: '2026-05-18 10:00',
    salesName: '王顾问',
    salesPhone: '13900139002',
    status: 1,
    remark: '',
    createTime: '2026-05-10 15:30:00'
  },
  {
    id: 3,
    projectId: 203,
    projectName: '保利天悦',
    projectCoverUrl: '',
    appointmentTime: '2026-05-15 16:00',
    salesName: '张经理',
    salesPhone: '13900139003',
    status: 2,
    remark: '已看房，比较满意',
    createTime: '2026-05-08 11:00:00'
  },
  {
    id: 4,
    projectId: 204,
    projectName: '华润城润府',
    projectCoverUrl: '',
    appointmentTime: '2026-05-10 09:30',
    salesName: '赵顾问',
    salesPhone: '13900139004',
    status: 3,
    remark: '临时有事取消',
    createTime: '2026-05-06 17:00:00'
  }
]

// Mock 延迟模拟
function mockDelay<T>(data: T, ms = 300): Promise<T> {
  return new Promise(resolve => setTimeout(() => resolve(data), ms))
}

// ---- API 函数 ----

/** 获取用户详细资料 */
export function getUserProfileApi(): Promise<UserProfile> {
  return request.get('/api/portal/v1/user/profile')
}

/** 更新用户资料 */
export function updateUserProfileApi(data: UpdateProfileDTO): Promise<string> {
  return request.put('/api/portal/v1/user/profile', data)
}

/** 修改密码 */
export function updatePasswordApi(data: UpdatePasswordDTO): Promise<string> {
  return request.put('/api/portal/v1/user/password', data)
}

/** 分页获取收藏列表 */
export function getFavoritesApi(params: PageParams): Promise<PageResult<FavoriteItem>> {
  return request.get('/api/portal/v1/user/favorites', { params })
}

/** 取消收藏 */
export function removeFavoriteApi(id: number): Promise<string> {
  return request.delete(`/api/portal/v1/user/favorites/${id}`)
}

/** 添加收藏 */
export function addFavoriteApi(data: { targetId: number; targetType: number }): Promise<string> {
  return request.post('/api/portal/v1/user/favorites', data)
}

/** 分页获取预约记录 */
export function getAppointmentsApi(params: PageParams): Promise<PageResult<AppointmentItem>> {
  return request.get('/api/portal/v1/user/appointments', { params })
}

/** 提交预约看房 */
export function bookAppointmentApi(data: { houseId: number; viewTime: string; remark?: string }): Promise<void> {
  return request.post('/api/portal/v1/user/appointments', data)
}

/** 取消预约 */
export function cancelAppointmentApi(id: number): Promise<void> {
  return request.delete(`/api/portal/v1/user/appointments/${id}`)
}

/** 添加浏览记录 */
export function addBrowseHistoryApi(data: { resourceId: number; resourceType: number; duration?: number }): Promise<void> {
  return request.post('/api/portal/v1/user/browse-history', data)
}

/** 分页获取浏览记录 */
export function getBrowseHistoryApi(params: PageParams & { resourceType?: number }): Promise<PageResult<BrowseHistoryItem>> {
  return request.get('/api/portal/v1/user/browse-history', { params })
}

/** 删除单条浏览记录 */
export function removeBrowseHistoryApi(id: number): Promise<string> {
  return request.delete(`/api/portal/v1/user/browse-history/${id}`)
}

/** 清空所有浏览记录 */
export function clearBrowseHistoryApi(): Promise<string> {
  return request.delete('/api/portal/v1/user/browse-history')
}

/** 提交意见反馈 */
export function submitFeedbackApi(data: FeedbackSubmit): Promise<string> {
  // TODO: return request.post('/api/portal/v1/user/feedback', data)
  console.log('提交反馈:', data)
  return mockDelay('success')
}
