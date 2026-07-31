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

// 백엔드 WalletTransaction Item과 같은 필드·Type을 사용하는 화면용 Mock.
const mockTransactions = [
  {
    transactionId: 6,
    type: 'ESCROW_HOLD',
    amount: 90000,
    availableAfter: 1250000,
    lockedAfter: 480000,
    workCaseId: 106,
    workTitle: '주말 홀 서빙',
    workplaceName: '기가 허브',
    displayStatus: '예치중',
    createdAt: '2026-07-22T00:10:00Z'
  },
  {
    transactionId: 5,
    type: 'FUNDING',
    amount: 500000,
    availableAfter: 1340000,
    lockedAfter: 390000,
    workCaseId: null,
    workTitle: null,
    workplaceName: null,
    displayStatus: '충전',
    createdAt: '2026-07-21T09:32:00Z'
  },
  {
    transactionId: 4,
    type: 'ESCROW_RELEASE',
    amount: 120000,
    availableAfter: 840000,
    lockedAfter: 270000,
    workCaseId: 104,
    workTitle: '평일 주방 보조',
    workplaceName: '기가 허브',
    displayStatus: '지급완료',
    createdAt: '2026-07-19T23:05:00Z'
  },
  {
    transactionId: 3,
    type: 'ESCROW_REFUND',
    amount: 100000,
    availableAfter: 960000,
    lockedAfter: 150000,
    workCaseId: 103,
    workTitle: '홀 마감',
    workplaceName: '기가 허브',
    displayStatus: '환불',
    createdAt: '2026-07-19T13:40:00Z'
  },
  {
    transactionId: 2,
    type: 'WITHDRAWAL',
    amount: 300000,
    availableAfter: 860000,
    lockedAfter: 150000,
    workCaseId: null,
    workTitle: null,
    workplaceName: null,
    displayStatus: '출금',
    createdAt: '2026-07-18T02:15:00Z'
  },
  {
    transactionId: 1,
    type: 'FUNDING',
    amount: 1160000,
    availableAfter: 1160000,
    lockedAfter: 0,
    workCaseId: null,
    workTitle: null,
    workplaceName: null,
    displayStatus: '충전',
    createdAt: '2026-07-17T05:02:00Z'
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
 * @param {object} params workplaceId, from, to, type, sort, minAmount, maxAmount, keyword, page, size
 */
export async function fetchTransactions(params = {}) {
  if (USE_MOCK) {
    return {
      content: [...mockTransactions],
      page: { number: 0, size: 20, totalElements: mockTransactions.length, totalPages: 1 }
    }
  }
  const { data } = await http.get('/wallet/transactions', { params })
  return data
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
