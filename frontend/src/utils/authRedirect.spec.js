/**
 * 로그인 후 진입 분기 계약 테스트.
 * needsWorkplaceSetup 은 서버가 ACTIVE 사업장 0개일 때만 true 로 계산한다(API_SPEC.md:233).
 * 이 값을 무시하고 항상 등록 화면으로 보내면 사업장이 있는 OWNER 도 매번 등록 화면을 본다.
 * WORKER 는 어떤 경우에도 사업장 등록으로 강제 이동하지 않는다.
 */
import { describe, expect, it } from 'vitest'

import { resolveOwnerLoginRedirect, resolveWorkerLoginRedirect } from '@/utils/authRedirect'

describe('resolveOwnerLoginRedirect', () => {
  it('사업장 설정이 필요하면 등록 화면으로 보낸다', () => {
    expect(resolveOwnerLoginRedirect({ role: 'OWNER', needsWorkplaceSetup: true })).toBe(
      '/owner/workplaces/new'
    )
  })

  it('사업장이 있으면 OWNER 홈으로 보낸다', () => {
    expect(resolveOwnerLoginRedirect({ role: 'OWNER', needsWorkplaceSetup: false })).toBe(
      '/owner/home'
    )
  })

  it('설정이 필요 없으면 복귀 경로를 우선한다', () => {
    expect(
      resolveOwnerLoginRedirect({ role: 'OWNER', needsWorkplaceSetup: false }, '/owner/attendance')
    ).toBe('/owner/attendance')
  })

  it('설정이 필요하면 복귀 경로보다 등록 화면이 우선한다', () => {
    expect(
      resolveOwnerLoginRedirect({ role: 'OWNER', needsWorkplaceSetup: true }, '/owner/attendance')
    ).toBe('/owner/workplaces/new')
  })

  it('needsWorkplaceSetup 이 없으면 홈으로 보낸다', () => {
    expect(resolveOwnerLoginRedirect({ role: 'OWNER' })).toBe('/owner/home')
  })
})

describe('resolveWorkerLoginRedirect', () => {
  it('복귀 경로가 있으면 그곳으로 보낸다', () => {
    expect(resolveWorkerLoginRedirect('/invitations/abc123')).toBe('/invitations/abc123')
  })

  it('복귀 경로가 없으면 WORKER 홈으로 보낸다', () => {
    expect(resolveWorkerLoginRedirect(null)).toBe('/worker/home')
  })

  it('사업장 등록으로 보내지 않는다', () => {
    expect(resolveWorkerLoginRedirect(null, { role: 'WORKER', needsWorkplaceSetup: true })).toBe(
      '/worker/home'
    )
  })

  it.each([
    'https://evil.example/invitations/token',
    '//evil.example/invitations/token',
    '/owner/home',
    '/invitations/token?next=/owner/home',
    '/invitations/token#fragment'
  ])('외부·다른 역할·변형 경로 %s는 복귀하지 않는다', (redirect) => {
    expect(resolveWorkerLoginRedirect(redirect)).toBe('/worker/home')
  })

  it('복수 Query 값도 첫 번째 안전한 초대 경로만 사용한다', () => {
    expect(resolveWorkerLoginRedirect(['/invitations/abc_123', '/owner/home'])).toBe(
      '/invitations/abc_123'
    )
  })
})
