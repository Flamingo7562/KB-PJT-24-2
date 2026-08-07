/**
 * 알바생 전용 API 서비스 — 홈 대시보드·근무 히스토리·QR 스캔.
 *
 * 확보 안심금액(earning)은 명세 DASH-001 기준의 "표시 계산값"이다.
 * 일급·근무 시각 같은 기준값은 서버가 권위를 갖고, 시간 경과에 따라 변하는
 * 적립액·진행률은 화면에서 계산한다(@/utils/earning, @/composables/useEarningTick).
 *
 * 관련 API(명세 DASH-001, WORKER-001/002, ATT-002):
 *   GET  /api/worker/home   GET /api/worker/work-cases   GET /api/worker/workplaces
 *   POST /api/attendance/scans
 */
import http, { idempotentPost } from '@/services/http'
import { USE_MOCK } from '@/services/mockFlag'

// 보건증 공유 대상 API는 필드 계약이 아직 열려 있어 이번 근태 실연동 범위에서 제외합니다.
const USE_SHARE_WORKPLACES_MOCK = true

// 오늘 근무 중(지각 출근) 시나리오 — 확보 안심금액 진행 상태를 함께 보여준다.
// todayWorkCase.status: BEFORE_WORK / LATE / NO_SHOW / NONE
// earning: 기준값(agreedWage·근무 시각)은 서버 권위, 적립액·진행률은 화면 계산. progressRatio 0~1, 금액 KRW 정수.
const mockHome = {
  wallet: { availableBalance: 320000 },
  todayWorkCase: {
    status: 'LATE',
    title: '주말 홀 서빙',
    workplaceName: '카페 봄',
    workDate: '2026-07-22',
    startTime: '10:00',
    endTime: '18:00'
  },
  earning: {
    agreedWage: 90000, // 합의 일급 전액 — 지각 차감 없음
    totalMinutes: 480, // 총 구간(휴게 포함)
    unpaidBreakMinutes: 60, // 무급 휴게
    elapsedPayDisplay: 34526, // 서버 기준 적립액 — 명세 응답 계약. 화면은 자체 계산값을 쓴다
    progressRatio: 0.42, // 총 구간 기준 진행률(0~1) — 위와 같음
    expectedNetAmount: 90000, // 일급 전액 기준 예상 실수령액
    isLate: true, // 지각 여부 — 뱃지 노출 기준(지급액에 영향 없음)
    lateMinutes: 15 // 지각 분수 — 뱃지 문구에 사용
  }
}

const mockWorkCases = [
  {
    workCaseId: 101,
    workplaceName: '강남점',
    workDate: '2026-07-22',
    time: '10:00 ~ 18:00',
    dailyWage: 90000,
    status: 'IN_PROGRESS',
    settleStatus: 'HOLD'
  },
  {
    workCaseId: 90,
    workplaceName: '홍대점',
    workDate: '2026-07-15',
    time: '09:00 ~ 15:00',
    dailyWage: 72000,
    status: 'COMPLETED',
    settleStatus: 'SETTLED'
  }
]

// 보건증 공유 대상: 확정·시작 전(ACCEPTED/READY) 근무가 있는 지점만
const mockShareWorkplaces = [
  { workplaceId: 1, workplaceName: '강남점', ownerName: '김사장' },
  { workplaceId: 3, workplaceName: '신촌점', ownerName: '최사장' }
]

/** 홈 대시보드 조회 (명세 30) */
export async function getWorkerHome() {
  if (USE_MOCK) return structuredClone(mockHome)
  const { data } = await http.get('/worker/home')
  return data
}

/** 근무 히스토리 조회 → { content[], totalPages } (WORKER-001) */
export async function listWorkerWorkCases(params = {}) {
  if (USE_MOCK) return { content: mockWorkCases.map((s) => ({ ...s })), totalPages: 1 }
  const { data } = await http.get('/worker/work-cases', { params })
  return data
}

/** 근무 예정 지점 목록(보건증 공유 드롭다운 전용) (명세 32) */
export async function listWorkerWorkplaces() {
  if (USE_SHARE_WORKPLACES_MOCK) return mockShareWorkplaces.map((w) => ({ ...w }))
  const { data } = await http.get('/worker/workplaces')
  return data
}

/**
 * QR 출퇴근 스캔 → { scanType, scanTime, isLate, lateMinutes, settleDueAt? } (ATT-002).
 * 서버가 기록 상태로 출근/퇴근 자동 판별 + QR 유효성·GPS 반경 검증(실패 422).
 * @param {object} payload qrToken, latitude, longitude
 */
export async function scan(payload, { idempotencyKey = null } = {}) {
  if (USE_MOCK) {
    return {
      result: 'RECORDED',
      scanType: 'CHECK_IN',
      recordedAt: new Date().toISOString(),
      isLate: false,
      lateMinutes: 0,
      earlyCheckoutConfirmedAt: null,
      settlementDueAt: null
    }
  }
  const { data } = await idempotentPost('/attendance/scans', payload, { idempotencyKey })
  return data
}
