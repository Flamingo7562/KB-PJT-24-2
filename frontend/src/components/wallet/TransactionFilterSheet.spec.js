import { describe, expect, it } from 'vitest'

import { buildTransactionFilterParams } from '@/components/wallet/TransactionFilterSheet.vue'

describe('buildTransactionFilterParams', () => {
  it('기본값(전체·최신순·빈 값)은 sort 만 남긴다', () => {
    expect(buildTransactionFilterParams()).toEqual({ sort: 'LATEST' })
  })

  it("txType 이 'ALL' 이면 제외하고, 그 외에는 전달한다", () => {
    expect(buildTransactionFilterParams({ txType: 'ALL' })).not.toHaveProperty('txType')
    expect(buildTransactionFilterParams({ txType: 'HOLD' }).txType).toBe('HOLD')
  })

  it('검색어는 trim 하고, 빈 문자열은 제외한다', () => {
    expect(buildTransactionFilterParams({ keyword: '  서빙  ' }).keyword).toBe('서빙')
    expect(buildTransactionFilterParams({ keyword: '   ' })).not.toHaveProperty('keyword')
  })

  it('기간·금액 범위를 채우면 파라미터로 넣고, 금액은 숫자로 변환한다', () => {
    const params = buildTransactionFilterParams({
      startDate: '2026-07-01',
      endDate: '2026-07-22',
      minAmount: '10000',
      maxAmount: '500000',
      sort: 'AMOUNT'
    })
    expect(params).toEqual({
      sort: 'AMOUNT',
      startDate: '2026-07-01',
      endDate: '2026-07-22',
      minAmount: 10000,
      maxAmount: 500000
    })
  })

  it('빈 금액 문자열은 제외한다', () => {
    const params = buildTransactionFilterParams({ minAmount: '', maxAmount: '' })
    expect(params).not.toHaveProperty('minAmount')
    expect(params).not.toHaveProperty('maxAmount')
  })
})
