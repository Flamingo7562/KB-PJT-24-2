/**
 * 화면 공통 상수 — 상태 라벨·색 토큰·코드 목록.
 *
 * 화면 표기 용어와 색을 여기서 단일 관리한다(도메인 규칙 docs/rules/domain.md 기준).
 * - `label`: 화면에 노출할 한글 문구(예: DB enum `OPEN` → '매칭전').
 * - `color`: base.css 색 변수 문자열. 컴포넌트는 이 값을 그대로 style 에 바인딩한다.
 *
 * 아이콘 매핑(lucide)은 이 파일이 아니라 StatusChip.vue 가 담당한다(여기는 순수 데이터).
 */
import bankHana from '@/assets/images/banks/hana.png'
import bankIbk from '@/assets/images/banks/ibk.png'
import bankKakao from '@/assets/images/banks/kakao.png'
import bankKb from '@/assets/images/banks/kb.png'
import bankNh from '@/assets/images/banks/nh.png'
import bankShinhan from '@/assets/images/banks/shinhan.png'
import bankToss from '@/assets/images/banks/toss.png'
import bankWoori from '@/assets/images/banks/woori.png'

/* ---- 근무(shift) 상태 : OPEN→MATCHED→IN_PROGRESS→COMPLETED / MATCHED→NO_SHOW ---- */
export const SHIFT_STATUS = {
  OPEN: { label: '매칭전', color: 'var(--color-text-sub)' },
  MATCHED: { label: '근무전', color: 'var(--color-owner)' },
  IN_PROGRESS: { label: '근무중', color: 'var(--color-primary)' },
  COMPLETED: { label: '완료', color: 'var(--color-success)' },
  NO_SHOW: { label: '노쇼', color: 'var(--color-danger)' }
}

/* ---- 정산·에스크로 상태 ---- */
export const SETTLE_STATUS = {
  NONE: { label: '정산대기', color: 'var(--color-text-sub)' },
  HOLD: { label: '예치중', color: 'var(--color-brand)' },
  SETTLED: { label: '정산완료', color: 'var(--color-success)' },
  REFUNDED: { label: '환불완료', color: 'var(--color-text-sub)' }
}

/* ---- 거래(wallet_transaction) 유형 ---- */
export const TX_TYPE = {
  CHARGE: { label: '충전' },
  WITHDRAW: { label: '출금' },
  SETTLEMENT: { label: '정산 입금' }, // 알바생 안심지갑 임금 정산(+)
  ESCROW_HOLD: { label: '예치' },
  ESCROW_REFUND: { label: '환불' }
}

/* ---- 거래 상태 칩 ---- */
export const TX_STATUS = {
  DONE: { label: '완료', color: 'var(--color-text-sub)' },
  HOLD: { label: '예치중', color: 'var(--color-brand)' },
  SETTLED: { label: '정산완료', color: 'var(--color-success)' },
  REFUNDED: { label: '환불완료', color: 'var(--color-text-sub)' }
}

/* ---- 송금상세 필터(GET /api/wallet/transactions) 선택지 ---- */
export const TX_TYPE_FILTER = [
  { value: 'ALL', label: '전체' },
  { value: 'PAID', label: '지급완료' },
  { value: 'REFUND', label: '환불' },
  { value: 'HOLD', label: '예치중' }
]
export const TX_SORT = [
  { value: 'LATEST', label: '최신순' },
  { value: 'OLDEST', label: '오래된순' },
  { value: 'AMOUNT', label: '금액순' }
]

/* ---- 오늘의 알바 일정 카드(GET /api/worker/home todayShift.status) ---- */
export const TODAY_SHIFT_STATUS = {
  BEFORE_WORK: { label: '출근 전', color: 'var(--color-owner)' },
  LATE: { label: '지각', color: 'var(--color-warning)' },
  NO_SHOW: { label: '노쇼', color: 'var(--color-danger)' },
  NONE: { label: '오늘 알바 없음', color: 'var(--color-text-sub)' }
}

/* ---- 문서 유형·출처 ---- */
export const DOC_TYPE = {
  CONTRACT: { label: '근로계약서' },
  HEALTH_CERT: { label: '보건증' }
}
export const DOC_SOURCE = {
  OWN: { label: '내 문서' },
  SHARED: { label: '공유받음' }
}

/* ---- 신뢰 뱃지(GET /api/users/me/badge) ---- */
export const BADGE_TYPE = {
  TRUST_WORKER: {
    role: 'worker',
    criterionLabel: '성실근로',
    criterionDesc: '*성실근로란? 지각·결근 없이 정상 출퇴근 완료'
  },
  TRUST_OWNER: {
    role: 'owner',
    criterionLabel: '안심거래',
    criterionDesc: '*안심거래란? 임금분쟁 신고 없이 정상 정산 완료'
  }
}

/* ---- 알림 유형(GET /api/notifications notiType) ---- */
export const NOTI_TYPE = {
  SHIFT_CONFIRMED: { label: '근무 확정' },
  ESCROW_HELD: { label: '예치 완료' },
  SETTLED: { label: '정산 완료' },
  REFUNDED: { label: '노쇼 환불' },
  DOC_SHARED: { label: '보건증 공유' },
  WAGE_REPORTED: { label: '임금분쟁 신고' }
}

/* ---- QR 스캔 결과(POST /api/worker/scan scanType) ---- */
export const SCAN_TYPE = {
  CHECK_IN: { label: '출근' },
  CHECK_OUT: { label: '퇴근' }
}

/**
 * 은행 목록(충전·출금 은행 선택).
 * `logo`: assets/images/banks/*.png 로고. `chip`: 로고 로드 실패 시 폴백 색.
 */
export const BANKS = [
  { code: 'KB', name: '국민은행', logo: bankKb, chip: '#FFCC00' },
  { code: 'SHINHAN', name: '신한은행', logo: bankShinhan, chip: '#0046FF' },
  { code: 'WOORI', name: '우리은행', logo: bankWoori, chip: '#0067AC' },
  { code: 'HANA', name: '하나은행', logo: bankHana, chip: '#008485' },
  { code: 'NH', name: '농협은행', logo: bankNh, chip: '#19A94B' },
  { code: 'IBK', name: '기업은행', logo: bankIbk, chip: '#004C97' },
  { code: 'KAKAO', name: '카카오뱅크', logo: bankKakao, chip: '#FFE300' },
  { code: 'TOSS', name: '토스뱅크', logo: bankToss, chip: '#0064FF' }
]

/** 은행 코드 → 은행 객체 조회 */
export function findBank(code) {
  return BANKS.find((b) => b.code === code) ?? null
}
