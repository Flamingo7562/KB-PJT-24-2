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

import { idempotentPost } from '@/services/http'

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
})
