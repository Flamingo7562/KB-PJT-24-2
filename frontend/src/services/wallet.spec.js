import { beforeEach, describe, expect, it, vi } from 'vitest'

let walletService

describe('wallet Mock service', () => {
  beforeEach(async () => {
    vi.resetModules()
    walletService = await import('@/services/wallet')
  })

  it('지갑 요약을 승인 도메인 모델로 반환한다', async () => {
    const result = await walletService.fetchWallet()

    expect(result).toEqual({
      currency: 'KRW',
      availableBalance: 1_250_000,
      lockedBalance: 480_000
    })
  })

  it('거래 Item과 Page Metadata를 함께 보존한다', async () => {
    const result = await walletService.fetchTransactions({ page: 1, size: 2 })

    expect(result.page).toEqual({ number: 1, size: 2, totalElements: 6, totalPages: 3 })
    expect(result.content).toHaveLength(2)
    expect(Object.keys(result.content[0]).sort()).toEqual(
      [
        'transactionId',
        'type',
        'amount',
        'direction',
        'availableAfter',
        'lockedAfter',
        'workCaseId',
        'workTitle',
        'workplaceName',
        'displayStatus',
        'createdAt'
      ].sort()
    )
  })

  it('사업장 필터는 사업장과 무관한 충전·출금 거래를 제외한다', async () => {
    const result = await walletService.fetchTransactions({ workplaceId: 1 })

    expect(result.content).not.toHaveLength(0)
    expect(result.content.every((tx) => tx.workCaseId != null)).toBe(true)
    expect(result.content.some((tx) => ['FUNDING', 'WITHDRAWAL'].includes(tx.type))).toBe(false)
  })

  it('Mutation 응답에는 잔액 대신 승인 식별자와 상태만 반환하고 잔액은 재조회한다', async () => {
    const before = await walletService.fetchWallet()
    const result = await walletService.chargeWallet({
      bankCode: '004',
      accountNo: '170000000001',
      pin: '0000',
      amount: 100_000
    })
    const after = await walletService.fetchWallet()

    expect(Object.keys(result).sort()).toEqual(
      ['fundingOrderId', 'status', 'bankTransactionId'].sort()
    )
    expect(result.status).toBe('COMPLETED')
    expect(result).not.toHaveProperty('availableBalance')
    expect(after.availableBalance).toBe(before.availableBalance + 100_000)
  })

  it('잘못된 Demo PIN은 계좌 존재 여부를 구분하지 않는 승인 오류로 거부한다', async () => {
    await expect(
      walletService.chargeWallet({
        bankCode: '004',
        accountNo: '170000000001',
        pin: '1234',
        amount: 100_000
      })
    ).rejects.toMatchObject({
      code: 'FORBIDDEN',
      response: {
        status: 403,
        data: { code: 'FORBIDDEN', message: '계좌를 사용할 수 없습니다.' }
      }
    })
  })

  it('출금 Mock도 withdrawalRequestId 계약을 사용한다', async () => {
    const result = await walletService.withdrawWallet({
      bankCode: '088',
      accountNo: '170000000002',
      amount: 50_000
    })

    expect(Object.keys(result).sort()).toEqual(
      ['withdrawalRequestId', 'status', 'bankTransactionId'].sort()
    )
    expect(result).not.toHaveProperty('txId')
  })
})
