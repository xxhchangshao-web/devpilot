/**
 * NoteList 组件测试
 * 测试 src/views/notes/NoteList.vue: 笔记列表、筛选、分页、创建
 * 
 * 策略: 使用浅 stub 避免 Element Plus scoped slot 问题，通过 vm 测试业务逻辑
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { shallowMount, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'

// ============================================
// Mock 外部依赖 (使用 vi.hoisted 避免 hoisting 问题)
// ============================================

const { mockListNotesApi, mockCreateNoteApi, mockUpdateNoteApi } = vi.hoisted(() => ({
  mockListNotesApi: vi.fn(),
  mockCreateNoteApi: vi.fn(),
  mockUpdateNoteApi: vi.fn(),
}))
vi.mock('@/api/notes', () => ({
  listNotesApi: mockListNotesApi,
  createNoteApi: mockCreateNoteApi,
  updateNoteApi: mockUpdateNoteApi,
  getNoteDetailApi: vi.fn(),
  deleteNoteApi: vi.fn(),
  changeNoteStatusApi: vi.fn(),
}))

const { mockGetTagListApi } = vi.hoisted(() => ({
  mockGetTagListApi: vi.fn(),
}))
vi.mock('@/api/tags', () => ({
  getTagListApi: mockGetTagListApi,
}))

const { mockPush } = vi.hoisted(() => ({
  mockPush: vi.fn(),
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    back: vi.fn(),
  }),
  useRoute: () => ({
    params: {},
    query: {},
    path: '/notes',
    name: 'Notes',
  }),
}))

const { mockElMessageSuccess, mockElMessageError, mockElMessageWarning } = vi.hoisted(() => ({
  mockElMessageSuccess: vi.fn(),
  mockElMessageError: vi.fn(),
  mockElMessageWarning: vi.fn(),
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
  }
})

vi.mock('@element-plus/icons-vue', () => {
  const icon = { name: 'icon', render: () => null, inheritAttrs: false }
  return { Plus: icon, ArrowLeft: icon, Edit: icon, Delete: icon, ArrowDown: icon, User: icon, Lock: icon, HomeFilled: icon }
})

vi.mock('@/utils/format', () => ({
  formatDateTime: vi.fn((d: string) => '2024-01-15 10:30'),
}))

import NoteList from './NoteList.vue'

// ============================================
// 模拟数据
// ============================================

const mockTagList = [
  { id: 1, name: '后端' },
  { id: 2, name: '紧急' },
]

const mockNoteListItems = [
  { id: 1, title: '测试问题', status: 'OPEN', priority: 'P1', category: 'EXCEPTION',
    tags: [{ id: 1, name: '后端' }], creatorName: '张三',
    created_at: '2024-01-01', updated_at: '2024-01-02' },
]

const mockListResult = { records: mockNoteListItems, total: 25, pageNum: 1, pageSize: 20 }

// ============================================
// 测试套件
// ============================================

describe('NoteList 组件', () => {
  let wrapper: VueWrapper<any>

  /**
   * 使用 shallowMount 挂载组件，所有 Element Plus 子组件被浅 stub，
   * 避免 scoped slot 渲染问题。所有测试通过 wrapper.vm 访问组件内部状态。
   */
  const createWrapper = () => {
    return shallowMount(NoteList, {
      global: {
        stubs: {
          // 关键：stub NoteFormDialog 子组件
          NoteFormDialog: {
            props: ['visible', 'initialData'],
            emits: ['update:visible', 'submit'],
            template: '<div class="mock-dialog" v-if="visible"></div>',
          },
        },
      },
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockGetTagListApi.mockResolvedValue({ data: { records: mockTagList } })
    mockListNotesApi.mockResolvedValue({ data: mockListResult })
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
  })

  // ============================================
  // 基础挂载测试
  // ============================================
  describe('基础挂载', () => {
    it('组件应成功挂载', () => {
      wrapper = createWrapper()
      expect(wrapper.exists()).toBe(true)
    })

    it('vm 应可访问', () => {
      wrapper = createWrapper()
      expect(wrapper.vm).toBeDefined()
    })
  })

  // ============================================
  // 枚举映射函数测试
  // ============================================
  describe('枚举映射函数', () => {
    beforeEach(() => { wrapper = createWrapper() })

    it('getStatusLabel - 应返回正确的中文标签', () => {
      expect(wrapper.vm.getStatusLabel('OPEN')).toBe('待解决')
      expect(wrapper.vm.getStatusLabel('IN_PROGRESS')).toBe('排查中')
      expect(wrapper.vm.getStatusLabel('RESOLVED')).toBe('已解决')
      expect(wrapper.vm.getStatusLabel('ARCHIVED')).toBe('已归档')
    })

    it('getStatusLabel - 未知状态返回原值', () => {
      expect(wrapper.vm.getStatusLabel('UNKNOWN')).toBe('UNKNOWN')
    })

    it('getStatusTagType - 应返回正确的 tag 类型', () => {
      expect(wrapper.vm.getStatusTagType('OPEN')).toBe('primary')
      expect(wrapper.vm.getStatusTagType('IN_PROGRESS')).toBe('warning')
      expect(wrapper.vm.getStatusTagType('RESOLVED')).toBe('success')
      expect(wrapper.vm.getStatusTagType('ARCHIVED')).toBe('info')
    })

    it('getStatusTagType - 未知状态返回 info', () => {
      expect(wrapper.vm.getStatusTagType('UNKNOWN')).toBe('info')
    })

    it('getPriorityLabel - 应返回正确的中文标签', () => {
      expect(wrapper.vm.getPriorityLabel('P0')).toBe('P0-紧急')
      expect(wrapper.vm.getPriorityLabel('P1')).toBe('P1-高')
      expect(wrapper.vm.getPriorityLabel('P2')).toBe('P2-中')
      expect(wrapper.vm.getPriorityLabel('P3')).toBe('P3-低')
    })

    it('getPriorityLabel - 未知优先级返回原值', () => {
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
      expect(wrapper.vm.getCategoryLabel('UNKNOWN')).toBe('UNKNOWN')
    })

    it('truncateTitle - 短标题保持原样', () => {
      expect(wrapper.vm.truncateTitle('短标题')).toBe('短标题')
    })

    it('truncateTitle - 超长标题应截断并添加省略号', () => {
      const longTitle = 'A'.repeat(35)
      const result = wrapper.vm.truncateTitle(longTitle)
      expect(result.endsWith('...')).toBe(true)
      expect(result.length).toBe(33)
      expect(result).toBe(longTitle.substring(0, 30) + '...')
    })

    it('truncateTitle - 刚好30字符不变', () => {
      const exact30 = 'A'.repeat(30)
      expect(wrapper.vm.truncateTitle(exact30)).toBe(exact30)
    })
  })

  // ============================================
  // 计算属性测试
  // ============================================
  describe('计算属性', () => {
    beforeEach(() => { wrapper = createWrapper() })

    it('hasFilter - 无筛选条件时应为 false', () => {
      wrapper.vm.filterForm.keyword = ''
      wrapper.vm.filterForm.tagIds = []
      wrapper.vm.filterForm.status = undefined
      wrapper.vm.filterForm.priority = undefined
      wrapper.vm.filterForm.category = undefined
      expect(wrapper.vm.hasFilter).toBe(false)
    })

    it('hasFilter - 有关键词时应为 true', () => {
      wrapper.vm.filterForm.keyword = 'test'
      expect(wrapper.vm.hasFilter).toBe(true)
    })

    it('hasFilter - 有标签筛选时应为 true', () => {
      wrapper.vm.filterForm.keyword = ''
      wrapper.vm.filterForm.tagIds = [1]
      expect(wrapper.vm.hasFilter).toBe(true)
    })

    it('hasFilter - 有状态筛选时应为 true', () => {
      wrapper.vm.filterForm.keyword = ''
      wrapper.vm.filterForm.tagIds = []
      wrapper.vm.filterForm.status = 'OPEN'
      expect(wrapper.vm.hasFilter).toBe(true)
    })

    it('hasFilter - 有优先级筛选时应为 true', () => {
      wrapper.vm.filterForm.keyword = ''
      wrapper.vm.filterForm.tagIds = []
      wrapper.vm.filterForm.status = undefined
      wrapper.vm.filterForm.priority = 'P1'
      expect(wrapper.vm.hasFilter).toBe(true)
    })

    it('hasFilter - 有分类筛选时应为 true', () => {
      wrapper.vm.filterForm.keyword = ''
      wrapper.vm.filterForm.tagIds = []
      wrapper.vm.filterForm.status = undefined
      wrapper.vm.filterForm.priority = undefined
      wrapper.vm.filterForm.category = 'EXCEPTION'
      expect(wrapper.vm.hasFilter).toBe(true)
    })

    it('emptyDescription - 无筛选条件显示创建提示', () => {
      wrapper.vm.filterForm.keyword = ''
      wrapper.vm.filterForm.tagIds = []
      expect(wrapper.vm.emptyDescription).toContain('暂无问题笔记')
    })

    it('emptyDescription - 有筛选条件显示调整提示', () => {
      wrapper.vm.filterForm.keyword = '不存在'
      expect(wrapper.vm.emptyDescription).toContain('未找到匹配')
    })
  })

  // ============================================
  // 数据加载测试
  // ============================================
  describe('数据加载', () => {
    it('fetchList - 成功加载应更新 tableData 和分页', async () => {
      wrapper = createWrapper()
      await wrapper.vm.fetchList()
      await nextTick()

      expect(wrapper.vm.tableData).toEqual(mockNoteListItems)
      expect(wrapper.vm.pagination.total).toBe(25)
    })

    it('fetchList - 应传递筛选参数到 API', async () => {
      wrapper = createWrapper()
      wrapper.vm.filterForm.keyword = '测试'
      wrapper.vm.filterForm.status = 'OPEN'
      wrapper.vm.filterForm.tagIds = [1]

      await wrapper.vm.fetchList()
      await nextTick()

      expect(mockListNotesApi).toHaveBeenCalledWith(
        expect.objectContaining({ keyword: '测试', status: 'OPEN', tagIds: [1] })
      )
    })

    it('fetchList - API 错误应清空数据', async () => {
      mockListNotesApi.mockRejectedValue(new Error('网络错误'))
      wrapper = createWrapper()
      await wrapper.vm.fetchList()
      await nextTick()

      expect(wrapper.vm.tableData).toEqual([])
    })

    it('fetchList - loading 状态应在开始和结束时正确切换', async () => {
      wrapper = createWrapper()
      const promise = wrapper.vm.fetchList()
      expect(wrapper.vm.loading).toBe(true)
      await promise
      await nextTick()
      expect(wrapper.vm.loading).toBe(false)
    })

    it('loadTagList - 成功加载应更新 tagList', async () => {
      wrapper = createWrapper()
      await wrapper.vm.loadTagList()
      await nextTick()

      expect(wrapper.vm.tagList).toEqual(mockTagList)
    })

    it('loadTagList - 失败应使用空列表', async () => {
      mockGetTagListApi.mockRejectedValue(new Error('fail'))
      wrapper = createWrapper()
      await wrapper.vm.loadTagList()
      await nextTick()

      expect(wrapper.vm.tagList).toEqual([])
    })
  })

  // ============================================
  // 筛选操作测试
  // ============================================
  describe('筛选操作', () => {
    beforeEach(() => {
      wrapper = createWrapper()
    })

    it('handleSearch - 应重置到第一页', () => {
      wrapper.vm.pagination.pageNum = 5
      wrapper.vm.handleSearch()
      expect(wrapper.vm.pagination.pageNum).toBe(1)
    })

    it('handleReset - 应清空所有筛选条件', () => {
      wrapper.vm.filterForm.keyword = 'test'
      wrapper.vm.filterForm.tagIds = [1]
      wrapper.vm.filterForm.status = 'OPEN'
      wrapper.vm.filterForm.priority = 'P1'
      wrapper.vm.filterForm.category = 'EXCEPTION'
      wrapper.vm.pagination.pageNum = 3

      wrapper.vm.handleReset()

      expect(wrapper.vm.filterForm.keyword).toBe('')
      expect(wrapper.vm.filterForm.tagIds).toEqual([])
      expect(wrapper.vm.filterForm.status).toBeUndefined()
      expect(wrapper.vm.filterForm.priority).toBeUndefined()
      expect(wrapper.vm.filterForm.category).toBeUndefined()
      expect(wrapper.vm.pagination.pageNum).toBe(1)
    })
  })

  // ============================================
  // 分页操作测试
  // ============================================
  describe('分页操作', () => {
    beforeEach(() => { wrapper = createWrapper() })

    it('handleSizeChange - 应重置页码到第一页', () => {
      wrapper.vm.pagination.pageNum = 5
      wrapper.vm.handleSizeChange()
      expect(wrapper.vm.pagination.pageNum).toBe(1)
    })
  })

  // ============================================
  // 创建笔记测试
  // ============================================
  describe('创建笔记', () => {
    beforeEach(() => { wrapper = createWrapper() })

    it('handleCreate - 应打开弹窗并清空 editData', () => {
      wrapper.vm.editData = { title: 'old', description: '', category: '', priority: '', tagIds: [] }
      wrapper.vm.handleCreate()
      expect(wrapper.vm.editData).toBeNull()
      expect(wrapper.vm.dialogVisible).toBe(true)
    })

    it('handleFormSubmit - 新建模式应调用 createNoteApi', async () => {
      mockCreateNoteApi.mockResolvedValue({ data: { id: 3 } })
      wrapper.vm.editData = null

      const data = { title: 'N', description: 'D', investigation: '', solution: '', category: 'OTHER', priority: 'P2', tagIds: [], status: 'OPEN' }
      await wrapper.vm.handleFormSubmit(data)
      await nextTick()

      expect(mockCreateNoteApi).toHaveBeenCalledWith(data)
      expect(mockElMessageSuccess).toHaveBeenCalled()
    })

    it('handleFormSubmit - 新建失败 403 应显示 warning', async () => {
      mockCreateNoteApi.mockRejectedValue(new Error('403 无权'))
      wrapper.vm.editData = null
      const data = { title: 'T', description: 'D', investigation: '', solution: '', category: 'OTHER', priority: 'P2', tagIds: [], status: 'OPEN' }

      await wrapper.vm.handleFormSubmit(data)
      await nextTick()

      expect(mockElMessageWarning).toHaveBeenCalled()
    })

    it('handleFormSubmit - 新建失败其他错误应显示 error', async () => {
      mockCreateNoteApi.mockRejectedValue(new Error('网络异常'))
      wrapper.vm.editData = null
      const data = { title: 'T', description: 'D', investigation: '', solution: '', category: 'OTHER', priority: 'P2', tagIds: [], status: 'OPEN' }

      await wrapper.vm.handleFormSubmit(data)
      await nextTick()

      expect(mockElMessageError).toHaveBeenCalled()
    })

    it('handleFormSubmit - 编辑模式应调用 updateNoteApi', async () => {
      mockUpdateNoteApi.mockResolvedValue({ data: { id: 1 } })
      wrapper.vm.editData = { title: 'O', description: '', category: '', priority: '', tagIds: [] }
      const data = { title: 'U', description: 'D', investigation: '', solution: '', category: 'OTHER', priority: 'P2', tagIds: [], status: 'OPEN' }

      await wrapper.vm.handleFormSubmit(data)
      await nextTick()

      expect(mockUpdateNoteApi).toHaveBeenCalled()
      expect(mockElMessageSuccess).toHaveBeenCalled()
    })
  })

  // ============================================
  // 导航测试
  // ============================================
  describe('导航', () => {
    beforeEach(() => { wrapper = createWrapper() })

    it('goDetail - 应跳转到正确的详情页', () => {
      wrapper.vm.goDetail(42)
      expect(mockPush).toHaveBeenCalledWith('/notes/42')
    })
  })

  // ============================================
  // 选项数据测试
  // ============================================
  describe('选项数据', () => {
    beforeEach(() => { wrapper = createWrapper() })

    it('statusOptions 应包含 4 个选项', () => {
      expect(wrapper.vm.statusOptions).toHaveLength(4)
    })

    it('priorityOptions 应包含 4 个选项', () => {
      expect(wrapper.vm.priorityOptions).toHaveLength(4)
    })

    it('categoryOptions 应包含 6 个选项', () => {
      expect(wrapper.vm.categoryOptions).toHaveLength(6)
    })
  })
})
