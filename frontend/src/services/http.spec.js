import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  post: vi.fn(),
  requestUse: vi.fn(),
  responseUse: vi.fn()
}))

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      interceptors: {
        request: { use: mocks.requestUse },
        response: { use: mocks.responseUse }
      },
      post: mocks.post
    }))
  }
}))

import { authRequiredRedirect, idempotentPost } from '@/services/http'

describe('authRequiredRedirect', () => {
  it('초대 화면의 401은 WORKER 로그인과 원래 경로로 보낸다', () => {
    expect(authRequiredRedirect('/invitations/abc_DEF-123')).toBe(
      '/worker/login?redirect=%2Finvitations%2Fabc_DEF-123'
    )
  })

  it('일반 보호 화면의 401은 온보딩 복귀 경로를 유지한다', () => {
    expect(authRequiredRedirect('/owner/attendance', '?page=2')).toBe(
      '/?redirect=%2Fowner%2Fattendance%3Fpage%3D2'
    )
  })
})

describe('idempotentPost', () => {
  beforeEach(() => {
    mocks.post.mockReset()
  })

  it('자동 재시도와 이후 동일 의도 재시도에서 호출자가 보존한 키를 유지한다', async () => {
    const networkError = new Error('network')
    mocks.post.mockRejectedValueOnce(networkError).mockResolvedValueOnce({ data: { ok: true } })

    await idempotentPost(
      '/wallet/funding-orders',
      { bankCode: '004' },
      { retries: 1, idempotencyKey: 'funding-intent-203' }
    )

    expect(mocks.post).toHaveBeenCalledTimes(2)
    for (const [, , config] of mocks.post.mock.calls) {
      expect(config.headers['Idempotency-Key']).toBe('funding-intent-203')
    }

    mocks.post.mockResolvedValueOnce({ data: { ok: true } })
    await idempotentPost(
      '/wallet/funding-orders',
      { bankCode: '004' },
      { retries: 0, idempotencyKey: 'funding-intent-203' }
    )

    expect(mocks.post.mock.calls[2][2].headers['Idempotency-Key']).toBe('funding-intent-203')
  })

  it('Body를 생략한 요청은 JSON null 대신 undefined를 전송한다', async () => {
    mocks.post.mockResolvedValue({ data: { ok: true } })

    await idempotentPost('/invitations/token/accept', undefined, {
      idempotencyKey: 'accept-intent-1'
    })

    expect(mocks.post).toHaveBeenCalledWith(
      '/invitations/token/accept',
      undefined,
      expect.objectContaining({
        headers: expect.objectContaining({ 'Idempotency-Key': 'accept-intent-1' })
      })
    )
  })
})
