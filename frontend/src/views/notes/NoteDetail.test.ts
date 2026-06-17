/**
 * NoteDetail 组件测试
 * 测试 src/views/notes/NoteDetail.vue: 笔记详情及操作
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, shallowMount, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'

// ============================================
// Mock 外部依赖
// ============================================

const { mockGetNoteDetailApi, mockUpdateNoteApi, mockDeleteNoteApi, mockChangeNoteStatusApi } = vi.hoisted(() => ({
  mockGetNoteDetailApi: vi.fn(),
  mockUpdateNoteApi: vi.fn(),
  mockDeleteNoteApi: vi.fn(),
  mockChangeNoteStatusApi: vi.fn(),
}))
vi.mock('@/api/notes', () => ({
  getNoteDetailApi: mockGetNoteDetailApi,
  updateNoteApi: mockUpdateNoteApi,
  deleteNoteApi: mockDeleteNoteApi,
  changeNoteStatusApi: mockChangeNoteStatusApi,
}))

const { mockPush, mockReplace, routeParamsRef } = vi.hoisted(() => ({
  mockPush: vi.fn(),
  mockReplace: vi.fn(),
  routeParamsRef: { id: '1' } as { id: string },
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
    back: vi.fn(),
  }),
  useRoute: () => ({
    get params() { return routeParamsRef },
    query: {},
    path: '/notes/1',
    name: 'NoteDetail',
  }),
}))

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

vi.mock('@element-plus/icons-vue', () => {
  const createIcon = (name: string) => ({ name, render: () => null, inheritAttrs: false })
  return {
    ArrowLeft: createIcon('ArrowLeft'),
    Edit: createIcon('Edit'),
    Delete: createIcon('Delete'),
    ArrowDown: createIcon('ArrowDown'),
    Plus: createIcon('Plus'),
    User: createIcon('User'),
    Lock: createIcon('Lock'),
    HomeFilled: createIcon('HomeFilled'),
  }
})

vi.mock('@/utils/format', () => ({
  formatFullDateTime: vi.fn((d: string) => d ? '2024-01-15 10:30:00' : ''),
}))

import NoteDetail from './NoteDetail.vue'

// ============================================
// 模拟数据
// ============================================

const mockNoteDetail = {
  id: 1,
  title: '测试问题笔记',
  description: '这是一个测试问题的描述',
  investigation: '排查步骤详细记录',
  solution: '最终解决方案',
  status: 'OPEN',
  priority: 'P1',
  category: 'EXCEPTION',
  tags: [
    { id: 1, name: '后端' },
    { id: 2, name: '紧急' },
  ],
  creatorId: 100,
  creatorName: '张三',
  created_at: '2024-01-15T10:00:00Z',
  updated_at: '2024-01-15T10:30:00Z',
  canEdit: true,
}

// ============================================
// Stub 模板
// ============================================

const stubCard = {
  props: ['shadow'],
  template: '<div class="el-card-stub"><div><slot name="header" /></div><div><slot /></div></div>',
}

const stubButton = {
  props: ['type', 'loading', 'disabled'],
  emits: ['click'],
  template: '<button class="el-button-stub" :disabled="loading || disabled" @click="$emit(\'click\')"><slot /></button>',
}

const stubSkeleton = {
  props: ['rows', 'animated', 'loading'],
  template: '<div class="el-skeleton-stub">skeleton</div>',
}

const stubEmpty = {
  props: ['description'],
  template: '<div class="el-empty-stub">{{ description }}</div>',
}

const stubTag = {
  props: ['type', 'size'],
  template: '<span class="el-tag-stub"><slot /></span>',
}

const stubDescriptions = {
  props: ['column', 'border'],
  template: '<div class="el-descriptions-stub"><slot /></div>',
}

const stubDescriptionsItem = {
  props: ['label', 'span'],
  template: '<div class="el-descriptions-item-stub"><span class="desc-label">{{ label }}</span><slot /></div>',
}

const stubDropdown = {
  props: ['command'],
  emits: ['command'],
  template: '<div class="el-dropdown-stub" @command="$emit(\'command\', $event)"><slot /><slot name="dropdown" /></div>',
}

const stubDropdownMenu = {
  template: '<div class="el-dropdown-menu-stub"><slot /></div>',
}

const stubDropdownItem = {
  props: ['command'],
  template: '<div class="el-dropdown-item-stub" :data-command="command"><slot /></div>',
}

const stubDialog = {
  props: ['modelValue', 'title', 'width', 'closeOnClickModal'],
  emits: ['update:modelValue'],
  template: '<div v-if="modelValue" class="el-dialog-stub"><div><slot /></div><div><slot name="footer" /></div></div>',
}

// NoteFormDialog stub
const stubNoteFormDialog = {
  props: ['visible', 'initialData'],
  emits: ['update:visible', 'submit'],
  template: '<div class="note-form-dialog-stub" v-if="visible"><button class="submit-btn" @click="$emit(\'submit\', {})">submit</button></div>',
}

// ============================================
// 测试套件
// ============================================

describe('NoteDetail 组件', () => {
  let wrapper: VueWrapper<any>

  const createWrapper = () => {
    return mount(NoteDetail, {
      global: {
        stubs: {
          'el-card': stubCard,
          'el-button': stubButton,
          'el-skeleton': stubSkeleton,
          'el-empty': stubEmpty,
          'el-tag': stubTag,
          'el-descriptions': stubDescriptions,
          'el-descriptions-item': stubDescriptionsItem,
          'el-dropdown': stubDropdown,
          'el-dropdown-menu': stubDropdownMenu,
          'el-dropdown-item': stubDropdownItem,
          'el-dialog': stubDialog,
          'el-icon': { template: '<span class="el-icon-stub"><slot /></span>' },
          'NoteFormDialog': stubNoteFormDialog,
        },
      },
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    routeParamsRef.id = '1'
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

    it('应显示返回按钮', () => {
      wrapper = createWrapper()
      expect(wrapper.text()).toContain('返回列表')
    })
  })

  // ============================================
  // 加载状态测试
  // ============================================
  describe('加载状态', () => {
    it('初始状态应显示骨架屏', async () => {
      mockGetNoteDetailApi.mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve({ data: mockNoteDetail }), 100))
      )
      wrapper = createWrapper()
      await nextTick()

      expect(wrapper.find('.el-skeleton-stub').exists()).toBe(true)
    })

    it('加载完成后应隐藏骨架屏', async () => {
      mockGetNoteDetailApi.mockResolvedValue({ data: mockNoteDetail })
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()

      expect(wrapper.find('.el-skeleton-stub').exists()).toBe(false)
    })

    it('API 调用成功 loading 应为 false', async () => {
      mockGetNoteDetailApi.mockResolvedValue({ data: mockNoteDetail })
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()

      expect(wrapper.vm.loading).toBe(false)
    })
  })

  // ============================================
  // 笔记不存在测试
  // ============================================
  describe('笔记不存在', () => {
    it('API 返回 404 错误应显示不存在提示', async () => {
      mockGetNoteDetailApi.mockRejectedValue(new Error('笔记不存在或已被删除'))
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()

      expect(wrapper.find('.el-empty-stub').exists()).toBe(true)
      expect(wrapper.text()).toContain('笔记不存在或已被删除')
    })

    it('笔记不存在时应调用 ElMessage.error', async () => {
      mockGetNoteDetailApi.mockRejectedValue(new Error('笔记不存在或已被删除'))
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()

      expect(mockElMessageError).toHaveBeenCalled()
    })
  })

  // ============================================
  // 笔记详情显示测试
  // ============================================
  describe('笔记详情显示', () => {
    beforeEach(async () => {
      mockGetNoteDetailApi.mockResolvedValue({ data: mockNoteDetail })
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()
    })

    it('应显示笔记标题', () => {
      expect(wrapper.text()).toContain('测试问题笔记')
    })

    it('应显示问题描述', () => {
      expect(wrapper.text()).toContain('这是一个测试问题的描述')
    })

    it('应显示排查过程', () => {
      expect(wrapper.text()).toContain('排查步骤详细记录')
    })

    it('应显示解决方案', () => {
      expect(wrapper.text()).toContain('最终解决方案')
    })

    it('应显示创建者', () => {
      expect(wrapper.text()).toContain('张三')
    })

    it('应显示状态标签', () => {
      const tags = wrapper.findAll('.el-tag-stub')
      // 至少应有状态和优先级两个标签
      expect(tags.length).toBeGreaterThanOrEqual(2)
    })

    it('应显示标签列表', () => {
      expect(wrapper.text()).toContain('后端')
      expect(wrapper.text()).toContain('紧急')
    })

    it('canEdit 为 true 时应显示操作按钮', () => {
      expect(wrapper.text()).toContain('编辑')
      expect(wrapper.text()).toContain('更改状态')
      expect(wrapper.text()).toContain('删除')
    })

    it('canEdit 为 false 时不应显示操作按钮', async () => {
      wrapper.unmount()
      mockGetNoteDetailApi.mockResolvedValue({
        data: { ...mockNoteDetail, canEdit: false },
      })
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()

      expect(wrapper.text()).not.toContain('编辑')
      expect(wrapper.text()).not.toContain('删除')
    })
  })

  // ============================================
  // 枚举映射函数测试
  // ============================================
  describe('枚举映射函数', () => {
    beforeEach(async () => {
      mockGetNoteDetailApi.mockResolvedValue({ data: mockNoteDetail })
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()
    })

    it('getStatusLabel - 应返回正确的中文标签', () => {
      expect(wrapper.vm.getStatusLabel('OPEN')).toBe('待解决')
      expect(wrapper.vm.getStatusLabel('IN_PROGRESS')).toBe('排查中')
      expect(wrapper.vm.getStatusLabel('RESOLVED')).toBe('已解决')
      expect(wrapper.vm.getStatusLabel('ARCHIVED')).toBe('已归档')
    })

    it('getStatusLabel - 未知状态应返回原值', () => {
      expect(wrapper.vm.getStatusLabel('UNKNOWN')).toBe('UNKNOWN')
    })

    it('getStatusTagType - 应返回正确的 tag 类型', () => {
      expect(wrapper.vm.getStatusTagType('OPEN')).toBe('primary')
      expect(wrapper.vm.getStatusTagType('IN_PROGRESS')).toBe('warning')
      expect(wrapper.vm.getStatusTagType('RESOLVED')).toBe('success')
      expect(wrapper.vm.getStatusTagType('ARCHIVED')).toBe('info')
    })

    it('getStatusTagType - 未知状态应返回 info', () => {
      expect(wrapper.vm.getStatusTagType('UNKNOWN')).toBe('info')
    })

    it('getPriorityLabel - 应返回正确的中文标签', () => {
      expect(wrapper.vm.getPriorityLabel('P0')).toBe('P0-紧急')
      expect(wrapper.vm.getPriorityLabel('P1')).toBe('P1-高')
      expect(wrapper.vm.getPriorityLabel('P2')).toBe('P2-中')
      expect(wrapper.vm.getPriorityLabel('P3')).toBe('P3-低')
    })

    it('getPriorityLabel - 未知优先级应返回原值', () => {
      expect(wrapper.vm.getPriorityLabel('P9')).toBe('P9')
    })

    it('getPriorityTagType - 应返回正确的 tag 类型', () => {
      expect(wrapper.vm.getPriorityTagType('P0')).toBe('danger')
      expect(wrapper.vm.getPriorityTagType('P1')).toBe('warning')
      expect(wrapper.vm.getPriorityTagType('P2')).toBe('primary')
      expect(wrapper.vm.getPriorityTagType('P3')).toBe('info')
    })

    it('getCategoryLabel - 应返回正确的分类标签', () => {
      expect(wrapper.vm.getCategoryLabel('PERFORMANCE')).toBe('性能问题')
      expect(wrapper.vm.getCategoryLabel('EXCEPTION')).toBe('异常报错')
      expect(wrapper.vm.getCategoryLabel('CONFIG')).toBe('配置问题')
      expect(wrapper.vm.getCategoryLabel('ENVIRONMENT')).toBe('环境问题')
      expect(wrapper.vm.getCategoryLabel('BUSINESS_LOGIC')).toBe('业务逻辑')
      expect(wrapper.vm.getCategoryLabel('OTHER')).toBe('其他')
    })

    it('getCategoryLabel - 未知分类应返回原值', () => {
      expect(wrapper.vm.getCategoryLabel('UNKNOWN_CAT')).toBe('UNKNOWN_CAT')
    })
  })

  // ============================================
  // 计算属性测试
  // ============================================
  describe('计算属性', () => {
    beforeEach(async () => {
      mockGetNoteDetailApi.mockResolvedValue({ data: mockNoteDetail })
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()
    })

    it('editFormData - 应从 note 数据正确转换', () => {
      const formData = wrapper.vm.editFormData
      expect(formData).not.toBeNull()
      expect(formData.title).toBe('测试问题笔记')
      expect(formData.description).toBe('这是一个测试问题的描述')
      expect(formData.investigation).toBe('排查步骤详细记录')
      expect(formData.solution).toBe('最终解决方案')
      expect(formData.category).toBe('EXCEPTION')
      expect(formData.priority).toBe('P1')
      expect(formData.tagIds).toEqual([1, 2])
    })

    it('editFormData - note 为 null 时应返回 null', async () => {
      wrapper.vm.note = null
      await nextTick()
      expect(wrapper.vm.editFormData).toBeNull()
    })

    it('availableStatuses - OPEN 状态应可流转到 IN_PROGRESS 和 ARCHIVED', () => {
      wrapper.vm.note = { ...mockNoteDetail, status: 'OPEN' }
      const statuses = wrapper.vm.availableStatuses
      const values = statuses.map((s: any) => s.value)
      expect(values).toContain('IN_PROGRESS')
      expect(values).toContain('ARCHIVED')
      expect(values).not.toContain('OPEN')
      expect(values).not.toContain('RESOLVED')
    })

    it('availableStatuses - IN_PROGRESS 应可流转到 RESOLVED 和 OPEN', () => {
      wrapper.vm.note = { ...mockNoteDetail, status: 'IN_PROGRESS' }
      const statuses = wrapper.vm.availableStatuses
      const values = statuses.map((s: any) => s.value)
      expect(values).toContain('RESOLVED')
      expect(values).toContain('OPEN')
    })

    it('availableStatuses - RESOLVED 应可流转到 OPEN 和 ARCHIVED', () => {
      wrapper.vm.note = { ...mockNoteDetail, status: 'RESOLVED' }
      const statuses = wrapper.vm.availableStatuses
      const values = statuses.map((s: any) => s.value)
      expect(values).toContain('OPEN')
      expect(values).toContain('ARCHIVED')
    })

    it('availableStatuses - ARCHIVED 终态不可变更', () => {
      wrapper.vm.note = { ...mockNoteDetail, status: 'ARCHIVED' }
      expect(wrapper.vm.availableStatuses).toEqual([])
    })

    it('availableStatuses - note 为 null 时应返回空数组', () => {
      wrapper.vm.note = null
      expect(wrapper.vm.availableStatuses).toEqual([])
    })
  })

  // ============================================
  // 导航测试
  // ============================================
  describe('导航', () => {
    it('goBack 应调用 router.push(\'/notes\')', () => {
      wrapper = createWrapper()
      wrapper.vm.goBack()
      expect(mockPush).toHaveBeenCalledWith('/notes')
    })

    it('无效的 ID 应跳转到列表页', async () => {
      routeParamsRef.id = ''  // 无效 ID
      wrapper = createWrapper()
      await nextTick()

      expect(mockPush).toHaveBeenCalledWith('/notes')
    })
  })

  // ============================================
  // 编辑操作测试
  // ============================================
  describe('编辑操作', () => {
    beforeEach(async () => {
      mockGetNoteDetailApi.mockResolvedValue({ data: mockNoteDetail })
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()
    })

    it('handleEdit 应打开编辑弹窗', () => {
      expect(wrapper.vm.editDialogVisible).toBe(false)
      wrapper.vm.handleEdit()
      expect(wrapper.vm.editDialogVisible).toBe(true)
    })

    it('handleEditSubmit - 应调用 updateNoteApi', async () => {
      mockUpdateNoteApi.mockResolvedValue({ data: { id: 1 } })
      const submitData = {
        title: '更新标题',
        description: '更新描述',
        investigation: '',
        solution: '',
        category: 'CONFIG',
        priority: 'P3',
        tagIds: [3],
        status: 'OPEN',
      }

      await wrapper.vm.handleEditSubmit(submitData)
      await nextTick()

      expect(mockUpdateNoteApi).toHaveBeenCalledWith({
        id: 1,
        title: '更新标题',
        description: '更新描述',
        investigation: '',
        solution: '',
        category: 'CONFIG',
        priority: 'P3',
        tagIds: [3],
      })
      expect(mockElMessageSuccess).toHaveBeenCalled()
    })

    it('handleEditSubmit - note 为 null 时应直接返回', async () => {
      wrapper.vm.note = null
      mockUpdateNoteApi.mockClear()

      await wrapper.vm.handleEditSubmit({
        title: 'T', description: 'D', investigation: '', solution: '',
        category: 'OTHER', priority: 'P2', tagIds: [], status: 'OPEN',
      })

      expect(mockUpdateNoteApi).not.toHaveBeenCalled()
    })

    it('handleEditSubmit - 403 错误应显示警告', async () => {
      mockUpdateNoteApi.mockRejectedValue(new Error('403 无权操作该笔记'))

      await wrapper.vm.handleEditSubmit({
        title: 'T', description: 'D', investigation: '', solution: '',
        category: 'OTHER', priority: 'P2', tagIds: [], status: 'OPEN',
      })
      await nextTick()

      expect(mockElMessageWarning).toHaveBeenCalled()
    })

    it('handleEditSubmit - 其他错误应显示 error', async () => {
      mockUpdateNoteApi.mockRejectedValue(new Error('网络错误'))

      await wrapper.vm.handleEditSubmit({
        title: 'T', description: 'D', investigation: '', solution: '',
        category: 'OTHER', priority: 'P2', tagIds: [], status: 'OPEN',
      })
      await nextTick()

      expect(mockElMessageError).toHaveBeenCalled()
    })
  })

  // ============================================
  // 状态变更测试
  // ============================================
  describe('状态变更', () => {
    beforeEach(async () => {
      mockGetNoteDetailApi.mockResolvedValue({ data: mockNoteDetail })
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()
    })

    it('确认后应调用 changeNoteStatusApi', async () => {
      mockChangeNoteStatusApi.mockResolvedValue({ data: { id: 1, status: 'IN_PROGRESS' } })
      mockElMessageBoxConfirm.mockResolvedValue('confirm')

      await wrapper.vm.handleChangeStatus('IN_PROGRESS')
      await nextTick()

      expect(mockElMessageBoxConfirm).toHaveBeenCalled()
      expect(mockChangeNoteStatusApi).toHaveBeenCalledWith({
        id: 1,
        status: 'IN_PROGRESS',
      })
      expect(mockElMessageSuccess).toHaveBeenCalled()
    })

    it('用户取消确认时不应调用 API', async () => {
      mockElMessageBoxConfirm.mockRejectedValue('cancel')

      await wrapper.vm.handleChangeStatus('RESOLVED')
      await nextTick()

      expect(mockChangeNoteStatusApi).not.toHaveBeenCalled()
    })

    it('note 为 null 时应直接返回', async () => {
      wrapper.vm.note = null
      mockElMessageBoxConfirm.mockClear()

      await wrapper.vm.handleChangeStatus('IN_PROGRESS')

      expect(mockElMessageBoxConfirm).not.toHaveBeenCalled()
    })

    it('API 失败应显示错误消息', async () => {
      mockChangeNoteStatusApi.mockRejectedValue(new Error('操作失败'))

      await wrapper.vm.handleChangeStatus('IN_PROGRESS')
      await nextTick()

      expect(mockElMessageError).toHaveBeenCalled()
    })
  })

  // ============================================
  // 删除操作测试
  // ============================================
  describe('删除操作', () => {
    beforeEach(async () => {
      mockGetNoteDetailApi.mockResolvedValue({ data: mockNoteDetail })
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()
    })

    it('handleDeleteConfirm 应打开删除确认弹窗', () => {
      expect(wrapper.vm.deleteDialogVisible).toBe(false)
      wrapper.vm.handleDeleteConfirm()
      expect(wrapper.vm.deleteDialogVisible).toBe(true)
    })

    it('handleDelete - 应调用 deleteNoteApi', async () => {
      mockDeleteNoteApi.mockResolvedValue({})
      wrapper.vm.deleteDialogVisible = true

      await wrapper.vm.handleDelete()
      await nextTick()

      expect(mockDeleteNoteApi).toHaveBeenCalledWith({ id: 1 })
      expect(mockElMessageSuccess).toHaveBeenCalled()
      expect(mockPush).toHaveBeenCalledWith('/notes')
    })

    it('handleDelete - note 为 null 时应直接返回', async () => {
      wrapper.vm.note = null
      mockDeleteNoteApi.mockClear()

      await wrapper.vm.handleDelete()

      expect(mockDeleteNoteApi).not.toHaveBeenCalled()
    })

    it('handleDelete - 删除中 deleting 应为 true', () => {
      mockDeleteNoteApi.mockImplementation(() => new Promise(() => {})) // 永不 resolve
      wrapper.vm.handleDelete()
      expect(wrapper.vm.deleting).toBe(true)
    })

    it('handleDelete - 403 错误应显示警告', async () => {
      mockDeleteNoteApi.mockRejectedValue(new Error('403 无权操作该笔记'))

      await wrapper.vm.handleDelete()
      await nextTick()

      expect(mockElMessageWarning).toHaveBeenCalled()
      expect(wrapper.vm.deleting).toBe(false)
    })

    it('handleDelete - 完成后 deleting 应为 false', async () => {
      mockDeleteNoteApi.mockResolvedValue({})

      await wrapper.vm.handleDelete()
      await nextTick()

      expect(wrapper.vm.deleting).toBe(false)
    })
  })

  // ============================================
  // 数据加载测试
  // ============================================
  describe('数据加载', () => {
    it('onMounted 时应调用 getNoteDetailApi', async () => {
      mockGetNoteDetailApi.mockResolvedValue({ data: mockNoteDetail })
      wrapper = createWrapper()
      await nextTick()

      expect(mockGetNoteDetailApi).toHaveBeenCalledWith({ id: 1 })
    })

    it('API 调用失败应设置 note 为 null', async () => {
      mockGetNoteDetailApi.mockRejectedValue(new Error('通用错误'))
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()

      expect(wrapper.vm.note).toBeNull()
      expect(wrapper.vm.loading).toBe(false)
    })
  })

  // ============================================
  // 路由参数监听测试
  // ============================================
  describe('路由参数监听', () => {
    it('路由参数变化时应重新加载数据', async () => {
      mockGetNoteDetailApi.mockResolvedValue({ data: mockNoteDetail })
      wrapper = createWrapper()
      await nextTick()
      await nextTick()
      await nextTick()

      // 清除首次调用记录
      mockGetNoteDetailApi.mockClear()

      // 模拟路由参数变化
      routeParamsRef.id = '2'
      // 触发 watch
      await nextTick()
      await nextTick()

      expect(mockGetNoteDetailApi).toHaveBeenCalledWith({ id: 2 })
    })
  })
})
