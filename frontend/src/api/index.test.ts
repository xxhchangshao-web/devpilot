/**
 * API 函数测试
 * 测试 src/api/index.ts: loginApi, logoutApi, getUserInfoApi
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

// Mock axios
vi.mock('axios', () => {
  const mockPost = vi.fn()
  const mockGet = vi.fn()
  const mockInstance = {
    post: mockPost,
    get: mockGet,
    interceptors: {
      request: { use: vi.fn(), eject: vi.fn() },
      response: { use: vi.fn(), eject: vi.fn() }
    }
  }
  return {
    default: {
      create: vi.fn(() => mockInstance),
      ...mockInstance
    }
  }
})

// Mock element-plus (for request.ts dependencies)
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  }
}))

import { loginApi, logoutApi, getUserInfoApi, type LoginParams, type LoginResult } from '@/api/index'

describe('API 函数', () => {
  let mockPost: ReturnType<typeof vi.fn>
  let mockGet: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.clearAllMocks()
    const axiosDefault = axios as any
    mockPost = axiosDefault.post
    mockGet = axiosDefault.get
  })

  // ============================================
  // loginApi 测试
  // ============================================
  describe('loginApi', () => {
    it('应调用 POST /auth/login 并传递正确的参数', async () => {
      const params: LoginParams = { username: 'admin', password: '123456' }
      const mockResponse = { data: { token: 'jwt-token', username: 'admin', role: 'admin' } }
      mockPost.mockResolvedValue({ data: { code: 200, data: mockResponse.data } })

      await loginApi(params)

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/auth/login', params)
    })

    it('应返回正确的响应数据', async () => {
      const params: LoginParams = { username: 'admin', password: '123456' }
      const responseData = { code: 200, data: { token: 'jwt-token', username: 'admin', role: 'admin' } }
      mockPost.mockResolvedValue({ data: responseData })

      const result = await loginApi(params)

      expect(result.data).toEqual(responseData)
    })

    it('应处理 API 返回错误', async () => {
      const params: LoginParams = { username: 'admin', password: 'wrong' }
      const errorResponse = { data: { code: 401, message: '用户名或密码错误' } }
      mockPost.mockResolvedValue(errorResponse)

      const result = await loginApi(params)

      expect(result.data).toEqual(errorResponse.data)
    })

    it('应处理网络异常', async () => {
      const params: LoginParams = { username: 'admin', password: '123456' }
      mockPost.mockRejectedValue(new Error('Network Error'))

      await expect(loginApi(params)).rejects.toThrow('Network Error')
    })
  })

  // ============================================
  // logoutApi 测试
  // ============================================
  describe('logoutApi', () => {
    it('应调用 POST /auth/logout', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })

      await logoutApi()

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/auth/logout')
    })

    it('应无参数调用', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })

      await logoutApi()

      expect(mockPost).toHaveBeenCalledWith('/auth/logout')
      // 验证只传了一个参数
      expect(mockPost.mock.calls[0].length).toBe(1)
    })
  })

  // ============================================
  // getUserInfoApi 测试
  // ============================================
  describe('getUserInfoApi', () => {
    it('应调用 GET /user/info', async () => {
      mockGet.mockResolvedValue({ data: { code: 200, data: { username: 'admin', role: 'admin' } } })

      await getUserInfoApi()

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/user/info')
    })

    it('应返回用户信息', async () => {
      const userData = { username: 'admin', role: 'admin', avatar: '' }
      mockGet.mockResolvedValue({ data: { code: 200, data: userData } })

      const result = await getUserInfoApi()

      expect(result.data).toEqual({ code: 200, data: userData })
    })

    it('应处理请求失败', async () => {
      mockGet.mockRejectedValue(new Error('Unauthorized'))

      await expect(getUserInfoApi()).rejects.toThrow('Unauthorized')
    })
  })

  // ============================================
  // LoginParams / LoginResult 类型测试
  // ============================================
  describe('类型定义', () => {
    it('LoginParams 应包含 username 和 password', () => {
      const params: LoginParams = { username: 'test', password: 'pass123' }
      expect(params.username).toBe('test')
      expect(params.password).toBe('pass123')
    })

    it('LoginResult 应包含 token, username, role', () => {
      const result: LoginResult = { token: 'abc', username: 'test', role: 'admin' }
      expect(result.token).toBe('abc')
      expect(result.username).toBe('test')
      expect(result.role).toBe('admin')
    })
  })
})
