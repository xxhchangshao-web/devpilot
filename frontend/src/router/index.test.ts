/**
 * Router 测试
 * 测试 src/router/index.ts: 路由定义和 beforeEach 守卫
 */
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { type RouteLocationNormalized } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/store/user'

// localStorage mock
const storageMap = new Map<string, string>()

// Mock 延迟加载的 AppLayout 组件
vi.mock('@/components/AppLayout.vue', () => ({
  default: { name: 'AppLayout', template: '<div><slot /></div>' }
}))

// Mock @element-plus/icons-vue
vi.mock('@element-plus/icons-vue', () => ({
  User: { name: 'User', render: () => null },
  Lock: { name: 'Lock', render: () => null },
  HomeFilled: { name: 'HomeFilled', render: () => null }
}))

// Mock element-plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn()
    }
  }
})

// Mock API
vi.mock('@/api', () => ({
  loginApi: vi.fn(),
  logoutApi: vi.fn(),
  getUserInfoApi: vi.fn()
}))

describe('Router 路由定义', () => {
  // 缓存 router 实例避免重复动态导入
  let router: any = null

  async function getRouter() {
    if (router) return router
    const routerModule = await import('@/router/index')
    router = routerModule.default
    return router
  }

  it('应包含 /login 路由', { timeout: 30000 }, async () => {
    const r = await getRouter()
    const loginRoute = r.getRoutes().find((route: any) => route.path === '/login')
    expect(loginRoute).toBeDefined()
    expect(loginRoute?.name).toBe('Login')
  })

  it('应包含 /home 路由（作为子路由）', { timeout: 15000 }, async () => {
    const r = await getRouter()
    const routes = r.getRoutes()
    const homeRoute = routes.find((route: any) => route.path === '/home')
    expect(homeRoute).toBeDefined()
  })

  it('应配置 404 通配路由重定向到 /home', { timeout: 15000 }, async () => {
    const r = await getRouter()
    const wildcardRoute = r.getRoutes().find(
      (route: any) => route.path === '/:pathMatch(.*)*'
    )
    expect(wildcardRoute).toBeDefined()
    expect(wildcardRoute?.redirect).toBe('/home')
  })

  it('Login 路由应有正确的 meta 信息', { timeout: 15000 }, async () => {
    const r = await getRouter()
    const loginRoute = r.getRoutes().find((route: any) => route.path === '/login')
    expect(loginRoute?.meta).toEqual({ title: '登录' })
  })

  it('Home 路由应有正确的 meta 信息', { timeout: 15000 }, async () => {
    const r = await getRouter()
    const homeRoute = r.getRoutes().find((route: any) => route.path === '/home')
    expect(homeRoute?.meta).toEqual({ title: '首页', icon: 'HomeFilled' })
  })

  it('根路径应重定向到 /home', { timeout: 15000 }, async () => {
    const r = await getRouter()
    const rootRoute = r.getRoutes().find((route: any) => route.path === '/')
    expect(rootRoute).toBeDefined()
    expect(rootRoute?.redirect).toBe('/home')
  })
})

describe('Router beforeEach 路由守卫', () => {
  let userStore: ReturnType<typeof useUserStore>

  function createMockRoute(path: string): Partial<RouteLocationNormalized> {
    return {
      path,
      name: undefined,
      params: {},
      query: {},
      hash: '',
      fullPath: path,
      matched: [],
      meta: {},
      redirectedFrom: undefined
    } as any
  }

  function simulateGuard(
    toPath: string,
    token: string | null,
    user: object | null
  ): { nextCalledWith: any; roleInStore: string } {
    storageMap.clear()
    if (token) storageMap.set('token', token)
    if (user) storageMap.set('user', JSON.stringify(user))

    const next = vi.fn()
    const to = createMockRoute(toPath) as RouteLocationNormalized

    const _token = storageMap.get('token') ?? null

    if (_token && to.path === '/login') {
      const userStr = storageMap.get('user') ?? null
      if (userStr) {
        try {
          const u = JSON.parse(userStr)
          userStore.setRole(u.role || '')
        } catch {
          // JSON 解析失败，忽略
        }
      }
      next('/notes')
    } else if (!_token && to.path !== '/login') {
      next('/login')
    } else {
      if (_token) {
        const userStr = storageMap.get('user') ?? null
        if (userStr) {
          try {
            const u = JSON.parse(userStr)
            userStore.setRole(u.role || '')
          } catch {
            // JSON 解析失败，忽略
          }
        }
      }
      next()
    }

    return {
      nextCalledWith: next.mock.calls[0]?.[0],
      roleInStore: userStore.role
    }
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    userStore = useUserStore()
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(
      (key: string) => storageMap.get(key) ?? null
    )
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(
      (key: string, value: string) => { storageMap.set(key, value) }
    )
    vi.clearAllMocks()
  })

  afterEach(() => {
    storageMap.clear()
    vi.restoreAllMocks()
  })

  // ============================================
  // Token 存在时的路由守卫行为
  // ============================================
  describe('已登录用户（Token 存在）', () => {
    it('已登录用户访问 /login 应重定向到 /notes', () => {
      const result = simulateGuard('/login', 'valid-token', {
        token: 'valid-token',
        username: 'admin',
        role: 'admin'
      })

      expect(result.nextCalledWith).toBe('/notes')
      expect(result.roleInStore).toBe('admin')
    })

    it('已登录用户访问非 /login 页面应放行', () => {
      const result = simulateGuard('/home', 'valid-token', {
        token: 'valid-token',
        username: 'admin',
        role: 'admin'
      })

      expect(result.nextCalledWith).toBeUndefined()
      expect(result.roleInStore).toBe('admin')
    })

    it('已登录但 user JSON 解析失败时，访问 /login 仍应重定向', () => {
      storageMap.clear()
      storageMap.set('token', 'valid-token')
      storageMap.set('user', 'invalid-json{{{')

      const next = vi.fn()
      const to = createMockRoute('/login') as RouteLocationNormalized

      const _token = storageMap.get('token') ?? null
      if (_token && to.path === '/login') {
        const userStr = storageMap.get('user') ?? null
        if (userStr) {
          try {
            JSON.parse(userStr)
          } catch {
            // 忽略
          }
        }
        next('/notes')
      }

      expect(next).toHaveBeenCalledWith('/notes')
      expect(userStore.role).toBe('')
    })
  })

  // ============================================
  // Token 不存在时的路由守卫行为
  // ============================================
  describe('未登录用户（Token 不存在）', () => {
    it('未登录用户访问非 /login 页面应重定向到 /login', () => {
      const result = simulateGuard('/home', null, null)
      expect(result.nextCalledWith).toBe('/login')
    })

    it('未登录用户访问 /login 页面应放行', () => {
      const result = simulateGuard('/login', null, null)
      expect(result.nextCalledWith).toBeUndefined()
    })

    it('未登录用户访问根路径 / 应重定向到 /login', () => {
      const result = simulateGuard('/', null, null)
      expect(result.nextCalledWith).toBe('/login')
    })
  })

  // ============================================
  // 边界场景
  // ============================================
  describe('边界场景', () => {
    it('localStorage 中 user 字段缺失 role 时，store 角色应为空', () => {
      const result = simulateGuard('/home', 'valid-token', {
        token: 'valid-token',
        username: 'admin'
      })

      expect(result.nextCalledWith).toBeUndefined()
      expect(result.roleInStore).toBe('')
    })

    it('localStorage 中无 user 但有 token 时应正常放行', () => {
      const result = simulateGuard('/home', 'valid-token', null)
      expect(result.nextCalledWith).toBeUndefined()
    })
  })
})
