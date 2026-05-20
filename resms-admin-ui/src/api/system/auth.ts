import request from '@/utils/request'
import type { Result } from '@/types/api'

/**
 * 登录请求参数
 */
export interface LoginData {
  username: string;
  password: string;
}

/**
 * 登录成功返回结果
 */
export interface LoginResult {
  token: string;
  user: {
    id: number;
    username: string;
    nickname: string;
    avatar: string;
    deptId: number;
    dataScope: number;
  }
}

/**
 * 用户登录
 */
export function login(data: LoginData) {
  return request<Result<LoginResult>>({
    url: '/system/v1/auth/login',
    method: 'post',
    data
  })
}

/**
 * 用户登出
 */
export function logout() {
  return request<Result<string>>({
    url: '/system/v1/auth/logout',
    method: 'post'
  })
}
