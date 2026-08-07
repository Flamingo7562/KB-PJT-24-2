/**
 * 문서함 API 서비스 — 근로계약서(자동 생성) + 보건증(업로드·공유).
 *
 * 업로드 역할 규칙(서버 검증, 위반 403): 알바생=HEALTH_CERT 만 / 사장=CONTRACT 스캔본만.
 * 삭제: 연결 근무 종료 후만(위반 409) — 사장 문서함은 계약서·보건증 동일. 허용 형식 jpg/png/pdf.
 * 보건증 공유 대상 = 확정·시작 전(ACCEPTED/READY) 근무 보유 지점만. 원본 삭제 시 공유 REVOKED.
 *
 * 관련 API(명세 37~44):
 *   GET/POST /api/documents   PATCH/DELETE /api/documents/{documentId}
 *   GET /api/documents/{documentId}/file
 *   GET/POST /api/documents/{documentId}/shares
 *   DELETE /api/documents/{documentId}/shares/{workplaceId}
 */
import http from '@/services/http'

const USE_MOCK = true

// source: OWN(내 문서) / SHARED(공유받음, 사장 문서함). expiryDate = 보건증 발급일+1년(표시 계산)
// workCaseStatus: 연결된 근무(work_case)의 상태 스냅샷 — 문서 삭제 가능 여부 판단용(mock 전용 필드).
//   실제 API 연동 시 서버가 이미 계산해 내려주는 값으로 교체될 수 있다(교체 지점: isDocumentDeletable).
//
// DB(documents 테이블) ↔ 이 mock 필드 대응 — 서버 연동 시 이름이 달라 매핑이 필요하다:
//   issuedDate  ↔ issued_on (NULL 허용 — 직접 업로드본은 서버가 안 채우면 화면 날짜가 빈다)
//   expiryDate  ↔ expires_on (실제 컬럼 존재. domain.md 의 "컬럼 없음" 서술과 어긋나 확인 필요)
//   docType     ↔ document_type ('CONTRACT'↔'EMPLOYMENT_CONTRACT', 'HEALTH_CERT'↔'HEALTH_CERTIFICATE')
//   workCaseId  ↔ work_case_id (NULL = 근무 미연결 직접 업로드본 → 상시 삭제 가능)
const mockDocuments = [
  {
    documentId: 1,
    docType: 'CONTRACT',
    workplaceId: 1,
    workCaseId: 201,
    fileName: '근로계약서_강남점_0722',
    fileExt: 'pdf',
    issuedDate: '2026-07-22',
    expiryDate: null,
    source: 'OWN',
    sharedByName: null,
    workCaseStatus: 'READY', // 근무 시작 전 — 삭제 잠금
    createdAt: '2026-07-22T09:20:00'
  },
  {
    documentId: 2,
    docType: 'CONTRACT',
    workplaceId: 1,
    workCaseId: 189,
    fileName: '근로계약서_강남점_0610',
    fileExt: 'pdf',
    issuedDate: '2026-06-10',
    expiryDate: null,
    source: 'OWN',
    sharedByName: null,
    workCaseStatus: 'COMPLETED', // 근무 종료 — 삭제 가능
    createdAt: '2026-06-10T09:00:00'
  },
  {
    documentId: 3,
    docType: 'HEALTH_CERT',
    workplaceId: 1,
    workCaseId: 201,
    fileName: '보건증_김알바',
    fileExt: 'jpg',
    issuedDate: '2026-06-01',
    expiryDate: '2027-06-01',
    source: 'SHARED',
    sharedByName: '김알바',
    workCaseStatus: 'READY', // 근무 시작 전 — 삭제 잠금
    createdAt: '2026-06-05T10:00:00'
  },
  {
    documentId: 4,
    docType: 'HEALTH_CERT',
    workplaceId: 1,
    workCaseId: 189,
    fileName: '보건증_박알바',
    fileExt: 'jpg',
    issuedDate: '2026-05-20',
    expiryDate: '2027-05-20',
    source: 'SHARED',
    sharedByName: '박알바',
    workCaseStatus: 'COMPLETED', // 근무 종료 — 삭제 가능
    createdAt: '2026-06-01T10:00:00'
  }
]

