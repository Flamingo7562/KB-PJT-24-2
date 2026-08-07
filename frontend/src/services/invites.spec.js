import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/services/http', () => ({
  default: { get: vi.fn() },
  idempotentPost: vi.fn()
}))

import http, { idempotentPost } from '@/services/http'
import { confirmInvite, getInvite } from '@/services/invites'

describe('invites service', () => {
  beforeEach(() => {
    http.get.mockReset()
    idempotentPost.mockReset()
  })

  it('인증된 초대 조회의 승인 DTO를 그대로 반환한다', async () => {
    const invite = {
      title: '주말 홀 서빙',
      startsAt: '2026-08-20T01:00:00Z',
      endsAt: '2026-08-20T09:00:00Z',
      termsVersion: 3,
      ownerBadge: null
    }
    http.get.mockResolvedValue({ data: invite })

    await expect(getInvite('safe_token')).resolves.toEqual(invite)
    expect(http.get).toHaveBeenCalledWith('/invitations/safe_token')
  })

  it('서명 이미지나 JSON 없이 같은 멱등 Key로 Body 없는 수락을 호출한다', async () => {
    idempotentPost.mockResolvedValue({ data: { workCaseId: 42, escrowStatus: 'HELD' } })

    await expect(
      confirmInvite('safe_token', { idempotencyKey: 'accept-intent-1' })
    ).resolves.toEqual({ workCaseId: 42, escrowStatus: 'HELD' })

    expect(idempotentPost).toHaveBeenCalledWith('/invitations/safe_token/accept', undefined, {
      idempotencyKey: 'accept-intent-1',
      retries: 0
    })
  })
})
