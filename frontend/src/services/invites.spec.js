import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  idempotentPost: vi.fn()
}))

vi.mock('@/services/http', () => ({
  default: { get: mocks.get },
  idempotentPost: mocks.idempotentPost
}))

import { confirmInvite, getInvite } from '@/services/invites'

describe('invites service', () => {
  beforeEach(() => {
    mocks.get.mockReset()
    mocks.idempotentPost.mockReset()
  })

  it('인증된 서버 초대 조회 결과에서 승인 DTO만 반환한다', async () => {
    const detail = { title: '주말 홀 서빙', termsVersion: 3 }
    mocks.get.mockResolvedValueOnce({ data: detail })

    await expect(getInvite('invite-token-159')).resolves.toEqual(detail)
    expect(mocks.get).toHaveBeenCalledWith('/invitations/invite-token-159', {
      skipAuthRedirect: true
    })
  })

  it('수락은 서명 Payload 없이 같은 멱등 Key로 Body 없는 POST를 보낸다', async () => {
    const result = { workCaseId: 159, escrowStatus: 'HELD' }
    mocks.idempotentPost.mockResolvedValueOnce({ data: result })

    await expect(
      confirmInvite('invite-token-159', { idempotencyKey: 'accept-intent-159' })
    ).resolves.toEqual(result)

    expect(mocks.idempotentPost).toHaveBeenCalledWith(
      '/invitations/invite-token-159/accept',
      undefined,
      {
        idempotencyKey: 'accept-intent-159',
        config: { skipAuthRedirect: true }
      }
    )
  })
})
