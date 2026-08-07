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

  it('첫 CONFLICT는 서버 문구와 무관하게 금액이나 내부 잔액 없이 안내한다', () => {
    const message = invitationErrorMessage(apiError(409, 'CONFLICT', '서버 문구가 변경됨'))

    expect(message).toContain('임금을 예치할 수 없어')
    expect(message).not.toMatch(/\d/)
  })

  it('네트워크·5xx와 그 뒤 동일 의도 CONFLICT에서만 같은 Key를 유지한다', () => {
    expect(shouldRetainAcceptanceKey(new Error('network'))).toBe(true)
    expect(shouldRetainAcceptanceKey(apiError(500, 'INTERNAL_ERROR'))).toBe(true)
    expect(shouldRetainAcceptanceKey(apiError(409, 'CONFLICT', '어떤 서버 문구'))).toBe(false)
    expect(
      shouldRetainAcceptanceKey(apiError(409, 'CONFLICT', '어떤 서버 문구'), {
        requestWasUncertain: true
      })
    ).toBe(true)
    expect(shouldRetainAcceptanceKey(apiError(409, 'INVITATION_REVOKED'))).toBe(false)
  })

  it('불확실 요청의 CONFLICT 안내도 서버 문구를 파싱하지 않는다', () => {
    const message = invitationErrorMessage(apiError(409, 'CONFLICT', '완전히 다른 문구'), {
      sameIntentRetry: true
    })

    expect(message).toContain('같은 요청으로 다시 확인')
    expect(message).not.toContain('완전히 다른 문구')
  })

  it('역할·권한 오류는 403 화면 대상으로 판정한다', () => {
    expect(isInvitationForbidden(apiError(403, 'ROLE_MISMATCH'))).toBe(true)
    expect(isInvitationForbidden(apiError(409, 'INVITATION_REVOKED'))).toBe(false)
  })
})
