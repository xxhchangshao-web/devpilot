/**
 * NoteFormDialog 组件测试
 * 测试 src/views/notes/NoteFormDialog.vue: 新建/编辑笔记弹窗表单
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'

// ============================================
// Mock 外部依赖
// ============================================

// Mock @/api/tags - 使用 vi.hoisted 避免 hoisting 问题
const { mockGetTagListApi } = vi.hoisted(() => ({
  mockGetTagListApi: vi.fn(),
}))
vi.mock('@/api/tags', () => ({
  getTagListApi: mockGetTagListApi,
}))

// Mock element-plus
const { mockElMessageSuccess, mockElMessageError, mockElMessageWarning, mockElMessageBoxConfirm } = vi.hoisted(() => ({
  mockElMessageSuccess: vi.fn(),
  mockElMessageError: vi.fn(),
  mockElMessageWarning: vi.fn(),
  mockElMessageBoxConfirm: vi.fn().mockResolvedValue('confirm'),
}))
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: mockElMessageSuccess,
      error: mockElMessageError,
      warning: mockElMessageWarning,
      info: vi.fn(),
    },
    ElMessageBox: {
      confirm: mockElMessageBoxConfirm,
      alert: vi.fn(),
    },
  }
})

// Mock icons
vi.mock('@element-plus/icons-vue', () => {
  const createIcon = (name: string) => ({
    name,
    render: () => null,
    inheritAttrs: false,
  })
  return {
    Plus: createIcon('Plus'),
    ArrowLeft: createIcon('ArrowLeft'),
    Edit: createIcon('Edit'),
    Delete: createIcon('Delete'),
    ArrowDown: createIcon('ArrowDown'),
    User: createIcon('User'),
    Lock: createIcon('Lock'),
    HomeFilled: createIcon('HomeFilled'),
  }
})

import NoteFormDialog from './NoteFormDialog.vue'

// ============================================
// Stub 模板
// ============================================

const stubDialog = {
  props: ['modelValue', 'title', 'width', 'closeOnClickModal'],
  emits: ['update:modelValue', 'closed'],
  template: `
    <div v-if="modelValue" class="el-dialog-stub">
      <div class="el-dialog-title">{{ title }}</div>
      <div class="el-dialog-body"><slot /></div>
      <div class="el-dialog-footer"><slot name="footer" /></div>
    </div>
  `,
}

const stubForm = {
  props: ['model', 'rules', 'labelWidth', 'ref'],
  emits: [],
  template: '<div class="el-form-stub"><slot /></div>',
  methods: {
    validate(cb: any) { cb?.(true); return Promise.resolve(true) },
    clearValidate() {},
    resetFields() {},
  },
}

const stubFormItem = {
  props: ['label', 'prop', 'labelWidth'],
  template: '<div class="el-form-item-stub"><label>{{ label }}</label><slot /></div>',
}

const stubInput = {
  props: ['modelValue', 'placeholder', 'maxlength', 'showWordLimit', 'type', 'rows'],
  emits: ['update:modelValue', 'blur', 'focus'],
  template: '<input class="el-input-stub" :value="modelValue" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

const stubSelect = {
  props: ['modelValue', 'placeholder', 'multiple', 'filterable', 'collapseTags', 'clearable'],
  emits: ['update:modelValue', 'change'],
  template: '<select class="el-select-stub" :value="modelValue" :multiple="multiple" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
}

const stubOption = {
  props: ['label', 'value', 'key'],
  template: '<option :value="value">{{ label }}</option>',
}

const stubButton = {
  props: ['type', 'loading', 'disabled'],
  emits: ['click'],
  template: '<button class="el-button-stub" :disabled="loading || disabled" @click="$emit(\'click\')"><slot /></button>',
}

const stubRow = {
  props: ['gutter'],
  template: '<div class="el-row-stub"><slot /></div>',
}

const stubCol = {
  props: ['span'],
  template: '<div class="el-col-stub"><slot /></div>',
}

// ============================================
// 测试套件
// ============================================

describe('NoteFormDialog 组件', () => {
  let wrapper: VueWrapper<any>

  const createWrapper = (props = {}) => {
    return mount(NoteFormDialog, {
      props: {
        visible: true,
        submitting: false,
        initialData: null,
        ...props,
      },
      global: {
        stubs: {
          'el-dialog': stubDialog,
          'el-form': stubForm,
          'el-form-item': stubFormItem,
          'el-input': stubInput,
          'el-select': stubSelect,
          'el-option': stubOption,
          'el-button': stubButton,
          'el-row': stubRow,
          'el-col': stubCol,
        },
      },
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockGetTagListApi.mockResolvedValue({
      data: { records: [{ id: 1, name: 'Tag1' }, { id: 2, name: 'Tag2' }] },
    })
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
  })

  // ============================================
  // 基础渲染测试
  // ============================================
  describe('基础渲染', () => {
    it('visible 为 true 时应渲染对话框', () => {
      wrapper = createWrapper({ visible: true })
      expect(wrapper.find('.el-dialog-stub').exists()).toBe(true)
    })

    it('visible 为 false 时应隐藏对话框', () => {
      wrapper = createWrapper({ visible: false })
      expect(wrapper.find('.el-dialog-stub').exists()).toBe(false)
    })

    it('新建模式标题应为"新建笔记"', () => {
      wrapper = createWrapper({ visible: true, initialData: null })
      expect(wrapper.find('.el-dialog-title').text()).toBe('新建笔记')
    })

    it('编辑模式标题应为"编辑笔记"', () => {
      wrapper = createWrapper({
        visible: true,
        initialData: {
          title: 'Test Note',
          description: 'Desc',
          investigation: '',
          solution: '',
          category: 'OTHER',
          priority: 'P2',
          tagIds: [1],
        },
      })
      expect(wrapper.find('.el-dialog-title').text()).toBe('编辑笔记')
    })
  })

  // ============================================
  // 表单字段测试
  // ============================================
  describe('表单字段', () => {
    it('应包含标题字段', () => {
      wrapper = createWrapper()
      const labels = wrapper.findAll('.el-form-item-stub label')
      const titleLabel = labels.find(l => l.text() === '标题')
      expect(titleLabel).toBeTruthy()
    })

    it('应包含问题描述字段', () => {
      wrapper = createWrapper()
      const labels = wrapper.findAll('.el-form-item-stub label')
      const descLabel = labels.find(l => l.text() === '问题描述')
      expect(descLabel).toBeTruthy()
    })

    it('应包含分类和优先级字段', () => {
      wrapper = createWrapper()
      const labels = wrapper.findAll('.el-form-item-stub label')
      expect(labels.some(l => l.text() === '分类')).toBe(true)
      expect(labels.some(l => l.text() === '优先级')).toBe(true)
    })

    it('应包含标签选择字段', () => {
      wrapper = createWrapper()
      const labels = wrapper.findAll('.el-form-item-stub label')
      expect(labels.some(l => l.text() === '标签')).toBe(true)
    })
  })

  // ============================================
  // 表单初始化测试
  // ============================================
  describe('表单初始化', () => {
    it('新建模式应有默认值', () => {
      wrapper = createWrapper({ visible: true, initialData: null })
      expect(wrapper.vm.formData.category).toBe('OTHER')
      expect(wrapper.vm.formData.priority).toBe('P2')
      expect(wrapper.vm.formData.status).toBe('OPEN')
      expect(wrapper.vm.formData.title).toBe('')
      expect(wrapper.vm.formData.description).toBe('')
      expect(wrapper.vm.formData.tagIds).toEqual([])
    })

    it('编辑模式应填充 initialData', async () => {
      const initialData = {
        title: '问题标题',
        description: '问题描述内容',
        investigation: '排查步骤',
        solution: '解决方案',
        category: 'EXCEPTION',
        priority: 'P1',
        tagIds: [1, 2],
        status: 'IN_PROGRESS',
      }
      wrapper = createWrapper({ visible: true, initialData })
      await nextTick()

      expect(wrapper.vm.formData.title).toBe('问题标题')
      expect(wrapper.vm.formData.description).toBe('问题描述内容')
      expect(wrapper.vm.formData.investigation).toBe('排查步骤')
      expect(wrapper.vm.formData.solution).toBe('解决方案')
      expect(wrapper.vm.formData.category).toBe('EXCEPTION')
      expect(wrapper.vm.formData.priority).toBe('P1')
      expect(wrapper.vm.formData.tagIds).toEqual([1, 2])
    })

    it('编辑模式应设置 isEdit 为 true', async () => {
      wrapper = createWrapper({
        visible: true,
        initialData: {
          title: 'T',
          description: 'D',
          category: 'OTHER',
          priority: 'P2',
          tagIds: [],
        },
      })
      await nextTick()
      expect(wrapper.vm.isEdit).toBe(true)
    })

    it('编辑模式下 tagIds 应为 initialData 的副本（非同一引用）', async () => {
      const tagIds = [1, 2, 3]
      wrapper = createWrapper({
        visible: true,
        initialData: {
          title: 'T',
          description: 'D',
          category: 'OTHER',
          priority: 'P2',
          tagIds,
        },
      })
      await nextTick()
      expect(wrapper.vm.formData.tagIds).toEqual([1, 2, 3])
      expect(wrapper.vm.formData.tagIds).not.toBe(tagIds)
    })
  })

  // ============================================
  // 标签加载测试
  // ============================================
  describe('标签加载', () => {
    it('visible 变为 true 时应加载标签列表', async () => {
      wrapper = createWrapper({ visible: true })
      await nextTick()
      await nextTick()

      expect(mockGetTagListApi).toHaveBeenCalled()
    })

    it('标签加载成功应更新 tagList', async () => {
      wrapper = createWrapper({ visible: true })
      await nextTick()
      await nextTick()
      await nextTick()

      expect(wrapper.vm.tagList).toEqual([
        { id: 1, name: 'Tag1' }, { id: 2, name: 'Tag2' },
      ])
    })

    it('标签加载失败应使用空列表', async () => {
      mockGetTagListApi.mockRejectedValueOnce(new Error('Network error'))
      wrapper = createWrapper({ visible: true })
      await nextTick()
      await nextTick()
      await nextTick()

      expect(wrapper.vm.tagList).toEqual([])
    })
  })

  // ============================================
  // 表单验证测试
  // ============================================
  describe('表单验证规则', () => {
    it('标题为必填项', () => {
      wrapper = createWrapper()
      const rules = wrapper.vm.rules
      expect(rules.title).toBeDefined()
      const titleRules = rules.title as any[]
      expect(titleRules.some((r: any) => r.required)).toBe(true)
    })

    it('描述为必填项', () => {
      wrapper = createWrapper()
      const rules = wrapper.vm.rules
      const descRules = rules.description as any[]
      expect(descRules.some((r: any) => r.required)).toBe(true)
    })

    it('分类为必选项', () => {
      wrapper = createWrapper()
      const rules = wrapper.vm.rules
      const catRules = rules.category as any[]
      expect(catRules.some((r: any) => r.required)).toBe(true)
    })

    it('优先级为必选项', () => {
      wrapper = createWrapper()
      const rules = wrapper.vm.rules
      const priRules = rules.priority as any[]
      expect(priRules.some((r: any) => r.required)).toBe(true)
    })

    it('标题长度限制 1-200 字符', () => {
      wrapper = createWrapper()
      const titleRules = wrapper.vm.rules.title as any[]
      const minMaxRule = titleRules.find((r: any) => r.min !== undefined)
      expect(minMaxRule?.min).toBe(1)
      expect(minMaxRule?.max).toBe(200)
    })

    it('描述最大长度 10000 字符', () => {
      wrapper = createWrapper()
      const descRules = wrapper.vm.rules.description as any[]
      const maxRule = descRules.find((r: any) => r.max !== undefined)
      expect(maxRule?.max).toBe(10000)
    })
  })

  // ============================================
  // 表单提交测试
  // ============================================
  describe('表单提交', () => {
    it('验证通过应 emit submit 事件', async () => {
      wrapper = createWrapper({ visible: true })

      // 填充表单
      wrapper.vm.formData.title = '测试标题'
      wrapper.vm.formData.description = '测试描述'
      wrapper.vm.formData.category = 'EXCEPTION'
      wrapper.vm.formData.priority = 'P1'
      wrapper.vm.formData.tagIds = [1, 2]
      wrapper.vm.formData.status = 'OPEN'

      await wrapper.find('.el-dialog-footer button[class*="el-button-stub"]:not(:first-child)').trigger('click')
      await nextTick()

      expect(wrapper.emitted('submit')).toBeTruthy()
      const submittedData = wrapper.emitted('submit')![0][0] as {
        title: string
        description: string
        investigation: string
        solution: string
        category: string
        priority: string
        tagIds: number[]
        status: string
      }
      expect(submittedData.title).toBe('测试标题')
      expect(submittedData.description).toBe('测试描述')
      expect(submittedData.category).toBe('EXCEPTION')
      expect(submittedData.priority).toBe('P1')
      expect(submittedData.tagIds).toEqual([1, 2])
      expect(submittedData.status).toBe('OPEN')
    })

    it('提交后 submitting prop 应由父组件控制，子组件不修改', async () => {
      wrapper = createWrapper({ visible: true, submitting: true })
      wrapper.vm.formData.title = 'T'
      wrapper.vm.formData.description = 'D'

      await wrapper.find('.el-dialog-footer button[class*="el-button-stub"]:not(:first-child)').trigger('click')
      await nextTick()

      // submitting 为 prop，子组件不应修改它
      expect(wrapper.props('submitting')).toBe(true)
    })

    it('点击取消按钮应 emit update:visible(false)', async () => {
      wrapper = createWrapper({ visible: true })
      await wrapper.find('.el-dialog-footer button[class*="el-button-stub"]:first-child').trigger('click')
      await nextTick()

      expect(wrapper.emitted('update:visible')).toBeTruthy()
      expect(wrapper.emitted('update:visible')![0]).toEqual([false])
    })
  })

  // ============================================
  // 弹窗关闭重置测试
  // ============================================
  describe('弹窗关闭重置', () => {
    it('closed 事件应重置表单并恢复 isEdit', async () => {
      wrapper = createWrapper({
        visible: true,
        initialData: {
          title: '编辑中',
          description: 'Desc',
          category: 'PERFORMANCE',
          priority: 'P0',
          tagIds: [1, 2],
        },
      })
      await nextTick()

      // 触发 closed
      wrapper.findComponent(stubDialog).vm.$emit('closed')
      await nextTick()

      expect(wrapper.vm.formData.title).toBe('')
      expect(wrapper.vm.formData.description).toBe('')
      expect(wrapper.vm.formData.category).toBe('OTHER')
      expect(wrapper.vm.formData.priority).toBe('P2')
      expect(wrapper.vm.formData.tagIds).toEqual([])
      expect(wrapper.vm.formData.status).toBe('OPEN')
      expect(wrapper.vm.isEdit).toBe(false)
    })
  })

  // ============================================
  // visible 变化监听测试
  // ============================================
  describe('visible 变化监听', () => {
    it('从 false 变为 true 新建模式应重置表单', async () => {
      wrapper = createWrapper({ visible: false })
      expect(wrapper.vm.isEdit).toBe(false)

      await wrapper.setProps({ visible: true, initialData: null })
      await nextTick()

      expect(wrapper.vm.formData.category).toBe('OTHER')
      expect(wrapper.vm.formData.priority).toBe('P2')
      expect(wrapper.vm.isEdit).toBe(false)
    })

    it('从 false 变为 true 编辑模式应填充数据', async () => {
      wrapper = createWrapper({ visible: false })
      await wrapper.setProps({
        visible: true,
        initialData: {
          title: '编辑标题',
          description: '编辑描述',
          category: 'CONFIG',
          priority: 'P3',
          tagIds: [3],
        },
      })
      await nextTick()

      expect(wrapper.vm.isEdit).toBe(true)
      expect(wrapper.vm.formData.title).toBe('编辑标题')
      expect(wrapper.vm.formData.description).toBe('编辑描述')
      expect(wrapper.vm.formData.category).toBe('CONFIG')
    })

    it('visible 变为 false 不应触发初始化', async () => {
      wrapper = createWrapper({ visible: true })
      await nextTick()

      // 修改表单数据
      wrapper.vm.formData.title = 'Modified'
      await wrapper.setProps({ visible: false })
      await nextTick()

      // 数据不会被重置（handleClosed 监听的是 el-dialog 的 closed 事件）
      expect(wrapper.vm.formData.title).toBe('Modified')
    })
  })

  // ============================================
  // 模式切换测试
  // ============================================
  describe('模式切换', () => {
    it('从编辑切换到新建应重置所有字段', async () => {
      wrapper = createWrapper({
        visible: true,
        initialData: {
          title: '旧标题',
          description: '旧描述',
          category: 'CONFIG',
          priority: 'P3',
          tagIds: [],
        },
      })
      await nextTick()
      expect(wrapper.vm.isEdit).toBe(true)

      // 模拟关闭后重新打开（新建模式）
      wrapper.findComponent(stubDialog).vm.$emit('closed')
      await nextTick()
      await wrapper.setProps({ visible: true, initialData: null })
      await nextTick()

      expect(wrapper.vm.isEdit).toBe(false)
      expect(wrapper.vm.formData.title).toBe('')
    })
  })
})
