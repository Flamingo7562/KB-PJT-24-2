/**
 * 근무(shift) API 서비스 — 사장 근태관리 + 근무 상세/정산/신고.
 *
 * 상태 전이: OPEN(매칭전)→MATCHED(근무전)→IN_PROGRESS(근무중)→COMPLETED(완료) / MATCHED→NO_SHOW.
 * 수정·삭제·링크생성은 OPEN 에서만(확정 후 409). 정산은 HOLD 일 때만(멱등).
 *
 * 관련 API(명세 19~25, 33~36):
 *   GET  /api/workplaces/{workplaceId}/shifts/summary
 *   GET  /api/workplaces/{workplaceId}/shifts
 *   POST /api/workplaces/{workplaceId}/shifts
 *   GET  /api/shifts/{shiftId}   PATCH /api/shifts/{shiftId}   DELETE /api/shifts/{shiftId}
 *   POST /api/shifts/{shiftId}/invites
 *   POST /api/shifts/{shiftId}/settlement/approve
 *   GET  /api/shifts/{shiftId}/owner-contact
 *   GET  /api/shifts/{shiftId}/reports   POST /api/worker/shifts/{shiftId}/reports
 */
import http from '@/services/http'

const USE_MOCK = true

// 지점별 근무 목록. 실제 API 처럼 workplaceId·keyword 로 걸러서 응답한다.
const mockShiftList = [
  {
    shiftId: 101,
    workplaceId: 1,
    workerName: '이알바',
    title: '주말 홀 서빙',
    workDate: '2026-07-22',
    startTime: '10:00',
    endTime: '18:00',
    status: 'IN_PROGRESS',
    matched: true
  },
  {
    shiftId: 102,
    workplaceId: 1,
    workerName: null,
    title: '평일 주방 보조',
    workDate: '2026-07-23',
    startTime: '09:00',
    endTime: '15:00',
    status: 'OPEN',
    matched: false
  },
  {
    shiftId: 103,
    workplaceId: 1,
    workerName: '박알바',
    title: '마감 청소',
    workDate: '2026-07-21',
    startTime: '20:00',
    endTime: '23:00',
    status: 'COMPLETED',
    matched: true
  },
  {
    shiftId: 201,
    workplaceId: 2,
    workerName: '최알바',
    title: '홍대 오픈 캐셔',
    workDate: '2026-07-24',
    startTime: '08:00',
    endTime: '14:00',
    status: 'IN_PROGRESS',
    matched: true
  },
  {
    shiftId: 202,
    workplaceId: 2,
    workerName: null,
    title: '주말 디저트 보조',
    workDate: '2026-07-25',
    startTime: '13:00',
    endTime: '19:00',
    status: 'OPEN',
    matched: false
  }
]

/** 지점 + 검색어(제목·알바생 이름) + 상태로 거르는 mock 필터 */
function filterMockShifts(workplaceId, { keyword = '', status = '' } = {}) {
  const q = String(keyword).trim().toLowerCase()
  return mockShiftList
    .filter((s) => s.workplaceId === Number(workplaceId))
    .filter((s) => status === '' || s.status === status)
    .filter(
      (s) =>
        q === '' ||
        s.title.toLowerCase().includes(q) ||
        (s.workerName ?? '').toLowerCase().includes(q)
    )
}

// 지점 이름(mock 표시용). 실제 API 는 상세 응답에 workplaceName 을 담아준다.
const MOCK_WORKPLACE_NAMES = { 1: '강남점', 2: '홍대점' }

const mockShiftDetail = {
  shiftId: 101,
  workplaceId: 1,
  workplaceName: '강남점',
  title: '주말 홀 서빙',
  workDate: '2026-07-22',
  startTime: '10:00',
  endTime: '18:00',
  breakMinutes: 60,
  breakPaid: false,
  dailyWage: 90000,
  status: 'IN_PROGRESS',
  settleStatus: 'HOLD',
  worker: { name: '이알바', badgeLevel: 2 } // 매칭된 알바생 + 성실 뱃지
}

/** 근태 현황 요약 → { recruitingCount, workingCount } (명세 19) */
export async function getShiftSummary(workplaceId) {
  if (USE_MOCK) {
    const list = filterMockShifts(workplaceId)
    return {
      recruitingCount: list.filter((s) => s.status === 'OPEN').length,
      workingCount: list.filter((s) => s.status === 'IN_PROGRESS').length
    }
  }
  const { data } = await http.get(`/workplaces/${workplaceId}/shifts/summary`)
  return data
}

