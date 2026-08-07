/**
 * 지갑 API 서비스.
 *
 * #150에서는 승인 계약과 동일한 화면용 Mock 모델을 유지한다. 실제 Session·CSRF·멱등
 * HTTP 전환과 USE_MOCK 제거는 #151, 충전 PIN 입력 흐름은 #203의 책임이다.
 */
import http, { idempotentPost } from '@/services/http'

const USE_MOCK = true
const DEFAULT_PAGE = 0
const DEFAULT_SIZE = 20
const MAX_SIZE = 100

const mockWallet = {
  currency: 'KRW',
  availableBalance: 1_250_000,
  lockedBalance: 480_000
}

// workplaceId는 Mock 필터 전용 내부 값이며 API Item에는 노출하지 않는다.
const mockTransactions = [
  {
    workplaceId: 1,
    transactionId: 6,
    type: 'ESCROW_HOLD',
    amount: 90_000,
    direction: 'DEBIT',
    availableAfter: 1_250_000,
    lockedAfter: 480_000,
    workCaseId: 106,
    workTitle: '주말 홀 서빙',
    workplaceName: '기가 허브',
    displayStatus: 'COMPLETED',
    createdAt: '2026-07-22T00:10:00Z'
  },
  {
    workplaceId: null,
    transactionId: 5,
    type: 'FUNDING',
    amount: 500_000,
    direction: 'CREDIT',
    availableAfter: 1_340_000,
    lockedAfter: 390_000,
    workCaseId: null,
    workTitle: null,
    workplaceName: null,
    displayStatus: 'COMPLETED',
    createdAt: '2026-07-21T09:32:00Z'
  },
  {
    workplaceId: 1,
    transactionId: 4,
    type: 'ESCROW_RELEASE',
    amount: 120_000,
    direction: 'DEBIT',
    availableAfter: 840_000,
    lockedAfter: 270_000,
    workCaseId: 104,
    workTitle: '평일 주방 보조',
    workplaceName: '기가 허브',
    displayStatus: 'COMPLETED',
    createdAt: '2026-07-19T23:05:00Z'
  },
  {
    workplaceId: 1,
    transactionId: 3,
    type: 'ESCROW_REFUND',
    amount: 100_000,
    direction: 'CREDIT',
    availableAfter: 960_000,
    lockedAfter: 150_000,
    workCaseId: 103,
    workTitle: '홀 마감',
    workplaceName: '기가 허브',
    displayStatus: 'REFUNDED',
    createdAt: '2026-07-19T13:40:00Z'
  },
  {
    workplaceId: null,
    transactionId: 2,
    type: 'WITHDRAWAL',
    amount: 300_000,
    direction: 'DEBIT',
    availableAfter: 860_000,
    lockedAfter: 150_000,
    workCaseId: null,
    workTitle: null,
    workplaceName: null,
    displayStatus: 'COMPLETED',
    createdAt: '2026-07-18T02:15:00Z'
  },
  {
    workplaceId: null,
    transactionId: 1,
    type: 'FUNDING',
    amount: 1_160_000,
    direction: 'CREDIT',
    availableAfter: 1_160_000,
    lockedAfter: 0,
    workCaseId: null,
    workTitle: null,
    workplaceName: null,
    displayStatus: 'COMPLETED',
    createdAt: '2026-07-17T05:02:00Z'
  }
]

let nextTransactionId = 7
let nextFundingOrderId = 10
let nextWithdrawalRequestId = 11
let nextBankTransactionId = 20

function toTransactionItem({ workplaceId, ...item }) {
  void workplaceId
  return { ...item }
}

function toPageNumber(value, fallback) {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback
}

function filterMockTransactions(params) {
  const keyword = String(params.keyword ?? '')
    .trim()
    .toLowerCase()
  const workplaceId =
    params.workplaceId == null || params.workplaceId === '' ? null : Number(params.workplaceId)

  return mockTransactions
    .filter((tx) => workplaceId == null || tx.workplaceId === workplaceId)
    .filter((tx) => !params.type || tx.type === params.type)
    .filter((tx) => !params.from || tx.createdAt.slice(0, 10) >= params.from)
    .filter((tx) => !params.to || tx.createdAt.slice(0, 10) <= params.to)
    .filter((tx) => params.minAmount == null || tx.amount >= Number(params.minAmount))
    .filter((tx) => params.maxAmount == null || tx.amount <= Number(params.maxAmount))
    .filter((tx) => {
      if (!keyword) return true
      return [tx.workTitle, tx.workplaceName].some((value) =>
        String(value ?? '')
          .toLowerCase()
          .includes(keyword)
      )
    })
    .sort((left, right) => {
      switch (params.sort) {
        case 'OLDEST':
          return left.createdAt.localeCompare(right.createdAt)
        case 'AMOUNT_ASC':
          return left.amount - right.amount || right.createdAt.localeCompare(left.createdAt)
        case 'AMOUNT_DESC':
          return right.amount - left.amount || right.createdAt.localeCompare(left.createdAt)
        default:
          return right.createdAt.localeCompare(left.createdAt)
      }
    })
}

