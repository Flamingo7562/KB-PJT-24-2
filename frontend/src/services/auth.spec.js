/**
 * 인증 Service 계약 테스트.
 * 승인 계약(API_SPEC.md '인증·회원')의 요청 경로와 Body 를 고정한다.
 * 가용성 조회와 가입·로그인이 같은 정규화 값을 보내지 않으면, 사전 확인을 통과한
 * 아이디가 최종 요청에서 중복으로 거부될 수 있다.
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/services/http', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

import { checkLoginId, getSession, login, logout, signup } from '@/services/auth'
import http from '@/services/http'

const SIGNUP_REQUIRED_FIELDS = ['loginId', 'password', 'passwordConfirm', 'name', 'email', 'role']

describe('auth service', () => {
  beforeEach(() => {
    http.get.mockReset()
    http.post.mockReset()
  })

  it('세션 조회는 전역 401 리다이렉트에서 제외한다', async () => {
    http.get.mockResolvedValue({ data: { authenticated: false } })

    const session = await getSession()

    expect(session).toEqual({ authenticated: false })
    const [url, config] = http.get.mock.calls[0]
    expect(url).toBe('/auth/session')
    expect(config.skipAuthRedirect).toBe(true)
  })

  it('가용성 조회와 가입은 같은 정규화 loginId 를 보낸다', async () => {
    http.get.mockResolvedValue({ data: { available: true } })
    http.post.mockResolvedValue({ data: { userId: 1 } })

    await checkLoginId('  Owner01  ')
    await signup({
      loginId: '  Owner01  ',
      password: 'secret123',
      passwordConfirm: 'secret123',
      name: ' 김사장 ',
      email: '  Owner@Test.COM ',
      role: 'OWNER'
    })

    expect(http.get.mock.calls[0][1].params.loginId).toBe('owner01')
    expect(http.post.mock.calls[0][1].loginId).toBe('owner01')
  })

  it('가입 Body 는 승인 필드만 포함하고 전화번호가 없으면 키를 만들지 않는다', async () => {
    http.post.mockResolvedValue({ data: { userId: 1 } })

    await signup({
      loginId: 'owner01',
      password: 'secret123',
      passwordConfirm: 'secret123',
      name: '김사장',
      email: 'owner@test.com',
      phone: '',
      role: 'OWNER'
    })

    const [, body] = http.post.mock.calls[0]
    expect(Object.keys(body).sort()).toEqual([...SIGNUP_REQUIRED_FIELDS].sort())
  })

  it('전화번호를 보내면 정규화한 숫자만 싣는다', async () => {
    http.post.mockResolvedValue({ data: { userId: 1 } })

    await signup({
      loginId: 'owner01',
      password: 'secret123',
      passwordConfirm: 'secret123',
      name: '김사장',
      email: 'owner@test.com',
      phone: '010-1234-5678',
      role: 'OWNER'
    })

    expect(http.post.mock.calls[0][1].phone).toBe('01012345678')
  })

  it('비밀번호에는 정규화를 적용하지 않는다', async () => {
    http.post.mockResolvedValue({ data: { userId: 1 } })

    await signup({
      loginId: 'owner01',
      password: '  Secret123  ',
      passwordConfirm: '  Secret123  ',
      name: '김사장',
      email: 'owner@test.com',
      role: 'OWNER'
    })

    expect(http.post.mock.calls[0][1].password).toBe('  Secret123  ')
  })

  it('로그인은 역할 토글을 expectedRole 로 보낸다', async () => {
    http.post.mockResolvedValue({
      data: { role: 'OWNER', name: '김사장', needsWorkplaceSetup: true }
    })

    await login({ loginId: 'Owner01', password: 'secret123', role: 'OWNER' })

    const [url, body] = http.post.mock.calls[0]
    expect(url).toBe('/auth/login')
    expect(body).toEqual({ loginId: 'owner01', password: 'secret123', expectedRole: 'OWNER' })
  })

  it('로그아웃은 Body 없이 POST 한다', async () => {
    http.post.mockResolvedValue(undefined)

    await logout()

    expect(http.post).toHaveBeenCalledWith('/auth/logout')
  })
})