/**
 * 근무 리스트 조회 → { content[], totalPages } (명세 20). 기본 최신순.
 * @param {number} workplaceId
 * @param {object} params keyword, status, date, page, size
 */
export async function listShifts(workplaceId, params = {}) {
  if (USE_MOCK) {
    const content = filterMockShifts(workplaceId, {
      keyword: params.keyword,
      status: params.status
    }).map((s) => ({ ...s }))
    return { content, totalPages: 1 }
  }
  const { data } = await http.get(`/workplaces/${workplaceId}/shifts`, { params })
  return data
}

/**
 * 근무 포지션 등록 → { shiftId } (명세 21). status=OPEN 생성.
 * @param {object} payload title, workDate, startTime, endTime, breakMinutes, breakPaid, dailyWage
 */
export async function createShift(workplaceId, payload) {
  if (USE_MOCK) return { shiftId: Date.now() }
  const { data } = await http.post(`/workplaces/${workplaceId}/shifts`, payload)
  return data
}

/** 근무 상세 조회 (명세 22). 사장/해당 알바생 */
export async function getShift(shiftId) {
  if (USE_MOCK) {
    // 목록과 같은 근무를 보여주도록 리스트 항목(지점·제목·상태)을 우선 반영한다.
    const listItem = mockShiftList.find((s) => s.shiftId === Number(shiftId))
    if (!listItem) return { ...mockShiftDetail, shiftId: Number(shiftId) }
    return {
      ...mockShiftDetail,
      ...listItem,
      shiftId: Number(shiftId),
      workplaceName: MOCK_WORKPLACE_NAMES[listItem.workplaceId] ?? mockShiftDetail.workplaceName,
      worker: listItem.workerName ? { name: listItem.workerName, badgeLevel: 2 } : null
    }
  }
  const { data } = await http.get(`/shifts/${shiftId}`)
  return data
}

/** 근무 수정 (명세 23). OPEN 만 허용, 확정 후 409 */
export async function updateShift(shiftId, payload) {
  if (USE_MOCK) return { shiftId, ...payload }
  const { data } = await http.patch(`/shifts/${shiftId}`, payload)
  return data
}

/** 근무 삭제 (명세 24). OPEN 만 허용, 확정 후 409 */
export async function deleteShift(shiftId) {
  if (USE_MOCK) return
  await http.delete(`/shifts/${shiftId}`)
}

/** 근무 연결 링크 생성 → { inviteUrl, expiresAt } (명세 25). OPEN 만, 1회성 토큰 */
export async function createInvite(shiftId) {
  if (USE_MOCK) {
    return {
      inviteUrl: `${location.origin}/invite/mock-token-${shiftId}`,
      expiresAt: '2026-07-23T23:59:59'
    }
  }
  const { data } = await http.post(`/shifts/${shiftId}/invites`)
  return data
}

/** 정산 즉시 승인 → { settledAt } (명세 34). HOLD 상태만, 멱등 */
export async function approveSettlement(shiftId) {
  if (USE_MOCK) return { settledAt: new Date().toISOString() }
  const { data } = await http.post(`/shifts/${shiftId}/settlement/approve`)
  return data
}

/** 사장 연락처 조회(문의하기) → { ownerName, phone } (명세 33). 해당 알바생 */
export async function getOwnerContact(shiftId) {
  if (USE_MOCK) return { ownerName: '김사장', phone: '010-1234-5678' }
  const { data } = await http.get(`/shifts/${shiftId}/owner-contact`)
  return data
}

/** 신고 내역 조회 → { content[] } (명세 36). 당사자만 */
export async function listReports(shiftId) {
  if (USE_MOCK) return { content: [] }
  const { data } = await http.get(`/shifts/${shiftId}/reports`)
  return data
}

/**
 * 임금분쟁 신고 제출 → { reportId } (명세 35). 기록·알림용, 정산 영향 없음.
 * @param {object} payload content(경위서)
 */
export async function createReport(shiftId, { content }) {
  if (USE_MOCK) return { reportId: Date.now() }
  const { data } = await http.post(`/worker/shifts/${shiftId}/reports`, { content })
  return data
}
