<template>
  <el-select
    :model-value="modelValue"
    multiple
    filterable
    :placeholder="placeholder"
    :disabled="loadError"
    style="width: 100%"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-option
      v-for="tag in tagList"
      :key="tag.id"
      :label="tag.name"
      :value="tag.id"
    />
  </el-select>
  <!-- 无可用标签时显示提示 -->
  <template v-if="!loadError && tagList.length === 0 && loaded">
    <span class="tag-selector-empty">暂无可用标签</span>
  </template>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllTagsApi, type TagInfo } from '@/api/tags'

// ========== Props & Emits ==========

defineProps<{
  modelValue: number[]
  placeholder?: string
}>()

defineEmits<{
  'update:modelValue': [value: number[]]
}>()

// ========== 状态 ==========

const tagList = ref<TagInfo[]>([])
const loaded = ref(false)
const loadError = ref(false)

// ========== 生命周期 ==========

onMounted(async () => {
  try {
    const res = await getAllTagsApi()
    tagList.value = res.data || []
  } catch {
    loadError.value = true
    ElMessage.error('加载标签列表失败')
  } finally {
    loaded.value = true
  }
})
</script>

<style scoped>
.tag-selector-empty {
  display: block;
  padding: 0 12px;
  font-size: 13px;
  color: #c0c4cc;
  line-height: 34px;
}
</style>
