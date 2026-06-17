<template>
  <div class="note-detail-container">
    <!-- 返回按钮 -->
    <div class="back-bar">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>
    </div>

    <!-- 加载中 -->
    <el-card v-if="loading" shadow="never">
      <el-skeleton :rows="10" animated />
    </el-card>

    <!-- 笔记不存在 -->
    <el-card v-else-if="!note" shadow="never">
      <el-empty description="笔记不存在或已被删除" />
    </el-card>

    <!-- 笔记详情 -->
    <template v-if="note">
      <!-- 元数据 -->
      <el-card shadow="never" class="meta-card">
        <template #header>
          <div class="card-header">
            <h2 class="note-title">{{ note.title }}</h2>
            <div class="status-tags">
              <el-tag :type="getStatusTagType(note.status)" size="default">
                {{ getStatusLabel(note.status) }}
              </el-tag>
              <el-tag :type="getPriorityTagType(note.priority)" size="default" style="margin-left: 8px">
                {{ getPriorityLabel(note.priority) }}
              </el-tag>
            </div>
          </div>
        </template>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="分类">
            {{ getCategoryLabel(note.category) }}
          </el-descriptions-item>
          <el-descriptions-item label="标签">
            <template v-if="note.tags && note.tags.length > 0">
              <el-tag
                v-for="tag in note.tags"
                :key="tag.id"
                size="small"
                style="margin-right: 4px"
              >
                {{ tag.name }}
              </el-tag>
            </template>
            <span v-else class="text-muted">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建者">
            {{ note.creatorName }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatFullDateTime(note.created_at) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间" :span="2">
            {{ formatFullDateTime(note.updated_at) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 问题描述 -->
      <el-card shadow="never" class="content-card">
        <template #header>
          <span class="section-title">问题描述</span>
        </template>
        <div class="content-text">{{ note.description }}</div>
      </el-card>

      <!-- 排查过程 -->
      <el-card v-if="note.investigation" shadow="never" class="content-card">
        <template #header>
          <span class="section-title">排查过程</span>
        </template>
        <div class="content-text pre-wrap">{{ note.investigation }}</div>
      </el-card>

      <!-- 解决方案 -->
      <el-card v-if="note.solution" shadow="never" class="content-card">
        <template #header>
          <span class="section-title">解决方案</span>
        </template>
        <div class="content-text pre-wrap">{{ note.solution }}</div>
      </el-card>

      <!-- 操作区 -->
      <div v-if="note.canEdit" class="action-bar">
        <el-button type="primary" @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>

        <el-dropdown @command="handleChangeStatus">
          <el-button type="warning">
            更改状态
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="item in availableStatuses"
                :key="item.value"
                :command="item.value"
              >
                {{ item.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button type="danger" @click="handleDeleteConfirm">
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
      </div>
    </template>

    <!-- 编辑弹窗 -->
    <NoteFormDialog
      v-model:visible="editDialogVisible"
      :submitting="submitting"
      :initial-data="editFormData"
      @submit="handleEditSubmit"
    />

    <!-- 删除确认弹窗 -->
    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除"
      width="420px"
      :close-on-click-modal="false"
    >
      <p>确定删除该笔记？此操作不可撤销</p>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="deleting" @click="handleDelete">
          确认删除
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Edit, Delete, ArrowDown } from '@element-plus/icons-vue'
import { getNoteDetailApi, updateNoteApi, deleteNoteApi, changeNoteStatusApi, type NoteDetailInfo } from '@/api/notes'
import { formatFullDateTime } from '@/utils/format'
import NoteFormDialog from './NoteFormDialog.vue'

// ========== 路由 ==========

const route = useRoute()
const router = useRouter()

// ========== 状态 ==========

const loading = ref(false)
const submitting = ref(false)
const note = ref<NoteDetailInfo | null>(null)
const editDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const deleting = ref(false)

// ========== 编辑表单数据 ==========

const editFormData = computed(() => {
  if (!note.value) return null
  return {
    id: note.value.id,
    title: note.value.title,
    description: note.value.description,
    investigation: note.value.investigation,
    solution: note.value.solution,
    category: note.value.category,
    priority: note.value.priority,
    tagIds: note.value.tags.map(t => t.id)
  }
})

// ========== 状态变化可用选项 ==========

const allStatusOptions = [
  { label: '待解决', value: 'OPEN' },
  { label: '排查中', value: 'IN_PROGRESS' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已归档', value: 'ARCHIVED' }
]

/** 根据当前状态计算可流转的状态选项 */
const availableStatuses = computed(() => {
  if (!note.value) return []
  const current = note.value.status
  // 状态流转规则
  const allowedMap: Record<string, string[]> = {
    OPEN: ['IN_PROGRESS', 'ARCHIVED'],
    IN_PROGRESS: ['RESOLVED', 'OPEN'],
    RESOLVED: ['OPEN', 'ARCHIVED'],
    ARCHIVED: [] // 终态不可变更
  }
  const allowed = allowedMap[current] || []
  return allStatusOptions.filter(item => allowed.includes(item.value))
})

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

// ========== 数据加载 ==========

async function fetchDetail() {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('笔记ID无效')
    router.push('/notes')
    return
  }

  loading.value = true
  try {
    const res = await getNoteDetailApi({ id })
    note.value = res.data
  } catch (err: any) {
    const msg = err?.message || '加载失败'
    if (msg.includes('404') || msg.includes('不存在')) {
      ElMessage.error('笔记不存在或已被删除')
      setTimeout(() => router.push('/notes'), 1500)
    } else {
      ElMessage.error(msg)
    }
    note.value = null
  } finally {
    loading.value = false
  }
}

// ========== 交互方法 ==========

function goBack() {
  router.push('/notes')
}

// 编辑
function handleEdit() {
  editDialogVisible.value = true
}

async function handleEditSubmit(data: {
  title: string
  description: string
  investigation: string
  solution: string
  category: string
  priority: string
  tagIds: number[]
  status: string
}) {
  if (!note.value) return
  submitting.value = true
  try {
    await updateNoteApi({
      id: note.value.id,
      title: data.title,
      description: data.description,
      investigation: data.investigation,
      solution: data.solution,
      category: data.category,
      priority: data.priority,
      tagIds: data.tagIds
    })
    ElMessage.success('操作成功')
    editDialogVisible.value = false
    fetchDetail() // 刷新详情
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

// 更改状态
async function handleChangeStatus(targetStatus: string) {
  if (!note.value) return
  const targetLabel = getStatusLabel(targetStatus)
  try {
    await ElMessageBox.confirm(
      `确定将状态更改为「${targetLabel}」？`,
      '更改状态',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }

  try {
    await changeNoteStatusApi({ id: note.value.id, status: targetStatus })
    ElMessage.success('操作成功')
    fetchDetail() // 刷新详情
  } catch (err: any) {
    ElMessage.error(err?.message || '操作失败')
  }
}

// 删除
function handleDeleteConfirm() {
  deleteDialogVisible.value = true
}

async function handleDelete() {
  if (!note.value) return
  deleting.value = true
  try {
    await deleteNoteApi({ id: note.value.id })
    ElMessage.success('操作成功')
    deleteDialogVisible.value = false
    router.push('/notes')
  } catch (err: any) {
    const msg = err?.message || '操作失败'
    if (msg.includes('403') || msg.includes('无权')) {
      ElMessage.warning('无权操作该笔记')
    } else {
      ElMessage.error(msg)
    }
  } finally {
    deleting.value = false
  }
}

// ========== 生命周期 ==========

onMounted(() => {
  fetchDetail()
})

// 监听路由参数变化（同一组件内切换详情）
watch(() => route.params.id, () => {
  fetchDetail()
})
</script>

<style scoped>
.note-detail-container {
  max-width: 1200px;
}

.back-bar {
  margin-bottom: 16px;
}

.meta-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.note-title {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.status-tags {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.section-title {
  font-weight: 600;
  color: #303133;
}

.content-card {
  margin-bottom: 16px;
}

.content-text {
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.pre-wrap {
  white-space: pre-wrap;
}

.action-bar {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.text-muted {
  color: #c0c4cc;
}
</style>
