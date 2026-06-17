import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 用户状态管理
 * 存储当前登录用户的角色信息
 */
export const useUserStore = defineStore('user', () => {
  const role = ref<string>('')
  const username = ref<string>('')

  /** 设置用户角色 */
  function setRole(r: string) {
    role.value = r
  }

  /** 设置用户名 */
  function setUsername(name: string) {
    username.value = name
  }

  /** 清除用户状态（退出登录时调用） */
  function clear() {
    role.value = ''
    username.value = ''
  }

  function canWrite() {
    const normalizedRole = normalizeRole(role.value)
    return normalizedRole === 'ADMIN' || normalizedRole === 'DEVELOPER'
  }

  return { role, username, setRole, setUsername, clear, canWrite }
})

function normalizeRole(role: string) {
  return role.replace(/^ROLE_/, '').toUpperCase()
}
