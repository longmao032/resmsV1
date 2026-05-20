import request from '@/utils/request'

export interface LoginParams {
  phone: string
  password: string
}

export interface RegisterParams {
  phone: string
  password: string
  nickname?: string
}

export interface LoginResult {
  token: string
  tokenHead: string
  userId: number
  phone: string
  nickname: string
  avatarUrl: string
}

/** C端用户登录 */
export function loginApi(data: LoginParams): Promise<LoginResult> {
  return request.post('/api/portal/v1/auth/login', data)
}

/** C端用户注册 */
export function registerApi(data: RegisterParams): Promise<LoginResult> {
  return request.post('/api/portal/v1/auth/register', data)
}

/** C端用户登出 */
export function logoutApi(): Promise<string> {
  return request.post('/api/portal/v1/auth/logout')
}
