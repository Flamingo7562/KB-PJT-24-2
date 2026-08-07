/**
 * 지갑 Service 계약 테스트 — 실제 HTTP 경로의 URL·Params·Body를 고정한다.
 * Mock 분기는 mockFlag(VITE_USE_MOCK)가 개발 환경에서만 켜므로 여기서는 다루지 않는다.
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/services/http', () => ({
  default: { get: vi.fn() },
  idempotentPost: vi.fn()
}))

import http, { idempotentPost } from '@/services/http'
import { chargeWallet, fetchTransactions, fetchWallet, withdrawWallet } from '@/services/wallet'

describe('wallet service', () => {
  beforeEach(() => {
    http.get.mockReset()
    idempotentPost.mockReset()
  })

  it('지갑 잔액을 GET /wallet으로 조회하고 data를 그대로 반환한다', async () => {
    http.get.mockResolvedValue({
      data: { currency: 'KRW', availableBalance: 100_000, lockedBalance: 20_000 }
    })

    const result = await fetchWallet()

    expect(http.get).toHaveBeenCalledWith('/wallet')
    expect(result).toEqual({ currency: 'KRW', availableBalance: 100_000, lockedBalance: 20_000 })
  })

  it('거래 조회는 승인 Query Parameter를 그대로 GET /wallet/transactions에 전달한다', async () => {
    const page = { number: 0, size: 20, totalElements: 1, totalPages: 1 }
    http.get.mockResolvedValue({ data: { content: [{ transactionId: 1 }], page } })
    const params = {
      workplaceId: 7,
      from: '2026-07-01',
      to: '2026-07-31',
      type: 'FUNDING',
      minAmount: 1_000,
      maxAmount: 5_000,
      keyword: '허브',
      sort: 'AMOUNT_ASC',
      page: 1,
      size: 10
    }

    const result = await fetchTransactions(params)

    expect(http.get).toHaveBeenCalledWith('/wallet/transactions', { params })
    expect(result).toEqual({ content: [{ transactionId: 1 }], page })
  })

  it('충전은 멱등 POST로 bankCode/accountNo/pin/amount만 보낸다', async () => {
    idempotentPost.mockResolvedValue({
      data: { fundingOrderId: 10, status: 'COMPLETED', bankTransactionId: 20 }
    })

    const result = await chargeWallet({
      bankCode: '004',
      accountNo: '170000000001',
      pin: '0000',
      amount: 100_000
    })

    expect(idempotentPost).toHaveBeenCalledWith('/wallet/funding-orders', {
      bankCode: '004',
      accountNo: '170000000001',
      pin: '0000',
      amount: 100_000
    })
    expect(result).toEqual({ fundingOrderId: 10, status: 'COMPLETED', bankTransactionId: 20 })
  })

  it('출금은 멱등 POST로 bankCode/accountNo/amount만 보내고 pin을 포함하지 않는다', async () => {
    idempotentPost.mockResolvedValue({
      data: { withdrawalRequestId: 11, status: 'COMPLETED', bankTransactionId: 21 }
    })

    const result = await withdrawWallet({
      bankCode: '088',
      accountNo: '170000000002',
      amount: 50_000
    })

    const [url, body] = idempotentPost.mock.calls[0]
    expect(url).toBe('/wallet/withdrawal-requests')
    expect(body).toEqual({ bankCode: '088', accountNo: '170000000002', amount: 50_000 })
    expect(result).toEqual({ withdrawalRequestId: 11, status: 'COMPLETED', bankTransactionId: 21 })
  })
})
