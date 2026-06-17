import request from './request'

// ========== 认证接口 ==========

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  username: string
  role: string
}

export function loginApi(params: LoginParams) {
  return request.post<LoginResult>('/auth/login', params)
}

export function logoutApi() {
  return request.post('/auth/logout')
}

// ========== 用户接口 ==========

export function getUserInfoApi() {
  return request.get('/user/info')
}
