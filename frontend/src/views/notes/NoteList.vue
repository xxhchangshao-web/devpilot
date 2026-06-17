<template>
  <div class="note-list-container">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm" @keyup.enter="handleSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索标题或问题描述"
            clearable
            style="width: 220px"
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="filterForm.tagIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部标签"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="tag in tagList"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="filterForm.status"
            placeholder="全部状态"
            clearable
            style="width: 130px"
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级">
          <el-select
            v-model="filterForm.priority"
            placeholder="全部优先级"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in priorityOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="分类">
          <el-select
            v-model="filterForm.category"
            placeholder="全部分类"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button v-if="canWrite" type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建笔记
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        style="width: 100%"
      >
        <el-table-column label="序号" width="70" align="center">
          <template #default="scope">
            {{ (pagination.pageNum - 1) * pagination.pageSize + (scope?.$index ?? 0) + 1 }}
          </template>
        </el-table-column>

        <el-table-column label="标题" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <el-link type="primary" @click="goDetail(scope?.row?.id)">
              {{ truncateTitle(scope?.row?.title || '') }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope?.row?.status || '')" size="small">
              {{ getStatusLabel(scope?.row?.status || '') }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="优先级" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getPriorityTagType(scope?.row?.priority || '')" size="small">
              {{ getPriorityLabel(scope?.row?.priority || '') }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="分类" width="110" align="center">
          <template #default="scope">
            {{ getCategoryLabel(scope?.row?.category || '') }}
          </template>
        </el-table-column>

        <el-table-column label="标签" width="160">
          <template #default="scope">
            <template v-if="scope?.row?.tags && scope.row.tags.length > 0">
              <el-tag
                v-for="tag in scope.row.tags.slice(0, 3)"
                :key="tag.id"
                size="small"
                style="margin-right: 4px"
              >
                {{ tag.name }}
              </el-tag>
              <el-tooltip
                v-if="scope.row.tags.length > 3"
                :content="scope.row.tags.slice(3).map((t: any) => t.name).join('、')"
                placement="top"
              >
                <el-tag size="small" type="info">+{{ scope.row.tags.length - 3 }}</el-tag>
              </el-tooltip>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="更新时间" width="160" align="center">
          <template #default="scope">
            {{ formatDateTime(scope?.row?.updated_at || '') }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>

      <!-- 空状态与搜索无结果 -->
      <el-empty
        v-if="!loading && tableData.length === 0"
        :description="emptyDescription"
      />
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <NoteFormDialog
      v-model:visible="dialogVisible"
      :submitting="submitting"
      :initial-data="editData"
      @submit="handleFormSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listNotesApi, createNoteApi, updateNoteApi, type NoteListItem, type NoteListParams } from '@/api/notes'
import { getTagListApi, type TagInfo } from '@/api/tags'
import { formatDateTime } from '@/utils/format'
import NoteFormDialog from './NoteFormDialog.vue'

// ========== 路由 ==========

const router = useRouter()

// ========== 状态 ==========

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<NoteListItem[]>([])
const tagList = ref<TagInfo[]>([])
const dialogVisible = ref(false)
const editData = ref<{
  title: string
  description: string
  investigation?: string
  solution?: string
  category: string
  priority: string
  tagIds: number[]
  status?: string
} | null>(null)

// ========== 筛选参数 ==========

const filterForm = reactive<NoteListParams>({
  keyword: '',
  tagIds: [],
  status: undefined,
  priority: undefined,
  category: undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// ========== 选项数据 ==========

const statusOptions = [
  { label: '待解决', value: 'OPEN' },
  { label: '排查中', value: 'IN_PROGRESS' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已归档', value: 'ARCHIVED' }
]

const priorityOptions = [
  { label: 'P0-紧急', value: 'P0' },
  { label: 'P1-高', value: 'P1' },
  { label: 'P2-中', value: 'P2' },
  { label: 'P3-低', value: 'P3' }
]

const categoryOptions = [
  { label: '性能问题', value: 'PERFORMANCE' },
  { label: '异常报错', value: 'EXCEPTION' },
  { label: '配置问题', value: 'CONFIG' },
  { label: '环境问题', value: 'ENVIRONMENT' },
  { label: '业务逻辑', value: 'BUSINESS_LOGIC' },
  { label: '其他', value: 'OTHER' }
]

// ========== 计算属性 ==========

/** 是否有筛选条件 */
const hasFilter = computed(() => {
  return !!(
    filterForm.keyword ||
    (filterForm.tagIds && filterForm.tagIds.length > 0) ||
    filterForm.status ||
    filterForm.priority ||
    filterForm.category
  )
})

/** 空状态描述文案 */
const emptyDescription = computed(() => {
  return hasFilter.value
    ? '未找到匹配的问题笔记，请调整筛选条件'
    : '暂无问题笔记，点击上方按钮创建第一条'
})

const canWrite = computed(() => getStoredRole() !== 'VIEWER')

// ========== 枚举映射函数 ==========

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    OPEN: '待解决',
    IN_PROGRESS: '排查中',
    RESOLVED: '已解决',
    ARCHIVED: '已归档'
  }
  return map[status] || status
}

function getStatusTagType(status: string): 'primary' | 'warning' | 'success' | 'info' {
  const map: Record<string, 'primary' | 'warning' | 'success' | 'info'> = {
    OPEN: 'primary',
    IN_PROGRESS: 'warning',
    RESOLVED: 'success',
    ARCHIVED: 'info'
  }
  return map[status] || 'info'
}

function getPriorityLabel(priority: string): string {
  const map: Record<string, string> = {
    P0: 'P0-紧急',
    P1: 'P1-高',
    P2: 'P2-中',
    P3: 'P3-低'
  }
  return map[priority] || priority
}

function getPriorityTagType(priority: string): 'danger' | 'warning' | 'primary' | 'info' {
  const map: Record<string, 'danger' | 'warning' | 'primary' | 'info'> = {
    P0: 'danger',
    P1: 'warning',
    P2: 'primary',
    P3: 'info'
  }
  return map[priority] || 'info'
}

function getCategoryLabel(category: string): string {
  const map: Record<string, string> = {
    PERFORMANCE: '性能问题',
    EXCEPTION: '异常报错',
    CONFIG: '配置问题',
    ENVIRONMENT: '环境问题',
    BUSINESS_LOGIC: '业务逻辑',
    OTHER: '其他'
  }
  return map[category] || category
}

function truncateTitle(title: string): string {
  if (title.length > 30) {
    return title.substring(0, 30) + '...'
  }
  return title
}

// ========== 数据加载 ==========

async function loadTagList() {
  try {
    const res = await getTagListApi()
    tagList.value = res.data.records
  } catch {
    // 标签加载失败使用空列表
    tagList.value = []
  }
}

async function fetchList() {
  loading.value = true
  try {
    const params: NoteListParams = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    if (filterForm.keyword) params.keyword = filterForm.keyword
    if (filterForm.tagIds && filterForm.tagIds.length > 0) params.tagIds = filterForm.tagIds
    if (filterForm.status) params.status = filterForm.status
    if (filterForm.priority) params.priority = filterForm.priority
    if (filterForm.category) params.category = filterForm.category

    const res = await listNotesApi(params)
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (err: any) {
    ElMessage.error(err?.message || '网络异常，请稍后重试')
    tableData.value = []
  } finally {
    loading.value = false
  }
}

// ========== 交互方法 ==========

function handleSearch() {
  pagination.pageNum = 1
  fetchList()
}

function handleReset() {
  filterForm.keyword = ''
  filterForm.tagIds = []
  filterForm.status = undefined
  filterForm.priority = undefined
  filterForm.category = undefined
  pagination.pageNum = 1
  fetchList()
}

function handlePageChange() {
  fetchList()
}

function handleSizeChange() {
  pagination.pageNum = 1
  fetchList()
}

function handleCreate() {
  if (!canWrite.value) {
    ElMessage.warning('演示账号仅支持浏览')
    return
  }
  editData.value = null
  dialogVisible.value = true
}

function goDetail(id?: number) {
  if (!id) return
  router.push(`/notes/${id}`)
}

async function handleFormSubmit(data: {
  title: string
  description: string
  investigation: string
  solution: string
  category: string
  priority: string
  tagIds: number[]
  status: string
}) {
  submitting.value = true
  try {
    if (!canWrite.value) {
      ElMessage.warning('演示账号仅支持浏览')
      return
    }
    // 判断是新建还是编辑
    if (editData.value) {
      // 编辑模式（实际在列表页不触发编辑，编辑入口在详情页）
      // 但保留此逻辑以备后续使用
      await updateNoteApi({
        id: (editData.value as any).id,
        ...data
      })
      ElMessage.success('操作成功')
      dialogVisible.value = false
      editData.value = null
      fetchList()
    } else {
      // 新建模式
      await createNoteApi(data)
      ElMessage.success('操作成功')
      dialogVisible.value = false
      fetchList()
    }
  } catch (err: any) {
    const msg = err?.message || '操作失败'
    if (msg.includes('403') || msg.includes('无权')) {
      ElMessage.warning('无权操作该笔记')
    } else {
      ElMessage.error(msg)
    }
  } finally {
    submitting.value = false
  }
}

// ========== 生命周期 ==========

onMounted(() => {
  loadTagList()
  fetchList()
})

function getStoredRole() {
  const storage = globalThis.localStorage
  if (!storage || typeof storage.getItem !== 'function') return ''

  const user = storage.getItem('user')
  if (!user) return ''
  try {
    return String(JSON.parse(user).role || '').replace(/^ROLE_/, '').toUpperCase()
  } catch {
    return ''
  }
}
</script>

<style scoped>
.note-list-container {
  max-width: 1400px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-card .el-form-item {
  margin-bottom: 0;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.text-muted {
  color: #c0c4cc;
}
</style>
