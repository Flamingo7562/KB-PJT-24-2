/**
 * 근무(work_case) API 서비스 — 사장 근태관리 + 근무 상세/정산/신고.
 *
 * 상태 전이(8단계, @/constants/workCaseStatus):
 *   DRAFT→ACCEPTED→READY→IN_PROGRESS→(CHECK_OUT_MISSING)→COMPLETED / 확정 계열 NO_SHOW / CANCELED.
 * 수정·삭제·링크생성은 DRAFT 에서만(확정 후 409 WORK_CASE_LOCKED). 정산은 HOLD 일 때만(멱등).
 *
 * 근무 DRAFT CRUD·요약·목록·초대 발급(#158)은 실제 Session·CSRF HTTP로 연결되어 있다.
 * 정산 즉시승인·사장 연락처·임금분쟁(M6)은 USE_MOCK_SETTLEMENT_DISPUTE로 별도 관리하며
 * 이번 실연동 범위 밖이다.
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
import { USE_MOCK } from '@/services/mockFlag'

// 정산 즉시승인·사장 연락처·임금분쟁은 M6 범위이며 #158에서 실연동하지 않는다.
// 위 USE_MOCK(공용 mockFlag)과 별개로 항상 Mock을 유지한다.
const USE_MOCK_SETTLEMENT_DISPUTE = true

// 지점별 근무 목록. 실제 API 처럼 workplaceId·keyword 로 걸러서 응답한다.
// status 는 7단계 enum(@/constants/workCaseStatus). 요약 6버킷을 골고루 보이도록 구성.
// 목록 UI가 Mock에서도 실 API와 같은 필드 계약(startsAt/endsAt/worker)을 사용하도록 맞춘다.
const mockWorkCaseList = [
  {
    workCaseId: 101,
    workplaceId: 1,
    worker: { workerId: 1001, name: '이알바' },
    title: '주말 홀 서빙',
    workDate: '2026-07-22',
    startsAt: '2026-07-22T01:00:00Z',
    endsAt: '2026-07-22T09:00:00Z',
    dailyWage: 90000,
    status: 'IN_PROGRESS'
  },
  {
    workCaseId: 102,
    workplaceId: 1,
    worker: null,
    title: '평일 주방 보조',
    workDate: '2026-07-23',
    startsAt: '2026-07-23T00:00:00Z',
    endsAt: '2026-07-23T06:00:00Z',
    dailyWage: 72000,
    status: 'DRAFT'
  },
  {
    workCaseId: 103,
    workplaceId: 1,
    worker: { workerId: 1002, name: '박알바' },
    title: '마감 청소',
    workDate: '2026-07-21',
    startsAt: '2026-07-21T11:00:00Z',
    endsAt: '2026-07-21T14:00:00Z',
    dailyWage: 45000,
    status: 'COMPLETED'
  },
  {
    workCaseId: 104,
    workplaceId: 1,
    worker: null,
    title: '금요일 저녁 서빙',
    workDate: '2026-07-25',
    startsAt: '2026-07-25T08:00:00Z',
    endsAt: '2026-07-25T13:00:00Z',
    dailyWage: 65000,
    status: 'DRAFT'
  },
  {
    workCaseId: 105,
    workplaceId: 1,
    worker: { workerId: 1003, name: '한알바' },
    title: '토요일 브런치',
    workDate: '2026-07-26',
    startsAt: '2026-07-26T00:00:00Z',
    endsAt: '2026-07-26T05:00:00Z',
    dailyWage: 65000,
    status: 'READY'
  },
  {
    workCaseId: 106,
    workplaceId: 1,
    worker: { workerId: 1004, name: '한알바2' },
    title: '토요일 브런치',
    workDate: '2026-07-26',
    startsAt: '2026-07-26T00:00:00Z',
    endsAt: '2026-07-26T05:00:00Z',
    dailyWage: 65000,
    status: 'ACCEPTED'
  },
  {
    workCaseId: 107,
    workplaceId: 1,
    worker: { workerId: 1005, name: '한알바3' },
    title: '토요일 브런치',
    workDate: '2026-07-26',
    startsAt: '2026-07-26T00:00:00Z',
    endsAt: '2026-07-26T05:00:00Z',
    dailyWage: 65000,
    status: 'COMPLETED'
  },
  {
    workCaseId: 108,
    workplaceId: 1,
    worker: { workerId: 1006, name: '박알바2' },
    title: '마감 청소',
    workDate: '2026-07-21',
    startsAt: '2026-07-21T11:00:00Z',
    endsAt: '2026-07-21T14:00:00Z',
    dailyWage: 45000,
    status: 'COMPLETED'
  },
  {
    workCaseId: 109,
    workplaceId: 1,
    worker: { workerId: 1007, name: '한알바4' },
    title: '토요일 브런치',
    workDate: '2026-07-26',
    startsAt: '2026-07-26T00:00:00Z',
    endsAt: '2026-07-26T05:00:00Z',
    dailyWage: 65000,
    status: 'IN_PROGRESS'
  },
  {
    workCaseId: 201,
    workplaceId: 2,
    worker: { workerId: 2001, name: '최알바' },
    title: '홍대 오픈 캐셔',
    workDate: '2026-07-24',
    startsAt: '2026-07-23T23:00:00Z',
    endsAt: '2026-07-24T05:00:00Z',
    dailyWage: 78000,
    status: 'IN_PROGRESS'
  },
  {
    workCaseId: 202,
    workplaceId: 2,
    worker: null,
    title: '주말 디저트 보조',
    workDate: '2026-07-25',
    startsAt: '2026-07-25T04:00:00Z',
    endsAt: '2026-07-25T10:00:00Z',
    dailyWage: 78000,
    status: 'DRAFT'
  },
  {
    workCaseId: 203,
    workplaceId: 2,
    worker: { workerId: 2002, name: '김알바' },
    title: '평일 마감 정리',
    workDate: '2026-07-20',
    startsAt: '2026-07-20T09:00:00Z',
    endsAt: '2026-07-20T14:00:00Z',
    dailyWage: 65000,
    status: 'NO_SHOW'
  }
]

/**
 * 지점 + 검색어(제목·알바생 이름) + 상태 + 기간으로 거르는 mock 필터.
 * from/to 는 `YYYY-MM-DD` 양끝 포함 구간(캘린더 뷰가 보고 있는 달). 문자열 비교로 충분하다.
 *
 * 정렬은 서버가 정하므로(WORK-002) 클라이언트 파라미터를 두지 않는다. mock 은 실제 API 의
 * 기본 정렬(근무 날짜 최신순, 같은 날짜는 시작 시간순)을 흉내 낸다.
 */
