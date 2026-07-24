/**
 * 근무(work_case) 상태 단일 소스 — 8단계 enum ↔ 화면 표시어·뱃지색 매핑.
 *
 * 도메인 규칙(docs/rules/domain.md · frontend.md): 상태 문자열을 컴포넌트에 하드코딩하지 않고
 * 반드시 이 파일의 매핑·헬퍼만 사용한다(상태 표기의 단일 원본).
 *
 * 상태 전이(v1.0 확정):
 *   DRAFT(작성) → INVITED(초대 발송) → ACCEPTED(수락·계약) → READY(시작 대기)
 *   → IN_PROGRESS(근무중) → COMPLETED(완료)
 *   확정 계열에서 NO_SHOW(미출근) · 확정 전 DRAFT/INVITED 에서 CANCELED(취소).
 *
 * - `label`: 화면 표기 한글 문구.
 * - `color`: base.css 색 변수 문자열(그대로 style 바인딩). 아이콘 매핑은 StatusChip.vue.
 */
export const WORK_CASE_STATUS = {
  DRAFT: { label: '작성중', color: 'var(--color-text-sub)' },
  INVITED: { label: '초대중', color: 'var(--color-warning)' },
  ACCEPTED: { label: '계약완료', color: 'var(--color-owner)' },
  READY: { label: '근무예정', color: 'var(--color-owner)' },
  IN_PROGRESS: { label: '근무중', color: 'var(--color-primary)' },
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
 * 수정·삭제·연결 링크 복사는 DRAFT 에서만 노출한다.
 * 초대·확정 이후에는 서버가 409 `WORK_CASE_LOCKED` 로 막으므로 화면도 버튼을 숨긴다.
 */
export function isDraft(status) {
  return status === 'DRAFT'
}

/**
 * 근태관리 요약 카운트(6종). `key` = 서버 요약 응답 필드, `status` = 매핑 enum.
 * ACCEPTED·CANCELED 는 요약에 집계하지 않는다.
 */
export const WORK_CASE_SUMMARY = [
  { key: 'draft', status: 'DRAFT' },
  { key: 'invited', status: 'INVITED' },
  { key: 'ready', status: 'READY' },
  { key: 'inProgress', status: 'IN_PROGRESS' },
  { key: 'completed', status: 'COMPLETED' },
  { key: 'noShow', status: 'NO_SHOW' }
]

/** 요약 카운트 초기값(모든 버킷 0). */
export function emptyWorkCaseSummary() {
  return Object.fromEntries(WORK_CASE_SUMMARY.map((b) => [b.key, 0]))
}
