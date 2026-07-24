/**
 * 근무(work_case) API 서비스 — 사장 근태관리 + 근무 상세/정산/신고.
 *
 * 상태 전이(8단계, @/constants/workCaseStatus):
 *   DRAFT→INVITED→ACCEPTED→READY→IN_PROGRESS→COMPLETED / 확정 계열 NO_SHOW / CANCELED.
 * 수정·삭제·링크생성은 DRAFT 에서만(확정 후 409 WORK_CASE_LOCKED). 정산은 HOLD 일 때만(멱등).
 *
 * 관련 API(명세 WORK-001~006, INVITE-001, SETTLE-002, CONTACT-001, DISPUTE-001/002):
 *   GET  /api/workplaces/{workplaceId}/work-cases/summary
 *   GET  /api/workplaces/{workplaceId}/work-cases
 *   POST /api/workplaces/{workplaceId}/work-cases
 *   GET  /api/work-cases/{workCaseId}   PATCH /api/work-cases/{workCaseId}   DELETE /api/work-cases/{workCaseId}
 *   POST /api/work-cases/{workCaseId}/invitations
 *   POST /api/work-cases/{workCaseId}/settlement/approve
 *   GET  /api/work-cases/{workCaseId}/workplace-contact
 *   GET  /api/work-cases/{workCaseId}/disputes   POST /api/work-cases/{workCaseId}/disputes
 */
import http, { idempotentPost } from '@/services/http'

const USE_MOCK = true

// 지점별 근무 목록. 실제 API 처럼 workplaceId·keyword 로 걸러서 응답한다.
// status 는 8단계 enum(@/constants/workCaseStatus). 요약 6버킷을 골고루 보이도록 구성.
const mockWorkCaseList = [
  {
    workCaseId: 101,
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
    workCaseId: 102,
    workplaceId: 1,
    workerName: null,
    title: '평일 주방 보조',
    workDate: '2026-07-23',
    startTime: '09:00',
    endTime: '15:00',
    status: 'DRAFT',
    matched: false
  },
  {
    workCaseId: 103,
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
    workCaseId: 104,
    workplaceId: 1,
    workerName: null,
    title: '금요일 저녁 서빙',
    workDate: '2026-07-25',
    startTime: '17:00',
    endTime: '22:00',
    status: 'INVITED',
    matched: false
  },
  {
    workCaseId: 105,
    workplaceId: 1,
    workerName: '한알바',
    title: '토요일 브런치',
    workDate: '2026-07-26',
    startTime: '09:00',
    endTime: '14:00',
    status: 'READY',
    matched: true
  },
  {
    workCaseId: 201,
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
    workCaseId: 202,
    workplaceId: 2,
    workerName: null,
    title: '주말 디저트 보조',
    workDate: '2026-07-25',
    startTime: '13:00',
    endTime: '19:00',
    status: 'DRAFT',
    matched: false
  },
  {
    workCaseId: 203,
    workplaceId: 2,
    workerName: '김알바',
    title: '평일 마감 정리',
    workDate: '2026-07-20',
    startTime: '18:00',
    endTime: '23:00',
    status: 'NO_SHOW',
    matched: true
  }
]

