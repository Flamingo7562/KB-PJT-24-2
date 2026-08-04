/**
 * 회원 서비스 계약 테스트 — 승인 프로필 계약을 고정한다.
 * PATCH /api/users/me 는 phone 외 필드를 무시하지 않고 400 으로 거부하므로,
 * 서비스가 승인 Body 만 만들어 보내는지 확인한다.
 */
import { describe, expect, it } from 'vitest'

import { getMe, updateMe } from '@/services/users'

const APPROVED_PROFILE_FIELDS = ['loginId', 'email', 'name', 'phone', 'role', 'status']

describe('getMe', () => {
  it('승인 응답 필드만 반환한다', async () => {
    const me = await getMe()
    expect(Object.keys(me).sort()).toEqual([...APPROVED_PROFILE_FIELDS].sort())
  })

  it('미승인 필드를 포함하지 않는다', async () => {
    const me = await getMe()
    expect(me).not.toHaveProperty('profileImageUrl')
    expect(me).not.toHaveProperty('passwordHash')
  })

  it('전화번호를 구분 문자 없는 정규화 형식으로 반환한다', async () => {
    const me = await getMe()
    expect(me.phone).toMatch(/^\d+$/)
  })
})

describe('updateMe', () => {
  it('전화번호를 정규화해 반영한다', async () => {
    const updated = await updateMe({ phone: '010-9999-8888' })
    expect(updated.phone).toBe('01099998888')
  })

  it('불변 필드는 요청과 무관하게 유지된다', async () => {
    const before = await getMe()
    const updated = await updateMe({ phone: '01055554444' })
    expect(updated.loginId).toBe(before.loginId)
    expect(updated.email).toBe(before.email)
    expect(updated.name).toBe(before.name)
    expect(updated.role).toBe(before.role)
    expect(updated.status).toBe(before.status)
  })

  it('응답에도 승인 필드만 남는다', async () => {
    const updated = await updateMe({ phone: '01012345678' })
    expect(Object.keys(updated).sort()).toEqual([...APPROVED_PROFILE_FIELDS].sort())
  })
})
