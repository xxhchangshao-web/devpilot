import request from './request'

// ========== 类型定义 ==========

/** 笔记列表项（list 接口返回） */
export interface NoteListItem {
  id: number
  title: string
  status: string
  priority: string
  category: string
  tags: { id: number; name: string }[]
  creatorName: string
  created_at: string
  updated_at: string
}

/** 笔记详情（detail 接口返回） */
export interface NoteDetailInfo {
  id: number
  title: string
  description: string
  investigation: string
  solution: string
  status: string
  priority: string
  category: string
  tags: { id: number; name: string }[]
  creatorId: number
  creatorName: string
  created_at: string
  updated_at: string
  canEdit: boolean
}

/** list 接口请求参数 */
export interface NoteListParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  tagIds?: number[]
  status?: string
  priority?: string
  category?: string
}

/** list 接口响应 */
export interface NoteListResult {
  records: NoteListItem[]
  total: number
  pageNum: number
  pageSize: number
}

/** detail 接口请求参数 */
export interface NoteDetailParams {
  id: number
}

/** create 接口请求参数 */
export interface NoteCreateParams {
  title: string
  description: string
  investigation?: string
  solution?: string
  status?: string
  priority?: string
  category?: string
  tagIds?: number[]
}

/** create 接口响应 */
export interface NoteCreateResult {
  id: number
  title: string
  status: string
  created_at: string
  updated_at: string
}

/** update 接口请求参数 */
export interface NoteUpdateParams {
  id: number
  title: string
  description: string
  investigation?: string
  solution?: string
  priority?: string
  category?: string
  tagIds?: number[]
}

/** update 接口响应 */
export interface NoteUpdateResult {
  id: number
  title: string
  updated_at: string
}

/** delete 接口请求参数 */
export interface NoteDeleteParams {
  id: number
}

/** changeStatus 接口请求参数 */
export interface NoteChangeStatusParams {
  id: number
  status: string
}

/** changeStatus 接口响应 */
export interface NoteChangeStatusResult {
  id: number
  status: string
  previousStatus: string
  updated_at: string
}

// ========== API 方法 ==========

/**
 * 分页查询笔记列表
 * POST /api/notes/list
 */
export function listNotesApi(params: NoteListParams) {
  return request.post<NoteListResult>('/notes/list', params)
}

/**
 * 获取笔记详情
 * POST /api/notes/detail
 */
export function getNoteDetailApi(params: NoteDetailParams) {
  return request.post<NoteDetailInfo>('/notes/detail', params)
}

/**
 * 创建笔记
 * POST /api/notes/create
 */
export function createNoteApi(params: NoteCreateParams) {
  return request.post<NoteCreateResult>('/notes/create', params)
}

/**
 * 编辑笔记
 * POST /api/notes/update
 */
export function updateNoteApi(params: NoteUpdateParams) {
  return request.post<NoteUpdateResult>('/notes/update', params)
}

/**
 * 删除笔记
 * POST /api/notes/delete
 */
export function deleteNoteApi(params: NoteDeleteParams) {
  return request.post('/notes/delete', params)
}

/**
 * 更改笔记状态
 * POST /api/notes/changeStatus
 */
export function changeNoteStatusApi(params: NoteChangeStatusParams) {
  return request.post<NoteChangeStatusResult>('/notes/changeStatus', params)
}
