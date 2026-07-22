/**
 * 알바생 홈 대시보드 API 서비스.
 *
 * 백엔드 연동 전 임시로 목(mock) 데이터를 반환한다.
 * USE_MOCK 를 false 로 바꾸면 실제 `/api/worker/home` 을 호출한다.
 *
 * 확보 안심금액(earning)은 서버 계산 결과를 그대로 사용한다(프론트 재계산 금지).
 * 관련 API: GET /api/worker/home
 */
import http from '@/services/http'

const USE_MOCK = true

// 오늘 근무 중(지각 출근) 시나리오 — 확보 안심금액 진행 상태를 함께 보여준다.
const mockHome = {
  wallet: { balance: 320000 },
  todayShift: {
    status: 'LATE', // BEFORE_WORK | LATE | NO_SHOW | NONE
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

export async function fetchWorkerHome() {
  if (USE_MOCK) {
    return {
      wallet: { ...mockHome.wallet },
      todayShift: { ...mockHome.todayShift },
      earning: { ...mockHome.earning }
    }
  }
  const { data } = await http.get('/worker/home')
  return data
}