/** 지갑 잔액 조회 → { currency, availableBalance, lockedBalance } */
export async function fetchWallet() {
  if (USE_MOCK) return { ...mockWallet }
  const { data } = await http.get('/wallet')
  return data
}

/**
 * 송금상세(거래내역) 조회.
 * @param {object} params workplaceId, from, to, type, minAmount, maxAmount, keyword, sort, page, size
 */
export async function fetchTransactions(params = {}) {
  if (USE_MOCK) {
    const page = toPageNumber(params.page, DEFAULT_PAGE)
    const size = Math.min(toPageNumber(params.size, DEFAULT_SIZE) || DEFAULT_SIZE, MAX_SIZE)
    const filtered = filterMockTransactions(params)
    const start = page * size
    return {
      content: filtered.slice(start, start + size).map(toTransactionItem),
      page: {
        number: page,
        size,
        totalElements: filtered.length,
        totalPages: filtered.length === 0 ? 0 : Math.ceil(filtered.length / size)
      }
    }
  }
  const { data } = await http.get('/wallet/transactions', { params })
  return data
}

/** 충전 성공 응답에는 최신 잔액을 합치지 않는다. PIN 입력과 수명주기는 #203에서 완성한다. */
export async function chargeWallet(
  { bankCode, accountNo, pin, amount },
  { idempotencyKey = null } = {}
) {
  if (USE_MOCK) {
    if (pin !== '0000') {
      // 계좌 존재·상태·PIN 불일치는 같은 외부 경계로 숨겨 계좌 정보를 추론하지 못하게 한다.
      const error = new Error('계좌를 사용할 수 없습니다.')
      error.code = 'FORBIDDEN'
      error.response = {
        status: 403,
        data: { code: 'FORBIDDEN', message: error.message, fieldErrors: [] }
      }
      throw error
    }
    const normalizedAmount = Number(amount)
    mockWallet.availableBalance += normalizedAmount
    const result = {
      fundingOrderId: nextFundingOrderId++,
      status: 'COMPLETED',
      bankTransactionId: nextBankTransactionId++
    }
    mockTransactions.unshift({
      workplaceId: null,
      transactionId: nextTransactionId++,
      type: 'FUNDING',
      amount: normalizedAmount,
      direction: 'CREDIT',
      availableAfter: mockWallet.availableBalance,
      lockedAfter: mockWallet.lockedBalance,
      workCaseId: null,
      workTitle: null,
      workplaceName: null,
      displayStatus: 'COMPLETED',
      createdAt: new Date().toISOString()
    })
    return result
  }
  const { data } = await idempotentPost(
    '/wallet/funding-orders',
    {
      bankCode,
      accountNo,
      pin,
      amount
    },
    { idempotencyKey }
  )
  return data
}

/** 출금 성공 응답에는 최신 잔액을 합치지 않고 GET /wallet 재조회 결과를 사용한다. */
export async function withdrawWallet({ bankCode, accountNo, amount }) {
  if (USE_MOCK) {
    const normalizedAmount = Number(amount)
    mockWallet.availableBalance -= normalizedAmount
    const result = {
      withdrawalRequestId: nextWithdrawalRequestId++,
      status: 'COMPLETED',
      bankTransactionId: nextBankTransactionId++
    }
    mockTransactions.unshift({
      workplaceId: null,
      transactionId: nextTransactionId++,
      type: 'WITHDRAWAL',
      amount: normalizedAmount,
      direction: 'DEBIT',
      availableAfter: mockWallet.availableBalance,
      lockedAfter: mockWallet.lockedBalance,
      workCaseId: null,
      workTitle: null,
      workplaceName: null,
      displayStatus: 'COMPLETED',
      createdAt: new Date().toISOString()
    })
    return result
  }
  const { data } = await idempotentPost('/wallet/withdrawal-requests', {
    bankCode,
    accountNo,
    amount
  })
  return data
}
