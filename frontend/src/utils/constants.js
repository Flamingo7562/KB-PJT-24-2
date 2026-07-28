/**
 * 화면 공통 상수 — 상태 라벨·색 토큰·코드 목록.
 *
 * 화면 표기 용어와 색을 여기서 단일 관리한다(도메인 규칙 docs/rules/domain.md 기준).
 * - `label`: 화면에 노출할 한글 문구(예: DB enum `SETTLED` → '정산완료').
 * - `color`: base.css 색 변수 문자열. 컴포넌트는 이 값을 그대로 style 에 바인딩한다.
 *
 * 아이콘 매핑(lucide)은 이 파일이 아니라 StatusChip.vue 가 담당한다(여기는 순수 데이터).
 *
 * 근무(work_case) 8단계 상태 매핑은 여기가 아니라 `@/constants/workCaseStatus` 단일 소스에 있다.
 */
import bankBusan from '@/assets/images/banks/busan.png'
import bankCity from '@/assets/images/banks/city.png'
import bankCu from '@/assets/images/banks/cu.png'
import bankDgb from '@/assets/images/banks/dgb.png'
import bankGwangju from '@/assets/images/banks/gwangju.png'
import bankHana from '@/assets/images/banks/hana.png'
import bankIbk from '@/assets/images/banks/ibk.png'
import bankIm from '@/assets/images/banks/im.png'
import bankK from '@/assets/images/banks/k.png'
import bankKakao from '@/assets/images/banks/kakao.png'
import bankKb from '@/assets/images/banks/kb.png'
import bankKdb from '@/assets/images/banks/kdb.png'
import bankMg from '@/assets/images/banks/mg.png'
import bankNh from '@/assets/images/banks/nh.png'
import bankPost from '@/assets/images/banks/post.png'
import bankSc from '@/assets/images/banks/sc.png'
import bankSh from '@/assets/images/banks/sh.png'
import bankShinhan from '@/assets/images/banks/shinhan.png'
import bankToss from '@/assets/images/banks/toss.png'
import bankWoori from '@/assets/images/banks/woori.png'

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

/* ---- 송금상세 필터(GET /api/wallet/transactions) 선택지 ----
 * 화면(송금상세 리스트)에 실제로 노출되는 거래 구분과 필터 유형을 통일한 목록.
 * txType 은 서버 파라미터로만 전달된다(프론트 재계산 없음 — docs/rules/domain.md).
 * 주의(명세 WALLET-004): 공식 txType 화이트리스트는 ALL/PAID/REFUND/HOLD 이며
 * CHARGE·WITHDRAW 는 화면 파생값이다. BE 연동 시 필터 화이트리스트를 함께 넓혀야 한다. */
export const TX_TYPE_FILTER = [
  { value: 'ALL', label: '전체' },
  { value: 'PAID', label: '정산완료' },
  { value: 'HOLD', label: '예치중' },
  { value: 'CHARGE', label: '충전' },
  { value: 'WITHDRAW', label: '출금' },
  { value: 'REFUND', label: '환불' }
]
export const TX_SORT = [
  { value: 'LATEST', label: '최신순' },
  { value: 'OLDEST', label: '오래된순' },
  { value: 'AMOUNT_DESC', label: '금액 높은순' },
  { value: 'AMOUNT_ASC', label: '금액 낮은순' }
]

/* ---- 오늘의 알바 일정 카드(GET /api/worker/home todayWorkCase.status) ---- */
export const TODAY_WORK_CASE_STATUS = {
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
  WORK_CASE_CONFIRMED: { label: '근무 확정' },
  ESCROW_HELD: { label: '예치 완료' },
  SETTLED: { label: '정산 완료' },
  REFUNDED: { label: '노쇼 환불' },
  DOC_SHARED: { label: '보건증 공유' },
  WAGE_REPORTED: { label: '임금분쟁 신고' }
}

/* ---- QR 스캔 결과(POST /api/attendance/scans scanType) ---- */
export const SCAN_TYPE = {
  CHECK_IN: { label: '출근' },
  CHECK_OUT: { label: '퇴근' }
}

/**
 * 은행 목록(충전·출금 은행 선택).
 * `logo`: assets/images/banks/*.png 로고. `chip`: 로고 로드 실패 시 폴백 색.
 *
 * BANKS       = 은행 선택 그리드에 바로 노출하는 주요 은행.
 * BANKS_EXTRA = "기타 은행" 시트에서만 추가로 노출하는 은행.
 * BANKS_ALL   = 전체(주요 + 기타). 코드→은행 조회는 이 전체 목록에서 한다.
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

/* 기타 은행 — "기타 은행" 시트에서 노출. dgb/im/cu 는 로고 매핑이 확실치 않아 추후 확인 필요. */
export const BANKS_EXTRA = [
  { code: 'KBANK', name: '케이뱅크', logo: bankK, chip: '#2A2A2A' },
  { code: 'BUSAN', name: '부산은행', logo: bankBusan, chip: '#E60012' },
  { code: 'DGB', name: 'DGB대구은행', logo: bankDgb, chip: '#005BAC' }, // ⚠️ iM뱅크로 리브랜딩 — im 과 중복 확인
  { code: 'IM', name: 'iM뱅크', logo: bankIm, chip: '#E4002B' }, // ⚠️ dgb 와 동일 은행일 수 있음
  { code: 'GWANGJU', name: '광주은행', logo: bankGwangju, chip: '#009999' },
  { code: 'SC', name: 'SC제일은행', logo: bankSc, chip: '#0B7A75' },
  { code: 'CITI', name: '씨티은행', logo: bankCity, chip: '#003B70' },
  { code: 'KDB', name: 'KDB산업은행', logo: bankKdb, chip: '#003DA5' },
  { code: 'SH', name: '수협은행', logo: bankSh, chip: '#0089CF' },
  { code: 'MG', name: '새마을금고', logo: bankMg, chip: '#00A6E2' },
  { code: 'CU', name: '신협', logo: bankCu, chip: '#0069B4' }, // ⚠️ 로고가 신협인지 확인 필요
  { code: 'POST', name: '우체국', logo: bankPost, chip: '#E5001E' }
]

export const BANKS_ALL = [...BANKS, ...BANKS_EXTRA]

/** 은행 코드 → 은행 객체 조회(전체 목록 기준) */
export function findBank(code) {
  return BANKS_ALL.find((b) => b.code === code) ?? null
}