/** 지점 + 검색어(제목·알바생 이름) + 상태로 거르는 mock 필터 */
function filterMockWorkCases(workplaceId, { keyword = '', status = '' } = {}) {
  const q = String(keyword).trim().toLowerCase()
  return mockWorkCaseList
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

const mockWorkCaseDetail = {
  workCaseId: 101,
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

/**
 * 근태 현황 요약 → 6버킷 카운트 (WORK-001).
 * { draft, invited, ready, inProgress, completed, noShow } — @/constants/workCaseStatus WORK_CASE_SUMMARY.
 */
export async function getWorkCaseSummary(workplaceId) {
  if (USE_MOCK) {
    const list = filterMockWorkCases(workplaceId)
    const count = (status) => list.filter((s) => s.status === status).length
    return {
      draft: count('DRAFT'),
      invited: count('INVITED'),
      ready: count('READY'),
      inProgress: count('IN_PROGRESS'),
      completed: count('COMPLETED'),
      noShow: count('NO_SHOW')
    }
  }
  const { data } = await http.get(`/workplaces/${workplaceId}/work-cases/summary`)
  return data
}

/**
 * 근무 리스트 조회 → { content[], totalPages } (WORK-002). 기본 최신순.
 * @param {number} workplaceId
 * @param {object} params keyword, status, date, page, size
 */
export async function listWorkCases(workplaceId, params = {}) {
  if (USE_MOCK) {
    const content = filterMockWorkCases(workplaceId, {
      keyword: params.keyword,
      status: params.status
    }).map((s) => ({ ...s }))
    return { content, totalPages: 1 }
  }
  // 페이지 응답 { content, page, size, totalElements } 은 data 래핑이 없어 본문을 그대로 반환.
  return http.get(`/workplaces/${workplaceId}/work-cases`, { params })
}

/**
 * 근무 포지션 등록 → { workCaseId } (WORK-003). status=DRAFT 생성.
 * @param {object} payload title, workDate, startTime, endTime, breakMinutes, breakPaid, dailyWage
 */
export async function createWorkCase(workplaceId, payload) {
  if (USE_MOCK) return { workCaseId: Date.now() }
  const { data } = await http.post(`/workplaces/${workplaceId}/work-cases`, payload)
  return data
}

/** 근무 상세 조회 (WORK-004). 사장/해당 알바생 */
export async function getWorkCase(workCaseId) {
  if (USE_MOCK) {
    // 목록과 같은 근무를 보여주도록 리스트 항목(지점·제목·상태)을 우선 반영한다.
    const listItem = mockWorkCaseList.find((s) => s.workCaseId === Number(workCaseId))
    if (!listItem) return { ...mockWorkCaseDetail, workCaseId: Number(workCaseId) }
    return {
      ...mockWorkCaseDetail,
      ...listItem,
      workCaseId: Number(workCaseId),
      workplaceName: MOCK_WORKPLACE_NAMES[listItem.workplaceId] ?? mockWorkCaseDetail.workplaceName,
      worker: listItem.workerName ? { name: listItem.workerName, badgeLevel: 2 } : null
    }
  }
  const { data } = await http.get(`/work-cases/${workCaseId}`)
  return data
}

/** 근무 수정 (WORK-005). DRAFT 만 허용, 확정 후 409 WORK_CASE_LOCKED */
export async function updateWorkCase(workCaseId, payload) {
  if (USE_MOCK) return { workCaseId, ...payload }
  const { data } = await http.patch(`/work-cases/${workCaseId}`, payload)
  return data
}

/** 근무 삭제 (WORK-006). DRAFT 만 허용, 확정 후 409 WORK_CASE_LOCKED */
export async function deleteWorkCase(workCaseId) {
  if (USE_MOCK) return
  await http.delete(`/work-cases/${workCaseId}`)
}

/** 근무 연결 링크 생성 → { inviteUrl, expiresAt } (INVITE-001). DRAFT 만, 1회성 토큰 */
export async function createInvite(workCaseId) {
  if (USE_MOCK) {
    return {
      inviteUrl: `${location.origin}/invitations/mock-token-${workCaseId}`,
      expiresAt: '2026-07-23T23:59:59'
    }
  }
  const { data } = await http.post(`/work-cases/${workCaseId}/invitations`)
  return data
}

/**
 * 정산 즉시 승인 → { settledAt } (SETTLE-002). HOLD 상태만.
 * Idempotency-Key(UUID) 필수 — 재시도 시 동일 키로 중복 지급 방지.
 */
export async function approveSettlement(workCaseId) {
  if (USE_MOCK) return { settledAt: new Date().toISOString() }
  const { data } = await idempotentPost(`/work-cases/${workCaseId}/settlement/approve`)
  return data
}

/** 사장 연락처 조회(문의하기) → { ownerName, phone } (CONTACT-001). 해당 알바생 */
export async function getOwnerContact(workCaseId) {
  if (USE_MOCK) return { ownerName: '김사장', phone: '010-1234-5678' }
  const { data } = await http.get(`/work-cases/${workCaseId}/workplace-contact`)
  return data
}

/** 신고 내역 조회 → { content[] } (DISPUTE-002). 당사자만 */
export async function listReports(workCaseId) {
  if (USE_MOCK) return { content: [] }
  // 페이지 응답 { content, ... } 은 data 래핑이 없어 본문을 그대로 반환.
  return http.get(`/work-cases/${workCaseId}/disputes`)
}

/**
 * 임금분쟁 신고 제출 → { reportId } (DISPUTE-001). 기록·알림용, 정산 영향 없음.
 * @param {object} payload content(경위서)
 */
export async function createReport(workCaseId, { content }) {
  if (USE_MOCK) return { reportId: Date.now() }
  const { data } = await http.post(`/work-cases/${workCaseId}/disputes`, { content })
  return data
}
