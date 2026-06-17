import request from './request'

// ========== 类型定义 ==========

/** 标签基本信息（下拉选择器用） */
export interface TagInfo {
  id: number
  name: string
}

/** 标签列表项（管理页表格用） */
export interface TagVO {
  id: number
  name: string
  noteCount: number
  createTime: string
}

/** 创建标签请求参数 */
export interface CreateTagParams {
  name: string
}

/** 编辑标签请求参数 */
export interface UpdateTagParams {
  id: number
  name: string
}

/** 删除标签请求参数 */
export interface DeleteTagParams {
  id: number
}

/** 删除标签响应 */
export interface DeleteTagResult {
  deletedNoteTags: number
}

/** 分页查询请求参数 */
export interface TagListParams {
  pageNum?: number
  pageSize?: number
}

/** 分页查询响应 */
export interface TagListResult {
  total: number
  pageNum: number
  pageSize: number
  records: TagVO[]
}

// ========== API 方法 ==========

/**
 * 创建标签
 * POST /api/tags/create
 */
export function createTagApi(params: CreateTagParams) {
  return request.post<TagVO>('/tags/create', params)
}

/**
 * 编辑标签
 * POST /api/tags/update
 */
export function updateTagApi(params: UpdateTagParams) {
  return request.post<TagVO>('/tags/update', params)
}

/**
 * 删除标签
 * POST /api/tags/delete
 */
export function deleteTagApi(params: DeleteTagParams) {
  return request.post<DeleteTagResult>('/tags/delete', params)
}

/**
 * 分页查询标签列表
 * POST /api/tags/list
 * @param params 分页参数，不传则默认 pageNum=1 pageSize=999（兼容旧调用）
 */
export function getTagListApi(params?: TagListParams) {
  return request.post<TagListResult>('/tags/list', params || { pageNum: 1, pageSize: 999 })
}

/**
 * 获取全部标签（下拉选择器用，无分页）
 * POST /api/tags/all
 */
export function getAllTagsApi() {
  return request.post<TagInfo[]>('/tags/all', {})
}