function filterMockWorkCases(workplaceId, { keyword = '', status = '', from = '', to = '' } = {}) {
  const q = String(keyword).trim().toLowerCase()
  return mockWorkCaseList
    .filter((s) => s.workplaceId === Number(workplaceId))
    .filter((s) => status === '' || s.status === status)
    .filter((s) => from === '' || s.workDate >= from)
    .filter((s) => to === '' || s.workDate <= to)
    .filter(
      (s) =>
        q === '' ||
        s.title.toLowerCase().includes(q) ||
        (s.worker?.name ?? '').toLowerCase().includes(q)
    )
    .sort(
      (a, b) =>
        -(
          String(a.workDate).localeCompare(String(b.workDate)) ||
          String(a.startsAt).localeCompare(String(b.startsAt))
        )
    )
}

/** Mock 내부 필터용 workplaceId는 숨기고 실제 목록 응답 필드만 복사한다. */
function toMockWorkCaseListItem(item) {
  return {
    workCaseId: item.workCaseId,
    title: item.title,
    workDate: item.workDate,
    startsAt: item.startsAt,
    endsAt: item.endsAt,
    dailyWage: item.dailyWage,
    status: item.status,
    worker: item.worker ? { ...item.worker } : null
  }
}

// 지점 이름(mock 표시용). 실제 API 는 상세 응답에 workplaceName 을 담아준다.
const MOCK_WORKPLACE_NAMES = { 1: '강남점', 2: '홍대점' }

const mockWorkCaseDetail = {
  workCaseId: 101,
  workplaceId: 1,
  workplaceName: '강남점',
  title: '주말 홀 서빙',
  workDate: '2026-07-22',
  startsAt: '2026-07-22T01:00:00Z',
  endsAt: '2026-07-22T09:00:00Z',
  breakMinutes: 60,
  breakPaid: false,
  dailyWage: 90000,
  status: 'IN_PROGRESS',
  termsVersion: 1,
  latestInvitation: null,
  contract: null,
  attendance: { checkedInAt: null, checkedOutAt: null },
  escrow: null,
  settlement: null,
  worker: { workerId: 1001, name: '이알바' }
}

/**
 * 근태 현황 요약 → 6버킷 카운트 (WORK-001).
 * { draft, accepted, ready, inProgress, completed, noShow }
 * — @/constants/workCaseStatus WORK_CASE_SUMMARY.
 */
