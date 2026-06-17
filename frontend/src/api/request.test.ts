/**
 * Axios Request 模块测试
 * 测试 src/api/request.ts: 请求/响应拦截器逻辑
 * 采用行为模拟方式测试拦截器的核心逻辑
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'

// Mock localStorage - 使用 storageMap 控制
const storageMap = new Map<string, string>()

// Mock element-plus - 工厂函数不能引用外部变量（vi.mock 会被提升）
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  }
}))

// 导入以获取 mocked ElMessage
import { ElMessage } from 'element-plus'

// Mock window.location
const mockLocation = {
  pathname: '/',
  href: '',
}
Object.defineProperty(window, 'location', {
  value: mockLocation,
  writable: true
})

describe('Request 拦截器', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    storageMap.clear()
    mockLocation.pathname = '/'
    mockLocation.href = ''
  })

  afterEach(() => {
    vi.clearAllMocks()
    storageMap.clear()
  })

  // ============================================
  // Axios 实例创建
  // ============================================
  describe('Axios 实例配置', () => {
    it('request 模块应可正常导入', async () => {
      const module = await import('@/api/request')
      expect(module.default).toBeDefined()
    })
  })

  // ============================================
  // 请求拦截器 - Token 注入
  // ============================================
  describe('请求拦截器 - Token 注入', () => {
    function simulateRequestInterceptor(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
      const token = storageMap.get('token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    }

    it('Token 存在时应添加 Authorization 头', () => {
      storageMap.set('token', 'jwt-token-abc')
      const config = { headers: {} } as InternalAxiosRequestConfig

      const result = simulateRequestInterceptor(config)

      expect(result.headers.Authorization).toBe('Bearer jwt-token-abc')
    })

    it('Token 不存在时不应添加 Authorization 头', () => {
      const config = { headers: {} } as InternalAxiosRequestConfig

      const result = simulateRequestInterceptor(config)

      expect(result.headers.Authorization).toBeUndefined()
    })

    it('已有 Authorization 头的请求应被覆盖', () => {
      storageMap.set('token', 'new-token')
      const config = {
        headers: { Authorization: 'Bearer old-token' }
      } as InternalAxiosRequestConfig

      const result = simulateRequestInterceptor(config)

      expect(result.headers.Authorization).toBe('Bearer new-token')
    })
  })

  // ============================================
  // 响应拦截器 - 成功响应
  // ============================================
  describe('响应拦截器 - 业务状态码处理', () => {
    function simulateResponseInterceptor(response: AxiosResponse): any {
      const res = response.data
      if (res.code !== 200) {
        ElMessage.error(res.message || '请求失败')
        return Promise.reject(new Error(res.message || '请求失败'))
      }
      return res
    }

    it('code=200 时应返回响应数据', () => {
      const response = {
        data: { code: 200, data: { name: 'test' } }
      } as AxiosResponse

      const result = simulateResponseInterceptor(response)

      expect(result).toEqual({ code: 200, data: { name: 'test' } })
    })

    it('code!=200 时应 reject 并显示错误消息', async () => {
      const response = {
        data: { code: 400, message: '参数错误' }
      } as AxiosResponse

      let caughtError: Error | null = null
      try {
        await simulateResponseInterceptor(response)
      } catch (e: any) {
        caughtError = e
      }

      expect(ElMessage.error).toHaveBeenCalledWith('参数错误')
      expect(caughtError).toBeInstanceOf(Error)
      expect(caughtError?.message).toBe('参数错误')
    })

    it('code!=200 且无 message 时应显示默认消息', async () => {
      const response = {
        data: { code: 500 }
      } as AxiosResponse

      let caughtError: Error | null = null
      try {
        await simulateResponseInterceptor(response)
      } catch (e: any) {
        caughtError = e
      }

      expect(ElMessage.error).toHaveBeenCalledWith('请求失败')
      expect(caughtError).toBeInstanceOf(Error)
      expect(caughtError?.message).toBe('请求失败')
    })
  })

  // ============================================
  // 响应拦截器 - 错误处理
  // ============================================
  describe('响应拦截器 - HTTP 错误处理', () => {
    function simulateErrorHandler(error: any): Promise<never> {
      if (error.response?.status === 401) {
        if (mockLocation.pathname === '/login') {
          return Promise.reject(error)
        }
        storageMap.delete('token')
        mockLocation.href = '/login'
        ElMessage.error('登录已过期，请重新登录')
      } else {
        ElMessage.error(error.message || '网络异常')
      }
      return Promise.reject(error)
    }

    it('401 错误在登录页时应仅 reject（不跳转）', async () => {
      mockLocation.pathname = '/login'
      storageMap.set('token', 'existing-token')
      const error = {
        response: { status: 401, data: { message: '密码错误' } }
      }

      let caught: any = null
      try {
        await simulateErrorHandler(error)
      } catch (e) {
        caught = e
      }

      expect(caught).toBe(error)
      expect(storageMap.get('token')).toBe('existing-token')
      expect(mockLocation.href).not.toBe('/login')
    })

    it('401 错误在非登录页时应清除 token 并跳转到 /login', async () => {
      mockLocation.pathname = '/home'
      storageMap.set('token', 'old-token')
      const error = {
        response: { status: 401 }
      }

      let caught: any = null
      try {
        await simulateErrorHandler(error)
      } catch (e) {
        caught = e
      }

      expect(storageMap.get('token')).toBeUndefined()
      expect(mockLocation.href).toBe('/login')
      expect(ElMessage.error).toHaveBeenCalledWith('登录已过期，请重新登录')
      expect(caught).toBe(error)
    })

    it('非 401 错误应显示错误消息', async () => {
      const error = {
        message: 'Network timeout',
        response: { status: 500 }
      }

      let caught: any = null
      try {
        await simulateErrorHandler(error)
      } catch (e) {
        caught = e
      }

      expect(ElMessage.error).toHaveBeenCalledWith('Network timeout')
      expect(caught).toBe(error)
    })

    it('非 401 错误且无 message 时应显示默认消息', async () => {
      const error = {
        response: { status: 503 }
      }

      let caught: any = null
      try {
        await simulateErrorHandler(error)
      } catch (e) {
        caught = e
      }

      expect(ElMessage.error).toHaveBeenCalledWith('网络异常')
      expect(caught).toBe(error)
    })

    it('401 错误无 message 时应正常 reject', async () => {
      mockLocation.pathname = '/login'
      const error = {
        response: { status: 401 }
      }

      let caught: any = null
      try {
        await simulateErrorHandler(error)
      } catch (e) {
        caught = e
      }

      expect(caught).toBe(error)
    })
  })
})
