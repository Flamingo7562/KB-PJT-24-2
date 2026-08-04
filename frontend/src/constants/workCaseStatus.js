/**
 * 근무(work_case) 상태 단일 소스 — 8단계 enum ↔ 화면 표시어·뱃지색 매핑.
 *
 * 도메인 규칙(docs/rules/domain.md · frontend.md): 상태 문자열을 컴포넌트에 하드코딩하지 않고
 * 반드시 이 파일의 매핑·헬퍼만 사용한다(상태 표기의 단일 원본).
 *
 * 상태 전이(v1.0 확정):
 *   DRAFT(수락 전) → ACCEPTED(수락·계약) → READY(시작 대기)
 *   → IN_PROGRESS(근무중) → COMPLETED(완료)
 *   확정 계열에서 NO_SHOW(미출근) · DRAFT에서 CANCELED(취소).
 * 초대 발급·대기 상태는 work_case가 아니라 work_invitations가 담당한다.
 *
 * CHECK_OUT_MISSING(퇴근 확인 필요)은 성공 출근 뒤 성공 퇴근이 없는 근무이며 NO_SHOW와
 * 상호 배타적이다(REQUIREMENTS ATT-006 · WORK-007). 판정 시점·해소·정산 흐름은
 * DEC-OPEN-CHECK-OUT-MISSING-FLOW 미결이라 이 파일은 표기만 담당한다.
 *
 * - `label`: 화면 표기 한글 문구.
 * - `color`: base.css 색 변수 문자열(그대로 style 바인딩). 아이콘 매핑은 StatusChip.vue.
 */
export const WORK_CASE_STATUS = {
  DRAFT: { label: '수락 전', color: 'var(--color-text-sub)' },
  ACCEPTED: { label: '계약완료', color: 'var(--color-owner)' },
  READY: { label: '근무예정', color: 'var(--color-owner)' },
  IN_PROGRESS: { label: '근무중', color: 'var(--color-primary)' },
  CHECK_OUT_MISSING: { label: '퇴근 확인 필요', color: 'var(--color-warning)' },
  COMPLETED: { label: '완료', color: 'var(--color-success)' },
  NO_SHOW: { label: '노쇼', color: 'var(--color-danger)' },
  CANCELED: { label: '취소', color: 'var(--color-text-sub)' }
}

/** 상태 → 표기 라벨(없으면 원문 반환). */
export function workCaseStatusLabel(status) {
  return WORK_CASE_STATUS[status]?.label ?? status
}

/** 상태 → 뱃지 색 변수(없으면 보조 텍스트색). */
export function workCaseStatusColor(status) {
  return WORK_CASE_STATUS[status]?.color ?? 'var(--color-text-sub)'
}

/**
 * 수정·삭제 가능 여부에 사용하는 DRAFT 판별이다.
 * 연결 링크 발급은 별도 canIssueInvitation capability를 사용한다.
 */
export function isDraft(status) {
  return status === 'DRAFT'
}

/**
 * 근태관리 요약 카운트(6종). `key` = 서버 요약 응답 필드, `status` = 매핑 enum.
 * CANCELED는 운영 현황 요약에 집계하지 않는다.
 */
export const WORK_CASE_SUMMARY = [
  { key: 'draft', status: 'DRAFT' },
  { key: 'accepted', status: 'ACCEPTED' },
  { key: 'ready', status: 'READY' },
  { key: 'inProgress', status: 'IN_PROGRESS' },
  { key: 'completed', status: 'COMPLETED' },
  { key: 'noShow', status: 'NO_SHOW' }
]

/** 요약 카운트 초기값(모든 버킷 0). */
export function emptyWorkCaseSummary() {
  return Object.fromEntries(WORK_CASE_SUMMARY.map((b) => [b.key, 0]))
}

/**
 * 근태관리 검색·필터 시트의 '유형' 선택지 — 전체 + 8단계 전부.
 *
 * WORK_CASE_STATUS 에서 파생시킨다(라벨을 다시 적으면 단일 원본이 깨진다). 나열 순서는
 * 그 객체의 키 순서 = 상태 전이 순서다.
 * 요약 카드(WORK_CASE_SUMMARY)와 달리 CANCELED 도 넣는다 — 요약은 운영 현황 집계라
 * 취소를 세지 않지만, 필터는 취소된 근무를 찾아보는 용도가 있다.
 */
export const WORK_CASE_STATUS_FILTER = [
  { value: 'ALL', label: '전체' },
  ...Object.entries(WORK_CASE_STATUS).map(([status, { label }]) => ({ value: status, label }))
]
