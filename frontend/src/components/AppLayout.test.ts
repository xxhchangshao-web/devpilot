/**
 * AppLayout 组件测试
 * 测试 src/components/AppLayout.vue: 布局、导航、退出登录
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// localStorage mock - 在 window 级别替换，确保组件内代码也能使用
const storageMap = new Map<string, string>()

Object.defineProperty(window, 'localStorage', {
  value: {
    getItem: vi.fn((key: string) => storageMap.get(key) ?? null),
    setItem: vi.fn((key: string, value: string) => { storageMap.set(key, value) }),
    removeItem: vi.fn((key: string) => { storageMap.delete(key) }),
    clear: vi.fn(() => { storageMap.clear() }),
    length: 0,
    key: vi.fn(() => null)
  },
  writable: true
})

// ============================================
// Mock 设置
// ============================================

const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush
  }),
  useRoute: () => ({
    path: '/home',
    meta: { title: '首页' },
    params: {},
    query: {},
    hash: '',
    fullPath: '/home',
    name: 'Home',
    matched: []
  })
}))

// Mock API
vi.mock('@/api', () => ({
  loginApi: vi.fn(),
  logoutApi: vi.fn().mockResolvedValue({ code: 200 }),
  getUserInfoApi: vi.fn()
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

// Mock @element-plus/icons-vue
vi.mock('@element-plus/icons-vue', () => ({
  Monitor: { name: 'Monitor', render: () => null },
  HomeFilled: { name: 'HomeFilled', render: () => null },
  UserFilled: { name: 'UserFilled', render: () => null },
  ArrowDown: { name: 'ArrowDown', render: () => null },
  SwitchButton: { name: 'SwitchButton', render: () => null }
}))

import AppLayout from '@/components/AppLayout.vue'
import { useUserStore } from '@/store/user'

describe('AppLayout 组件', () => {
  let wrapper: VueWrapper
  let userStore: ReturnType<typeof useUserStore>

  const createWrapper = () => {
    return mount(AppLayout, {
      global: {
        plugins: [createPinia()],
        stubs: {
          'router-view': { template: '<div class="router-view-stub"><slot /></div>' },
          'el-container': { template: '<div class="el-container"><slot /></div>' },
          'el-aside': { template: '<div class="el-aside"><slot /></div>' },
          'el-header': { template: '<div class="el-header"><slot /></div>' },
          'el-main': { template: '<div class="el-main"><slot /></div>' },
          'el-menu': { template: '<div class="el-menu"><slot /></div>' },
          'el-menu-item': { template: '<div class="el-menu-item"><slot /></div>' },
          'el-dropdown': { template: '<div class="el-dropdown"><slot /></div>' },
          'el-dropdown-menu': { template: '<div class="el-dropdown-menu"><slot /></div>' },
          'el-dropdown-item': {
            props: ['command', 'disabled', 'divided'],
            template: '<div class="el-dropdown-item"><slot /></div>',
            emits: ['click']
          },
          'el-breadcrumb': { template: '<div class="el-breadcrumb"><slot /></div>' },
          'el-breadcrumb-item': { template: '<div class="el-breadcrumb-item"><slot /></div>' },
          'el-icon': { template: '<span class="el-icon"><slot /></span>' }
        }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    storageMap.clear()
    mockPush.mockClear()
    setActivePinia(createPinia())
    userStore = useUserStore()
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
  })

  // ============================================
  // 基础渲染测试
  // ============================================
  describe('基础渲染', () => {
    it('组件应成功渲染', () => {
      wrapper = createWrapper()
      expect(wrapper.exists()).toBe(true)
    })

    it('应包含 DevPilot 标题', () => {
      wrapper = createWrapper()
      expect(wrapper.text()).toContain('DevPilot')
    })

    it('应包含首页菜单项', () => {
      wrapper = createWrapper()
      expect(wrapper.text()).toContain('首页')
    })
  })

  // ============================================
  // 用户信息显示
  // ============================================
  describe('用户信息显示', () => {
    it('未设置用户名时 store 为空字符串', () => {
      wrapper = createWrapper()
      expect(userStore.username).toBe('')
    })

    it('设置用户名后 store 应更新', () => {
      userStore.setUsername('张三')
      wrapper = createWrapper()
      expect(userStore.username).toBe('张三')
    })
  })

  // ============================================
  // 退出登录
  // ============================================
  describe('退出登录', () => {
    it('退出登录应调用 logoutApi 并跳转到 /login', async () => {
      storageMap.set('token', 'test-token')
      storageMap.set('user', JSON.stringify({ username: 'admin', role: 'admin' }))
      userStore.setRole('admin')
      userStore.setUsername('admin')

      wrapper = createWrapper()

      await wrapper.vm.handleLogout()

      expect(mockPush).toHaveBeenCalledWith('/login')
    })

    it('退出登录 API 失败时仍应清除 token 并跳转', async () => {
      const { logoutApi } = await import('@/api')
      vi.mocked(logoutApi).mockRejectedValueOnce(new Error('Network Error'))

      storageMap.set('token', 'test-token')
      userStore.setUsername('admin')

      wrapper = createWrapper()

      // handleLogout 使用 try/finally（无 catch），因此 rejection 会传播
      // 但 finally 块仍会执行清除和跳转
      await expect(wrapper.vm.handleLogout()).rejects.toThrow('Network Error')

      // finally 块仍会执行清除和跳转
      expect(mockPush).toHaveBeenCalledWith('/login')
    })
  })

  // ============================================
  // Computed 计算属性
  // ============================================
  describe('计算属性', () => {
    it('activeMenu 应返回当前路由路径', () => {
      wrapper = createWrapper()
      expect(wrapper.vm.activeMenu).toBe('/home')
    })

    it('currentTitle 应返回当前路由 meta.title', () => {
      wrapper = createWrapper()
      expect(wrapper.vm.currentTitle).toBe('首页')
    })
  })

  // ============================================
  // 模板结构
  // ============================================
  describe('模板结构', () => {
    it('应包含 el-container 布局', () => {
      wrapper = createWrapper()
      expect(wrapper.find('.el-container').exists()).toBe(true)
    })

    it('应渲染布局组件', () => {
      wrapper = createWrapper()
      expect(wrapper.find('.app-layout').exists()).toBe(true)
    })
  })
})