export async function getWorkCaseSummary(workplaceId) {
  if (USE_MOCK) {
    const list = filterMockWorkCases(workplaceId)
    const count = (status) => list.filter((s) => s.status === status).length
    return {
      draft: count('DRAFT'),
      accepted: count('ACCEPTED'),
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
 *
 * 목록 뷰·캘린더 뷰가 **같은 엔드포인트**를 쓴다. 캘린더 뷰일 때만 보고 있는 달을
 * from/to 로 좁혀서 요청하고, 결과를 날짜별로 묶어 그린다(@/utils/calendar).
 * content 항목은 실 API의 WorkCaseListItemResponse 필드 계약을 그대로 따른다.
 * @param {number} workplaceId
 * @param {object} params keyword, status, from, to, page, size
 */
export async function listWorkCases(workplaceId, params = {}) {
  if (USE_MOCK) {
    const content = filterMockWorkCases(workplaceId, {
      keyword: params.keyword,
      status: params.status,
      from: params.from,
      to: params.to
    }).map(toMockWorkCaseListItem)
    return {
      content,
      page: { number: 0, size: content.length || 1, totalElements: content.length, totalPages: 1 }
    }
  }
  const { data } = await http.get(`/workplaces/${workplaceId}/work-cases`, { params })
  return data
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
      worker: listItem.worker ? { ...listItem.worker } : null
    }
  }
  const { data } = await http.get(`/work-cases/${workCaseId}`)
  return data
}

/** 근무 수정 (WORK-005). DRAFT 만 허용, 확정 후 409 WORK_CASE_LOCKED. 성공은 204 로 Body 가 없다. */
export async function updateWorkCase(workCaseId, payload) {
  if (USE_MOCK) return
  await http.patch(`/work-cases/${workCaseId}`, payload)
}

/** 근무 삭제 (WORK-006). DRAFT 만 허용, 확정 후 409 WORK_CASE_LOCKED */
export async function deleteWorkCase(workCaseId) {
  if (USE_MOCK) return
  await http.delete(`/work-cases/${workCaseId}`)
}

/**
 * 근무 연결 링크 발급 → { inviteUrl, expiresAt } (INVITE-001).
 *
 * 활성 초대가 이미 있으면 서버가 그 링크를 그대로 돌려준다(새 발급 201, 재사용 200).
 * 공통 Client 가 Body 만 넘겨주므로 화면은 둘을 구분하지 않는다 — 어느 쪽이든 "지금 유효한
 * 링크"라는 의미가 같다. 링크를 바꾸려면 reissueInvite 를 쓴다.
 */
export async function createInvite(workCaseId) {
  if (USE_MOCK) {
    return {
      inviteUrl: `${location.origin}/invitations/mock-token-${workCaseId}`,
      expiresAt: '2026-07-23T23:59:59Z'
    }
  }
  const { data } = await http.post(`/work-cases/${workCaseId}/invitations`)
  return data
}

/**
 * 연결 링크 재발급 → { inviteUrl, expiresAt } (INVITE-001).
 *
 * 현재 활성 초대를 철회하고 새 Token 으로 교체한다. 이전 링크는 즉시 사용할 수 없게 되므로
 * 링크를 잘못 보냈을 때만 쓴다. 항상 새 초대를 만들어 성공은 언제나 201 이다.
 */
export async function reissueInvite(workCaseId) {
  if (USE_MOCK) {
    return {
      inviteUrl: `${location.origin}/invitations/mock-token-${workCaseId}-reissued`,
      expiresAt: '2026-07-23T23:59:59Z'
    }
  }
  const { data } = await http.post(`/work-cases/${workCaseId}/invitations/reissue`)
  return data
}

/**
 * 정산 즉시 승인 → { settlementId, status, completedAt } (SETTLE-002). HOLD 상태만.
 * Idempotency-Key(UUID) 필수 — 재시도 시 동일 키로 중복 지급 방지.
 */
export async function approveSettlement(workCaseId) {
  if (USE_MOCK_SETTLEMENT_DISPUTE) {
    return { settlementId: 1, status: 'COMPLETED', completedAt: new Date().toISOString() }
  }
  const { data } = await idempotentPost(`/work-cases/${workCaseId}/settlement/approve`)
  return data
}

/**
 * 사장 연락처 조회(문의하기) → { ownerName, phone } (CONTACT-001). 해당 알바생.
 * phone 은 승인 계약대로 구분 문자 없는 숫자다. 표시 형식은 화면에서 만든다.
 */
export async function getOwnerContact(workCaseId) {
  if (USE_MOCK_SETTLEMENT_DISPUTE) return { ownerName: '김사장', phone: '01012345678' }
  const { data } = await http.get(`/work-cases/${workCaseId}/workplace-contact`)
  return data
}

/** 신고 내역 조회 → { content[] } (DISPUTE-002). 당사자만 */
export async function listReports(workCaseId) {
  if (USE_MOCK_SETTLEMENT_DISPUTE) return { content: [] }
  // 페이지 응답 { content, ... } 은 data 래핑이 없어 본문을 그대로 반환.
  return http.get(`/work-cases/${workCaseId}/disputes`)
}

/**
 * 임금분쟁 신고 제출 → { reportId } (DISPUTE-001). 기록·알림용, 정산 영향 없음.
 * @param {object} payload content(경위서)
 */
export async function createReport(workCaseId, { content }) {
  if (USE_MOCK_SETTLEMENT_DISPUTE) return { reportId: Date.now() }
  const { data } = await http.post(`/work-cases/${workCaseId}/disputes`, { content })
  return data
}
