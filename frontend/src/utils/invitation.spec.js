import { describe, expect, it } from 'vitest'

import {
  invitationErrorMessage,
  isInvitationForbidden,
  shouldRetainAcceptanceKey
} from '@/utils/invitation'

function apiError(status, code, message) {
  return { response: { status, data: { code, message } }, code }
}

describe('invitation error UX', () => {
  it.each([
    ['INVITATION_EXPIRED', '만료'],
    ['INVITATION_REVOKED', '철회'],
    ['INVITATION_ALREADY_ACCEPTED', '이미 수락'],
    ['INVITATION_TERMS_CHANGED', '조건이 변경']
  ])('%s를 별도 안내한다', (code, expected) => {
    expect(invitationErrorMessage(apiError(409, code))).toContain(expected)
  })

  it('잔액 부족은 금액이나 내부 잔액 없이 안내한다', () => {
    const message = invitationErrorMessage(
      apiError(409, 'CONFLICT', '사장님의 예치 가능 잔액이 부족하여 근무를 확정할 수 없습니다.')
    )

    expect(message).toContain('임금을 예치할 수 없어')
    expect(message).not.toMatch(/\d/)
  })

  it('네트워크·5xx·처리 중 충돌에서는 같은 Key를 유지한다', () => {
    expect(shouldRetainAcceptanceKey(new Error('network'))).toBe(true)
    expect(shouldRetainAcceptanceKey(apiError(500, 'INTERNAL_ERROR'))).toBe(true)
    expect(shouldRetainAcceptanceKey(apiError(409, 'CONFLICT'))).toBe(true)
    expect(shouldRetainAcceptanceKey(apiError(409, 'INVITATION_REVOKED'))).toBe(false)
    expect(
      shouldRetainAcceptanceKey(
        apiError(409, 'CONFLICT', '사장님의 예치 가능 잔액이 부족하여 근무를 확정할 수 없습니다.')
      )
    ).toBe(false)
  })

  it('역할·권한 오류는 403 화면 대상으로 판정한다', () => {
    expect(isInvitationForbidden(apiError(403, 'ROLE_MISMATCH'))).toBe(true)
    expect(isInvitationForbidden(apiError(409, 'INVITATION_REVOKED'))).toBe(false)
  })
})
