/**
 * 지갑 API 서비스.
 *
 * 백엔드 연동 전 임시로 목(mock) 데이터를 반환한다.
 * 백엔드가 준비되면 USE_MOCK 를 false 로 바꾸면 실제 `/api/wallet` 을 호출한다.
 * (컴포넌트·스토어는 이 함수 시그니처만 바라보므로 교체 시 변경 지점이 여기로 한정된다.)
 *
 * 관련 API(명세 15~18):
 *   GET /api/wallet   POST /api/wallet/charge   POST /api/wallet/withdraw
 *   GET /api/wallet/transactions
 */
import http from '@/services/http'

const USE_MOCK = true

const mockWallet = {
  balance: 1250000, // 가용 잔액
  heldAmount: 480000 // 예치중 합계(escrow.status='HOLD')
}

// 사장 관점 거래 이력. txType: CHARGE/WITHDRAW/ESCROW_HOLD/ESCROW_REFUND
// status: HOLD(예치중)/SETTLED(정산완료)/REFUNDED(환불완료)/DONE(완료)
const mockTransactions = [
  {
    txId: 6,
    txType: 'ESCROW_HOLD',
    direction: 'DEBIT',
    amount: 90000,
    balanceAfter: 1250000,
    description: '주말 홀 서빙 · 예치',
    status: 'HOLD',
    createdAt: '2026-07-22T09:10:00'
  },
  {
    txId: 5,
    txType: 'CHARGE',
    direction: 'CREDIT',
    amount: 500000,
    balanceAfter: 1340000,
    description: '지갑 충전',
    status: 'DONE',
    createdAt: '2026-07-21T18:32:00'
  },
  {
    txId: 4,
    txType: 'ESCROW_HOLD',
    direction: 'DEBIT',
    amount: 120000,
    balanceAfter: 840000,
    description: '평일 주방 보조 · 예치',
    status: 'SETTLED',
    createdAt: '2026-07-20T08:05:00'
  },
  {
    txId: 3,
    txType: 'ESCROW_REFUND',
    direction: 'CREDIT',
    amount: 100000,
    balanceAfter: 960000,
    description: '노쇼 환불 · 홀 마감',
    status: 'REFUNDED',
    createdAt: '2026-07-19T22:40:00'
  },
  {
    txId: 2,
    txType: 'WITHDRAW',
    direction: 'DEBIT',
    amount: 300000,
    balanceAfter: 860000,
    description: '출금 · 국민은행',
    status: 'DONE',
    createdAt: '2026-07-18T11:15:00'
  },
  {
    txId: 1,
    txType: 'CHARGE',
    direction: 'CREDIT',
    amount: 1160000,
    balanceAfter: 1160000,
    description: '지갑 충전',
    status: 'DONE',
    createdAt: '2026-07-17T14:02:00'
  }
]

/** 지갑 잔액·예치중 금액 조회 */
export async function fetchWallet() {
  if (USE_MOCK) return { ...mockWallet }
  const { data } = await http.get('/wallet')
  return data
}

/**
 * 송금상세(거래내역) 조회.
 * @param {object} params workplaceId, startDate, endDate, txType, sort, minAmount, maxAmount, keyword, page, size
 */
export async function fetchTransactions(params = {}) {
  if (USE_MOCK) return { content: [...mockTransactions], totalPages: 1 }
  const { data } = await http.get('/wallet/transactions', { params })
  return data
}

/**
 * 충전 → { balance, txId } (명세 16). 사장 전용, Mock 승인.
 * 추후 PortOne 교체 지점(클라이언트 금액 불신 — 서버 재검증).
 * @param {object} payload bankCode, accountNo, amount
 */
export async function chargeWallet({ bankCode, accountNo, amount }) {
  if (USE_MOCK) return { balance: mockWallet.balance + Number(amount), txId: Date.now() }
  const { data } = await http.post('/wallet/charge', { bankCode, accountNo, amount })
  return data
}

/**
 * 출금 → { balance, txId } (명세 17). 가용 잔액 초과 시 400.
 * @param {object} payload bankCode, accountNo, amount
 */
export async function withdrawWallet({ bankCode, accountNo, amount }) {
  if (USE_MOCK) return { balance: mockWallet.balance - Number(amount), txId: Date.now() }
  const { data } = await http.post('/wallet/withdraw', { bankCode, accountNo, amount })
  return data
}
