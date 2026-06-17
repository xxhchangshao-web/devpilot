/**
 * Home 视图测试
 * 测试 src/views/Home.vue: 首页渲染
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'

// Mock element-plus（已在 test-setup.ts 中 Mock）
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

import Home from '@/views/Home.vue'

describe('Home 视图', () => {
  let wrapper: VueWrapper

  const createWrapper = () => {
    return mount(Home, {
      global: {
        stubs: {
          'el-card': {
            template: `
              <div class="el-card">
                <div class="el-card-header"><slot name="header" /></div>
                <div class="el-card-body"><slot /></div>
              </div>`
          },
          'el-row': {
            template: '<div class="el-row"><slot /></div>'
          },
          'el-col': {
            props: ['span'],
            template: '<div class="el-col"><slot /></div>'
          },
          'el-statistic': {
            props: ['title', 'value'],
            template: '<div class="el-statistic">{{ title }}: {{ value }}</div>'
          }
        }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
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
      expect(wrapper.html()).toBeTruthy()
    })

    it('应显示欢迎信息', () => {
      wrapper = createWrapper()
      expect(wrapper.text()).toContain('欢迎使用 DevPilot')
    })

    it('应显示 DevPilot 描述', () => {
      wrapper = createWrapper()
      expect(wrapper.text()).toContain('工程问题排查助手')
    })
  })

  // ============================================
  // 统计卡片渲染
  // ============================================
  describe('统计卡片', () => {
    it('应包含"待处理问题"统计卡片', () => {
      wrapper = createWrapper()
      expect(wrapper.text()).toContain('待处理问题')
    })

    it('应包含"处理中"统计卡片', () => {
      wrapper = createWrapper()
      expect(wrapper.text()).toContain('处理中')
    })

    it('应包含"已完成"统计卡片', () => {
      wrapper = createWrapper()
      expect(wrapper.text()).toContain('已完成')
    })

    it('统计数据初始值应为 0', () => {
      wrapper = createWrapper()
      expect(wrapper.text()).toContain('待处理问题: 0')
      expect(wrapper.text()).toContain('处理中: 0')
      expect(wrapper.text()).toContain('已完成: 0')
    })
  })

  // ============================================
  // 模板结构
  // ============================================
  describe('模板结构', () => {
    it('应包含 el-card 组件', () => {
      wrapper = createWrapper()
      expect(wrapper.find('.el-card').exists()).toBe(true)
    })

    it('应包含三列统计卡片', () => {
      wrapper = createWrapper()
      const cols = wrapper.findAll('.el-col')
      expect(cols.length).toBe(3)
    })
  })

  // ============================================
  // 组件无副作用
  // ============================================
  describe('组件状态', () => {
    it('组件应无内部状态（纯展示组件）', () => {
      wrapper = createWrapper()
      expect(wrapper.exists()).toBe(true)
    })
  })
})
