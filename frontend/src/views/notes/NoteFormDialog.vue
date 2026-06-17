<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑笔记' : '新建笔记'"
    width="800px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" placeholder="请输入标题" maxlength="200" show-word-limit />
      </el-form-item>

      <el-form-item label="问题描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="4"
          placeholder="请描述问题现象、影响范围等"
          maxlength="10000"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="排查过程">
        <el-input
          v-model="formData.investigation"
          type="textarea"
          :rows="4"
          placeholder="请记录排查步骤、分析过程等（选填）"
        />
      </el-form-item>

      <el-form-item label="解决方案">
        <el-input
          v-model="formData.solution"
          type="textarea"
          :rows="3"
          placeholder="请记录最终解决方案（选填）"
        />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="分类" prop="category" label-width="80px">
            <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%">
              <el-option
                v-for="item in categoryOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="优先级" prop="priority" label-width="80px">
            <el-select v-model="formData.priority" placeholder="请选择优先级" style="width: 100%">
              <el-option
                v-for="item in priorityOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="标签">
        <el-select
          v-model="formData.tagIds"
          multiple
          filterable
          placeholder="请选择标签（可多选）"
          style="width: 100%"
        >
          <el-option
            v-for="tag in tagList"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          />
        </el-select>
      </el-form-item>

      <!-- 仅新建时显示状态选择 -->
      <el-form-item v-if="!isEdit" label="状态">
        <el-select v-model="formData.status" placeholder="请选择状态" style="width: 100%">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getTagListApi, type TagInfo } from '@/api/tags'

// ========== Props & Emits ==========

const props = defineProps<{
  visible: boolean
  /** 由父组件控制的提交 loading 状态，防止 API 未完成时重复点击 */
  submitting: boolean
  /** 编辑模式时的初始数据 */
  initialData?: {
    title: string
    description: string
    investigation?: string
    solution?: string
    category: string
    priority: string
    tagIds: number[]
    status?: string
  } | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'submit': [data: {
    title: string
    description: string
    investigation: string
    solution: string
    category: string
    priority: string
    tagIds: number[]
    status: string
  }]
}>()

// ========== 计算属性 ==========

const isEdit = ref(false)

// ========== 响应式数据 ==========

const formRef = ref<FormInstance>()
const tagList = ref<TagInfo[]>([])

const formData = reactive({
  title: '',
  description: '',
  investigation: '',
  solution: '',
  category: 'OTHER',
  priority: 'P2',
  tagIds: [] as number[],
  status: 'OPEN'
})

// ========== 选项数据 ==========

const categoryOptions = [
  { label: '性能问题', value: 'PERFORMANCE' },
  { label: '异常报错', value: 'EXCEPTION' },
  { label: '配置问题', value: 'CONFIG' },
  { label: '环境问题', value: 'ENVIRONMENT' },
  { label: '业务逻辑', value: 'BUSINESS_LOGIC' },
  { label: '其他', value: 'OTHER' }
]

const priorityOptions = [
  { label: 'P0-紧急', value: 'P0' },
  { label: 'P1-高', value: 'P1' },
  { label: 'P2-中', value: 'P2' },
  { label: 'P3-低', value: 'P3' }
]

const statusOptions = [
  { label: '待解决', value: 'OPEN' },
  { label: '排查中', value: 'IN_PROGRESS' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已归档', value: 'ARCHIVED' }
]

// ========== 校验规则 ==========

const rules: FormRules = {
  title: [
    { required: true, message: '标题不能为空', trigger: 'blur' },
    { min: 1, max: 200, message: '标题长度需在1~200字符之间', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '问题描述不能为空', trigger: 'blur' },
    { max: 10000, message: '问题描述不能超过10000字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  priority: [
    { required: true, message: '请选择优先级', trigger: 'change' }
  ]
}

// ========== 方法 ==========

/** 加载标签列表 */
async function loadTags() {
  try {
    const res = await getTagListApi()
    tagList.value = res.data.records
  } catch {
    // 标签加载失败时使用空列表，不影响表单使用
    tagList.value = []
  }
}

/** 重置表单 */
function resetForm() {
  formData.title = ''
  formData.description = ''
  formData.investigation = ''
  formData.solution = ''
  formData.category = 'OTHER'
  formData.priority = 'P2'
  formData.tagIds = []
  formData.status = 'OPEN'
  formRef.value?.clearValidate()
}

/** 填充编辑数据 */
function fillEditData() {
  if (props.initialData) {
    formData.title = props.initialData.title
    formData.description = props.initialData.description
    formData.investigation = props.initialData.investigation || ''
    formData.solution = props.initialData.solution || ''
    formData.category = props.initialData.category
    formData.priority = props.initialData.priority
    formData.tagIds = [...props.initialData.tagIds]
  }
}

// 弹窗关闭时重置
function handleClosed() {
  resetForm()
  isEdit.value = false
}

// 监听 visible 变化，初始化表单
watch(
  () => [props.visible, props.initialData] as const,
  ([visible]) => {
    if (!visible) return

    loadTags()
    if (props.initialData) {
      isEdit.value = true
      fillEditData()
      return
    }

    isEdit.value = false
    resetForm()
  },
  { immediate: true, deep: true }
)

/** 提交表单 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  emit('submit', {
    title: formData.title,
    description: formData.description,
    investigation: formData.investigation,
    solution: formData.solution,
    category: formData.category,
    priority: formData.priority,
    tagIds: formData.tagIds,
    status: formData.status
  })
}
</script>
