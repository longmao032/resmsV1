import request from '@/utils/request'

export interface HousePageParams {
  pageNum?: number
  pageSize?: number
  houseType?: number
  projectId?: number
  city?: string
  district?: string
  minUnitPriceFen?: number
  maxUnitPriceFen?: number
  minTotalPriceFen?: number
  maxTotalPriceFen?: number
  minRentPriceFen?: number
  maxRentPriceFen?: number
  minArea?: number
  maxArea?: number
  layout?: string
  houseNo?: string
  roomNo?: string
}

export interface HousePageItem {
  id: number
  houseNo: string
  projectId: number
  projectName: string
  houseType: number
  buildingNo: string
  unitNo: string
  roomNo: string
  province: string
  city: string
  district: string
  area: number
  layout: string
  floor: number
  totalFloor: number
  orientation: string
  decoration: string
  tags: string[]
  unitPriceFen: number
  totalPriceFen: number
  rentPriceFen: number | null
  price: number
  priceUnit: number
  unitPrice: number
  description: string
  salesId: number
  status: number
  publishTime: string
  createTime: string
  updateTime: string
  // HousePageVO 扩展字段
  coverUrl: string
  salesName: string
  salesAvatar: string | null
  distance: number | null
}

export interface HouseImage {
  id: number
  houseId: number
  fileKey: string
  url: string
  imageType: number
  imageGroup: string
  sortOrder: number
  isDefault: number
}

export interface NewHouseExtend {
  id: number
  houseId: number
  preSaleLicenseNo: string
  recordPrice: number
  avgPrice: number
  propertyRightYears: number
  estimatedDeliveryDate: string
  deliveryStandard: string
  elevatorRatio: string
  actualAreaRate: number
}

export interface SecondHouseExtend {
  id: number
  houseId: number
  totalPrice: number
  buildYear: number
  houseUsage: string
  isOnlyHouse: number
  isFullTwo: number
  isFullFive: number
  mortgageStatus: number
  propertyDeed: string
  propertyDeedUrl: string
  lastTransactionTime: string
}

export interface RentHouseExtend {
  id: number
  houseId: number
  monthlyRent: number
  rentType: number
  depositMethod: string
  depositAmount: number
  checkInDate: string
  minLeasePeriod: number
  supportShortRent: number
  appliances: string
}

export interface HouseDetail {
  house: HousePageItem
  images: HouseImage[]
  newHouseExtend: NewHouseExtend | null
  secondHouseExtend: SecondHouseExtend | null
  rentHouseExtend: RentHouseExtend | null
  price: number
  priceUnit: number
  unitPrice: number
  salesName?: string
  salesAvatar?: string | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 分页查询在售房源 */
export function getHousePageApi(params: HousePageParams): Promise<PageResult<HousePageItem>> {
  return request.get('/api/portal/v1/houses/page', { params })
}

/** 获取房源详情 */
export function getHouseDetailApi(id: number): Promise<HouseDetail> {
  return request.get(`/api/portal/v1/houses/${id}`)
}

// ---- 楼盘（Project）相关 ----

export interface ProjectPageParams {
  pageNum?: number
  pageSize?: number
  city?: string
  district?: string
  projectName?: string
}

export interface ProjectPageItem {
  id: number
  projectName: string
  developer: string
  city: string
  district: string
  address: string
  tags: string[]
  coverUrl: string
  avgPrice: number | null
  minArea: number | null
  maxArea: number | null
  layoutSummary: string | null
  houseCount: number
  distance: number | null
}

/** 分页查询在售新房楼盘 */
export function getProjectPageApi(params: ProjectPageParams): Promise<PageResult<ProjectPageItem>> {
  return request.get('/api/portal/v1/projects/page', { params })
}

export interface ProjectDetailItem {
  id: number
  projectName: string
  developer: string
  propertyCompany: string
  city: string
  district: string
  address: string
  totalHouseholds: number | null
  propertyFee: number | null
  plotRatio: number | null
  greeningRate: number | null
  tags: string[]
  coverUrl: string
  avgPrice: number | null
  minArea: number | null
  maxArea: number | null
  layoutSummary: string | null
  houseCount: number
}

/** 获取楼盘详情 */
export function getProjectDetailApi(id: number): Promise<ProjectDetailItem> {
  return request.get(`/api/portal/v1/projects/${id}`)
}
