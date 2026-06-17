import { vi } from 'vitest'

// Mock Element Plus 消息提示
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
  User: { name: 'User', render: () => null },
  Lock: { name: 'Lock', render: () => null },
  HomeFilled: { name: 'HomeFilled', render: () => null }
}))
