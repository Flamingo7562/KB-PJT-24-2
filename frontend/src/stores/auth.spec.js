/**
 * 인증 Store 계약 테스트.
 * 승인 계약(API_SPEC.md:91)은 앱 최초 실행·로그인 성공 후·로그아웃 성공 후 세 시점에
 * CSRF 를 다시 준비하도록 요구한다. 로그인 시 서버가 Session ID 를 교체하면서 CSRF
 * Token 도 회전하므로, 재준비가 빠지면 로그인 직후 첫 상태변경 요청이 403 이 된다.
 */
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/services/auth', () => ({
  fetchCsrf: vi.fn(),
  getSession: vi.fn(),
  login: vi.fn(),
  logout: vi.fn()
}))

import { fetchCsrf, getSession, login as loginApi, logout as logoutApi } from '@/services/auth'
import { useAuthStore } from '@/stores/auth'
import { useWorkplaceStore } from '@/stores/workplace'

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    fetchCsrf.mockReset().mockResolvedValue(undefined)
    getSession.mockReset().mockResolvedValue({ authenticated: false })
    loginApi.mockReset()
    logoutApi.mockReset().mockResolvedValue(undefined)
  })

  it('로그인 성공 후 CSRF 를 다시 준비한다', async () => {
    loginApi.mockResolvedValue({ role: 'OWNER', name: '김사장', needsWorkplaceSetup: true })
    const auth = useAuthStore()

    await auth.login({ loginId: 'owner01', password: 'secret123', role: 'OWNER' })

    expect(fetchCsrf).toHaveBeenCalledTimes(1)
    expect(fetchCsrf.mock.invocationCallOrder[0]).toBeGreaterThan(
      loginApi.mock.invocationCallOrder[0]
    )
  })

  it('로그인이 실패하면 CSRF 를 다시 준비하지 않는다', async () => {
    loginApi.mockRejectedValue(new Error('AUTH_REQUIRED'))
    const auth = useAuthStore()

    await expect(
      auth.login({ loginId: 'owner01', password: 'wrong', role: 'OWNER' })
    ).rejects.toThrow()

    expect(fetchCsrf).not.toHaveBeenCalled()
  })

  it('로그아웃 성공 후 CSRF 를 다시 준비한다', async () => {
    const auth = useAuthStore()
    auth.setUser({ name: '김사장', role: 'OWNER', needsWorkplaceSetup: false })

    await auth.logout()

    expect(fetchCsrf).toHaveBeenCalledTimes(1)
  })

  it('로그아웃은 사업장 Context 까지 비운다', async () => {
    const auth = useAuthStore()
    const workplace = useWorkplaceStore()
    auth.setUser({ name: '김사장', role: 'OWNER', needsWorkplaceSetup: false })
    workplace.workplaces = [{ workplaceId: 1, name: '강남점', status: 'ACTIVE' }]
    workplace.selectedId = 1
    workplace.loaded = true

    await auth.logout()

    expect(auth.user).toBeNull()
    expect(workplace.workplaces).toEqual([])
    expect(workplace.selectedId).toBeNull()
    expect(workplace.loaded).toBe(false)
  })

  it('refreshSession 은 CSRF 를 다시 준비하지 않고 세션만 갱신한다', async () => {
    getSession.mockResolvedValue({
      authenticated: true,
      role: 'OWNER',
      name: '김사장',
      needsWorkplaceSetup: false
    })
    const auth = useAuthStore()

    const session = await auth.refreshSession()

    expect(session.needsWorkplaceSetup).toBe(false)
    expect(auth.needsWorkplaceSetup).toBe(false)
    expect(fetchCsrf).not.toHaveBeenCalled()
  })

  it('bootstrap 은 CSRF 준비 후 세션을 복원한다', async () => {
    const auth = useAuthStore()

    await auth.bootstrap()

    expect(fetchCsrf).toHaveBeenCalledTimes(1)
    expect(fetchCsrf.mock.invocationCallOrder[0]).toBeLessThan(
      getSession.mock.invocationCallOrder[0]
    )
    expect(auth.ready).toBe(true)
  })
})
