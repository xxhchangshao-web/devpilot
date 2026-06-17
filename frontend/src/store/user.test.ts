/**
 * User Store 测试
 * 测试 src/store/user.ts 的角色状态管理
 */
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/store/user'

describe('useUserStore', () => {
  let store: ReturnType<typeof useUserStore>

  // 手动重置 store 状态（Pinia setup syntax 无 $reset）
  function resetStore() {
    store.clear()
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useUserStore()
  })

  afterEach(() => {
    resetStore()
  })

  // ============================================
  // 初始状态测试
  // ============================================
  describe('initial state', () => {
    it('role 初始值应为空字符串', () => {
      expect(store.role).toBe('')
    })
  })

  // ============================================
  // setRole 操作测试
  // ============================================
  describe('setRole', () => {
    it('应正确设置角色为 admin', () => {
      store.setRole('admin')
      expect(store.role).toBe('admin')
    })

    it('应正确设置角色为 user', () => {
      store.setRole('user')
      expect(store.role).toBe('user')
    })

    it('应正确设置角色为 guest', () => {
      store.setRole('guest')
      expect(store.role).toBe('guest')
    })

    it('应能设置空字符串角色', () => {
      store.setRole('admin')
      expect(store.role).toBe('admin')

      store.setRole('')
      expect(store.role).toBe('')
    })
  })

  // ============================================
  // clear 操作测试
  // ============================================
  describe('clear', () => {
    it('应将角色重置为空字符串', () => {
      store.setRole('admin')
      expect(store.role).toBe('admin')

      store.clear()
      expect(store.role).toBe('')
    })

    it('在角色已为空时调用 clear 仍保持空字符串', () => {
      expect(store.role).toBe('')
      store.clear()
      expect(store.role).toBe('')
    })
  })

  // ============================================
  // 状态重置测试
  // ============================================
  describe('状态重置', () => {
    it('手动重置应恢复所有状态到初始值', () => {
      store.setRole('admin')

      resetStore()

      expect(store.role).toBe('')
    })
  })

  // ============================================
  // 边界与组合场景
  // ============================================
  describe('组合场景', () => {
    it('setRole → clear → 再 setRole 应正确工作', () => {
      store.setRole('admin')
      store.clear()
      store.setRole('viewer')

      expect(store.role).toBe('viewer')
    })

    it('多次 setRole 以最后一次为准', () => {
      store.setRole('admin')
      store.setRole('editor')
      store.setRole('viewer')

      expect(store.role).toBe('viewer')
    })

    it('独立 Store 实例之间隔离', () => {
      store.setRole('admin')
      expect(store.role).toBe('admin')

      // 创建新 Pinia 实例和 Store
      const pinia2 = createPinia()
      setActivePinia(pinia2)
      const store2 = useUserStore()
      expect(store2.role).toBe('')
    })
  })
})
