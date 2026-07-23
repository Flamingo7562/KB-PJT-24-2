/**
 * 알바생 전용 API 서비스 — 홈 대시보드·근무 히스토리·QR 스캔.
 *
 * 확보 안심금액(earning)은 서버 단일 소스 계산 결과를 그대로 표시한다
 * (프론트 재계산 금지 — docs/rules/domain.md).
 *
 * 관련 API(명세 29~32):
 *   GET  /api/worker/home   GET /api/worker/shifts   GET /api/worker/workplaces
 *   POST /api/worker/scan
 */
import http from '@/services/http'

const USE_MOCK = true

// 오늘 근무 중(지각 출근) 시나리오 — 확보 안심금액 진행 상태를 함께 보여준다.
// todayShift.status: BEFORE_WORK / LATE / NO_SHOW / NONE
// earning 은 서버 계산값(그대로 표시). progressRatio 0~1, 금액 KRW 정수.
const mockHome = {
  wallet: { balance: 320000 },
  todayShift: {
    status: 'LATE',
    title: '주말 홀 서빙',
    workplaceName: '카페 봄',
    workDate: '2026-07-22',
    startTime: '10:00',
    endTime: '18:00'
  },
  earning: {
    dailyWage: 90000,
    totalMinutes: 480, // 총 구간(휴게 포함)
    unpaidBreakMinutes: 60, // 무급 휴게
    lateMinutes: 15,
    lateDeduction: 3214,
    accruedAmount: 34526, // 현재까지 확보된 금액
    progressRatio: 0.42, // 총 구간 기준 진행률(0~1)
    expectedNetAmount: 83912 // 3.3% 공제 가정 예상 실수령액(참고)
  }
}

const mockShifts = [
  {
    shiftId: 101,
    workplaceName: '강남점',
    workDate: '2026-07-22',
    time: '10:00 ~ 18:00',
    dailyWage: 90000,
    status: 'IN_PROGRESS',
    settleStatus: 'HOLD'
  },
  {
    shiftId: 90,
    workplaceName: '홍대점',
    workDate: '2026-07-15',
    time: '09:00 ~ 15:00',
    dailyWage: 72000,
    status: 'COMPLETED',
    settleStatus: 'SETTLED'
  }
]

// 보건증 공유 대상: MATCHED(확정·시작 전) 근무가 있는 지점만
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

/** 근무 히스토리 조회 → { content[], totalPages } (명세 31) */
export async function listWorkerShifts(params = {}) {
  if (USE_MOCK) return { content: mockShifts.map((s) => ({ ...s })), totalPages: 1 }
  const { data } = await http.get('/worker/shifts', { params })
  return data
}

/** 근무 예정 지점 목록(보건증 공유 드롭다운 전용) (명세 32) */
export async function listWorkerWorkplaces() {
  if (USE_MOCK) return mockShareWorkplaces.map((w) => ({ ...w }))
  const { data } = await http.get('/worker/workplaces')
  return data
}

/**
 * QR 출퇴근 스캔 → { scanType, scanTime, isLate, lateMinutes, settleDueAt? } (명세 29).
 * 서버가 기록 상태로 출근/퇴근 자동 판별 + QR 유효성·GPS 반경 검증(실패 400).
 * @param {object} payload qrToken, latitude, longitude
 */
export async function scan({ qrToken, latitude, longitude }) {
  if (USE_MOCK) {
    return {
      scanType: 'CHECK_IN',
      scanTime: new Date().toISOString(),
      isLate: false,
      lateMinutes: 0
    }
  }
  const { data } = await http.post('/worker/scan', { qrToken, latitude, longitude })
  return data
}
