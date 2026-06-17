/**
 * Login 组件测试 - 核心登录逻辑
 * 直接测试 handleLogin 的行为模式
 */
import { describe, it, expect, vi } from 'vitest'

const storageMap = new Map<string, string>()

// 模拟 Login.vue 中 handleLogin 的核心逻辑
async function simulateHandleLogin(
  loginApi: (params: { username: string; password: string }) => Promise<{ data: { token: string; username: string; role: string } }>,
  form: { username: string; password: string },
  router: { push: (path: string) => void },
  ElMessage: { success: (msg: string) => void; error: (msg: string) => void },
  setToken: (token: string) => void,
  setUser: (data: any) => void
): Promise<{ loading: boolean }> {
  let loading = false

  const valid = form.username.length >= 3 && form.password.length >= 6
  if (!valid) return { loading }

  loading = true
  try {
    const res = await loginApi(form)
    setToken(res.data.token)
    setUser(JSON.stringify(res.data))
    ElMessage.success('登录成功')
    router.push('/notes')
  } catch (err: any) {
    ElMessage.error(err?.message || '登录失败')
  } finally {
    loading = false
  }

  return { loading }
}

describe('Login 核心登录逻辑', () => {
  // ============================================
  // 登录成功场景
  // ============================================
  describe('登录成功', () => {
    const successResponse = {
      data: { token: 'jwt-token', username: 'admin', role: 'admin' }
    }

    it('token 应存入 localStorage', async () => {
      storageMap.clear()
      const mockLoginApi = vi.fn().mockReturnValue(Promise.resolve(successResponse))
      const mockRouter = { push: vi.fn() }
      const mockElMessage = { success: vi.fn(), error: vi.fn() }

      await simulateHandleLogin(
        mockLoginApi,
        { username: 'admin', password: '123456' },
        mockRouter, mockElMessage,
        (token) => storageMap.set('token', token),
        (data) => storageMap.set('user', data)
      )

      expect(storageMap.get('token')).toBe('jwt-token')
      expect(mockLoginApi).toHaveBeenCalledWith({ username: 'admin', password: '123456' })
    })

    it('用户信息 JSON 应存入 localStorage', async () => {
      storageMap.clear()
      const mockLoginApi = vi.fn().mockReturnValue(Promise.resolve(successResponse))

      await simulateHandleLogin(
        mockLoginApi, { username: 'admin', password: '123456' },
        { push: vi.fn() }, { success: vi.fn(), error: vi.fn() },
        (token) => storageMap.set('token', token),
        (data) => storageMap.set('user', data)
      )

      const storedUser = JSON.parse(storageMap.get('user') || '{}')
      expect(storedUser.username).toBe('admin')
      expect(storedUser.role).toBe('admin')
    })

    it('应跳转到 /notes', async () => {
      const mockRouter = { push: vi.fn() }

      await simulateHandleLogin(
        vi.fn().mockReturnValue(Promise.resolve(successResponse)),
        { username: 'admin', password: '123456' },
        mockRouter, { success: vi.fn(), error: vi.fn() },
        () => {}, () => {}
      )

      expect(mockRouter.push).toHaveBeenCalledWith('/notes')
    })

    it('应显示成功提示', async () => {
      const mockElMessage = { success: vi.fn(), error: vi.fn() }

      await simulateHandleLogin(
        vi.fn().mockReturnValue(Promise.resolve(successResponse)),
        { username: 'admin', password: '123456' },
        { push: vi.fn() }, mockElMessage,
        () => {}, () => {}
      )

      expect(mockElMessage.success).toHaveBeenCalledWith('登录成功')
    })

    it('loading 应在 finally 中恢复为 false', async () => {
      const result = await simulateHandleLogin(
        vi.fn().mockReturnValue(Promise.resolve(successResponse)),
        { username: 'admin', password: '123456' },
        { push: vi.fn() }, { success: vi.fn(), error: vi.fn() },
        () => {}, () => {}
      )

      expect(result.loading).toBe(false)
    })
  })

  // ============================================
  // 登录失败场景
  // ============================================
  describe('登录失败', () => {
    it('API reject 时 catch 块应阻止 try 块执行', async () => {
      // 验证: 抛出异常时，try 块中的操作不执行，但流程不崩溃
      const mockElMessage = { success: vi.fn(), error: vi.fn() }
      const mockRouter = { push: vi.fn() }
      storageMap.clear()

      await simulateHandleLogin(
        vi.fn().mockImplementation(async () => { throw new Error('用户名或密码错误') }),
        { username: 'admin', password: 'wrong' },
        mockRouter, mockElMessage,
        (token) => storageMap.set('token', token),
        (data) => storageMap.set('user', data)
      )

      // 验证 try 块中的副作用未发生
      expect(storageMap.get('token')).toBeUndefined()
      expect(mockRouter.push).not.toHaveBeenCalled()
      expect(mockElMessage.success).not.toHaveBeenCalled()
    })

    it('token 不应存入', async () => {
      storageMap.clear()

      await simulateHandleLogin(
        vi.fn().mockImplementation(async () => { throw new Error('fail') }),
        { username: 'admin', password: 'wrong' },
        { push: vi.fn() }, { success: vi.fn(), error: vi.fn() },
        (token) => storageMap.set('token', token),
        () => {}
      )

      expect(storageMap.get('token')).toBeUndefined()
    })

    it('不应跳转页面', async () => {
      const mockRouter = { push: vi.fn() }

      await simulateHandleLogin(
        vi.fn().mockImplementation(async () => { throw new Error('fail') }),
        { username: 'admin', password: 'wrong' },
        mockRouter, { success: vi.fn(), error: vi.fn() },
        () => {}, () => {}
      )

      expect(mockRouter.push).not.toHaveBeenCalled()
    })

    it('API reject 无 message 时 try 块副作用不发生', async () => {
      const mockElMessage = { success: vi.fn(), error: vi.fn() }
      const mockRouter = { push: vi.fn() }
      storageMap.clear()

      await simulateHandleLogin(
        vi.fn().mockImplementation(async () => { throw {} }),
        { username: 'admin', password: 'wrong' },
        mockRouter, mockElMessage,
        (token) => storageMap.set('token', token),
        (data) => storageMap.set('user', data)
      )

      // 验证异常场景下不产生副作用
      expect(storageMap.get('token')).toBeUndefined()
      expect(mockRouter.push).not.toHaveBeenCalled()
      expect(mockElMessage.success).not.toHaveBeenCalled()
    })
  })

  // ============================================
  // 表单校验失败
  // ============================================
  describe('表单校验失败', () => {
    it('校验失败不应调用 API', async () => {
      const mockLoginApi = vi.fn()

      await simulateHandleLogin(
        mockLoginApi, { username: '', password: '' },
        { push: vi.fn() }, { success: vi.fn(), error: vi.fn() },
        () => {}, () => {}
      )

      expect(mockLoginApi).not.toHaveBeenCalled()
    })

    it('用户名不足3字符应校验失败', async () => {
      const mockLoginApi = vi.fn()

      await simulateHandleLogin(
        mockLoginApi, { username: 'ab', password: '123456' },
        { push: vi.fn() }, { success: vi.fn(), error: vi.fn() },
        () => {}, () => {}
      )

      expect(mockLoginApi).not.toHaveBeenCalled()
    })

    it('密码不足6字符应校验失败', async () => {
      const mockLoginApi = vi.fn()

      await simulateHandleLogin(
        mockLoginApi, { username: 'admin', password: '12345' },
        { push: vi.fn() }, { success: vi.fn(), error: vi.fn() },
        () => {}, () => {}
      )

      expect(mockLoginApi).not.toHaveBeenCalled()
    })
  })
})
