/**
 * 日期格式化工具函数
 * 项目未引入 dayjs，使用原生 Date 实现常用格式转换
 */

/**
 * 标准化日期字符串：将纳秒精度 ISO 截断为毫秒精度（3位小数）
 */
function normalizeDateStr(dateStr: string): string {
  return dateStr.replace(/\.(\d{3})\d*/, '.$1')
}

/**
 * 安全的 Date 解析：兼容 ISO 字符串、时间戳、Date 对象
 */
function safeDate(dateStr: string | number | Date | undefined | null): Date {
  if (!dateStr) return new Date(NaN)
  if (dateStr instanceof Date) return dateStr
  if (typeof dateStr === 'number') return new Date(dateStr)
  return new Date(normalizeDateStr(dateStr))
}

/**
 * 格式化日期为 yyyy-MM-dd HH:mm
 */
export function formatDateTime(dateStr: string | number | Date | undefined | null): string {
  const d = safeDate(dateStr)
  if (isNaN(d.getTime())) return '-'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * 格式化日期为 yyyy-MM-dd HH:mm:ss
 */
export function formatFullDateTime(dateStr: string | number | Date | undefined | null): string {
  const d = safeDate(dateStr)
  if (isNaN(d.getTime())) return '-'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