const mockShares = [{ workplaceId: 1, workplaceName: '강남점', sharedAt: '2026-07-20T10:00:00' }]

/**
 * 사장 문서함의 문서 삭제 가능 여부 — 계약서·보건증 모두 연결 근무 종료(COMPLETED/NO_SHOW) 후만.
 * 근무에 연결되지 않은 직접 업로드본은 잠글 근거가 없어 상시 삭제 가능.
 * 공유받은 보건증을 지워도 원본은 알바생 문서함에 남는다(내 문서함에서만 제거).
 * 실제 API 연동 시 서버가 최종 검증(409)하므로, 이 함수는 버튼 노출용 UI 힌트로만 쓴다 — 교체 지점.
 */
export function isDocumentDeletable(document) {
  if (!document.workCaseId) return true
  return ['COMPLETED', 'NO_SHOW'].includes(document.workCaseStatus)
}

/**
 * 문서 목록 조회 → { content[] } (명세 37).
 * @param {object} params workplaceId(사장), docType
 */
export async function listDocuments(params = {}) {
  if (USE_MOCK) return { content: mockDocuments.map((d) => ({ ...d })) }
  // 페이지 응답 { content, ... } 은 data 래핑이 없어 본문을 그대로 반환.
  return http.get('/documents', { params })
}

/**
 * 문서 업로드 → { documentId } (명세 38). multipart.
 * @param {FormData} formData docType, file(jpg/png/pdf), issuedDate(보건증 필수), workplaceId(사장)
 */
export async function uploadDocument(formData) {
  if (USE_MOCK) return { documentId: Date.now() }
  const { data } = await http.post('/documents', formData)
  return data
}

/** 문서 수정(보건증 발급일) (명세 39). 소유자 */
export async function updateDocumentIssuedDate(documentId, { issuedDate }) {
  if (USE_MOCK) return { documentId, issuedDate }
  const { data } = await http.patch(`/documents/${documentId}`, { issuedDate })
  return data
}

/** 문서 삭제 (명세 40). 사장 문서함은 계약서·보건증 모두 근무 종료 후만(409) */
export async function deleteDocument(documentId) {
  if (USE_MOCK) return
  await http.delete(`/documents/${documentId}`)
}

/**
 * 문서 파일 보기/다운로드 URL (명세 41). 이미지·PDF inline.
 * mock 은 표시용 placeholder 를 돌려준다(실제는 파일 스트림 URL).
 * @param {number} documentId
 * @param {'view'|'download'} mode
 */
export function documentFileUrl(documentId, mode = 'view') {
  if (USE_MOCK) return ''
  return serverDocumentFileUrl(documentId, mode)
}

/**
 * 계약 수락 직후처럼 목록 Mock과 무관하게 서버의 최종 문서 Stream을 열어야 할 때 사용한다.
 * 저장소 URL이 아니라 권한 검사를 수행하는 동일 출처 API 경로만 만든다.
 */
export function serverDocumentFileUrl(documentId, mode = 'view') {
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${base}/documents/${documentId}/file?mode=${mode}`
}

/** 문서 공유 현황 조회 → [{ workplaceId, workplaceName, sharedAt }] (명세 44) */
export async function getDocumentShares(documentId) {
  if (USE_MOCK) return mockShares.map((s) => ({ ...s }))
  const { data } = await http.get(`/documents/${documentId}/shares`)
  return data
}

/** 보건증 공유 → { shareId } (명세 42). 확정·시작 전(ACCEPTED/READY) 지점만, 중복 409 */
export async function shareDocument(documentId, { workplaceId }) {
  if (USE_MOCK) return { shareId: Date.now() }
  const { data } = await http.post(`/documents/${documentId}/shares`, { workplaceId })
  return data
}

/** 보건증 공유 취소 (명세 43). 소유자 */
export async function revokeShare(documentId, workplaceId) {
  if (USE_MOCK) return
  await http.delete(`/documents/${documentId}/shares/${workplaceId}`)
}
