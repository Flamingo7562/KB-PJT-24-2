/**
 * 지갑 API 서비스.
 *
 * 백엔드 연동 전 임시로 목(mock) 데이터를 반환한다.
 * 백엔드가 준비되면 USE_MOCK 를 false 로 바꾸면 실제 `/api/wallet` 을 호출한다.
 * (컴포넌트·스토어는 이 함수 시그니처만 바라보므로 교체 시 변경 지점이 여기로 한정된다.)
 *
 * 충전·출금은 PortOne 모의 결제창을 흉내낸다 — 은행(bankCode)과 계좌번호(accountNo)를
 * 직접 입력받아 요청에 싣는다(계좌 등록·조회 목록 없음). 현재는 전부 mock 처리.
 *
 * 관련 API(명세 WALLET-001~004):
 *   GET /api/wallet   POST /api/wallet/funding-orders   POST /api/wallet/withdrawal-requests
 *   GET /api/wallet/transactions
 */
import http, { idempotentPost } from '@/services/http'

const USE_MOCK = true

const mockWallet = {
  availableBalance: 1250000, // 대표 잔액·출금 가능액 (예치금 미포함)
  lockedBalance: 480000 // 예치중 금액(escrow.status='HOLD')
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

/** 지갑 잔액 조회 → { availableBalance, lockedBalance } */
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
  // 페이지 응답 { content, page, size, totalElements } 은 data 래핑이 없어 본문을 그대로 반환.
  return http.get('/wallet/transactions', { params })
}

/**
 * 충전 → { availableBalance, txId } (WALLET-002). 사장 전용, PortOne 모의 결제(Mock 승인).
 * 클라이언트 금액·계좌는 신뢰하지 않는다 — 서버(추후 PortOne)가 최종 재검증.
 * Idempotency-Key(UUID) 필수 — 더블클릭·네트워크 재시도 시 동일 키로 중복 충전 방지.
 * @param {object} payload bankCode(은행), accountNo(계좌번호), amount
 */
export async function chargeWallet({ bankCode, accountNo, amount }) {
  if (USE_MOCK)
    return { availableBalance: mockWallet.availableBalance + Number(amount), txId: Date.now() }
  const { data } = await idempotentPost('/wallet/funding-orders', { bankCode, accountNo, amount })
  return data
}

/**
 * 출금 → { availableBalance, txId } (WALLET-003). 가용 잔액 초과 시 409.
 * Idempotency-Key(UUID) 필수 — 재시도 시 동일 키로 중복 출금 방지.
 * @param {object} payload bankCode(입금 은행), accountNo(계좌번호), amount
 */
export async function withdrawWallet({ bankCode, accountNo, amount }) {
  if (USE_MOCK)
    return { availableBalance: mockWallet.availableBalance - Number(amount), txId: Date.now() }
  const { data } = await idempotentPost('/wallet/withdrawal-requests', {
    bankCode,
    accountNo,
    amount
  })
  return data
}
