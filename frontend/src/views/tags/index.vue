<template>
  <div class="tag-management-container">
    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <el-button
        v-if="canWrite"
        type="primary"
        @click="handleCreate"
      >
        <el-icon><Plus /></el-icon>
        新建标签
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
          <template #default="{ $index }">
            {{ (pagination.pageNum - 1) * pagination.pageSize + $index + 1 }}
          </template>
        </el-table-column>

        <el-table-column label="标签名称" min-width="160">
          <template #default="{ row }">
            <el-tag
              :color="getTagColor(row.name)"
              effect="dark"
              size="default"
            >
              {{ row.name }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="关联笔记数" width="120" align="center">
          <template #default="{ row }">
            <el-link
              type="primary"
              :underline="false"
              @click="goNotesByTag(row.id)"
            >
              {{ row.noteCount }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column
          v-if="canWrite"
          label="操作"
          width="140"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>

      <!-- 空状态 -->
      <el-empty
        v-if="!loading && tableData.length === 0"
        description="暂无标签，点击上方按钮创建第一个标签"
      />
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      :model-value="dialogVisible"
      :title="isEdit ? '编辑标签' : '新建标签'"
      width="400px"
      :close-on-click-modal="false"
      @update:model-value="dialogVisible = $event"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="80px"
        @keyup.enter="handleSubmit"
      >
        <el-form-item label="标签名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="请输入标签名称"
            maxlength="30"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createTagApi,
  updateTagApi,
  deleteTagApi,
  getTagListApi,
  type TagVO
} from '@/api/tags'
import { useUserStore } from '@/store/user'
import { formatDateTime } from '@/utils/format'

// ========== 路由 ==========

const router = useRouter()
const userStore = useUserStore()

// ========== 权限 ==========

const canWrite = computed(() => {
  return userStore.canWrite()
})

// ========== 状态 ==========

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<TagVO[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const formData = reactive({
  name: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// ========== 标签颜色 ==========

const TAG_COLORS = [
  '#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6',
  '#ec4899', '#06b6d4', '#84cc16', '#f97316', '#6366f1'
]

function getTagColor(name: string): string {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = ((hash << 5) - hash) + name.charCodeAt(i)
    hash |= 0
  }
  return TAG_COLORS[Math.abs(hash) % TAG_COLORS.length]
}

// ========== 校验规则 ==========

const rules: FormRules = {
  name: [
    { required: true, message: '标签名称不能为空', trigger: 'blur' },
    { min: 1, max: 30, message: '标签名称不能超过30个字符', trigger: 'blur' }
  ]
}

// ========== 数据加载 ==========

async function fetchList() {
  loading.value = true
  try {
    const res = await getTagListApi({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
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

function handlePageChange() {
  fetchList()
}

function handleSizeChange() {
  pagination.pageNum = 1
  fetchList()
}

function handleCreate() {
  isEdit.value = false
  editingId.value = null
  formData.name = ''
  dialogVisible.value = true
  // 弹窗打开后聚焦输入框
  setTimeout(() => {
    formRef.value?.resetFields()
  }, 0)
}

function handleEdit(row: TagVO) {
  isEdit.value = true
  editingId.value = row.id
  formData.name = row.name
  dialogVisible.value = true
  setTimeout(() => {
    formRef.value?.clearValidate()
  }, 0)
}

function handleDialogClosed() {
  formData.name = ''
  isEdit.value = false
  editingId.value = null
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value && editingId.value != null) {
      await updateTagApi({ id: editingId.value, name: formData.name })
    } else {
      await createTagApi({ name: formData.name })
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchList()
  } catch (err: any) {
    // 错误消息由后端返回，弹窗不关闭以保留用户输入
    ElMessage.error(err?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: TagVO) {
  try {
    await ElMessageBox.confirm(
      `关联的 ${row.noteCount} 条笔记将失去此标签，确认删除？`,
      '删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    // 用户取消删除
    return
  }

  try {
    await deleteTagApi({ id: row.id })
    ElMessage.success('操作成功')
    // 如果删除后当前页无数据且不是第一页，回到上一页
    if (tableData.value.length === 1 && pagination.pageNum > 1) {
      pagination.pageNum--
    }
    fetchList()
  } catch (err: any) {
    ElMessage.error(err?.message || '操作失败')
  }
}

function goNotesByTag(tagId: number) {
  router.push({ path: '/notes', query: { tagId: String(tagId) } })
}

// ========== 生命周期 ==========

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.tag-management-container {
  max-width: 1200px;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
