/**
 * 문서함 API 서비스 — 근로계약서(자동 생성) + 보건증(업로드·공유).
 *
 * 업로드 역할 규칙(서버 검증, 위반 403): 알바생=HEALTH_CERT 만 / 사장=CONTRACT 스캔본만.
 * 삭제: 보건증 상시 / 계약서는 연결 근무 종료 후(위반 409). 허용 형식 jpg/png/pdf.
 * 보건증 공유 대상 = MATCHED 근무 보유 지점만. 원본 삭제 시 공유 CASCADE 해제.
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
// shiftStatus: 연결된 근무(work_case)의 상태 스냅샷 — 계약서 삭제 가능 여부 판단용(mock 전용 필드).
//   실제 API 연동 시 서버가 이미 계산해 내려주는 값으로 교체될 수 있다(교체 지점: isContractDeletable).
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
    shiftStatus: 'MATCHED', // 근무 시작 전 — 삭제 잠금
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
    shiftStatus: 'COMPLETED', // 근무 종료 — 삭제 가능
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
    shiftStatus: null,
    createdAt: '2026-06-05T10:00:00'
  }
]

const mockShares = [{ workplaceId: 1, workplaceName: '강남점', sharedAt: '2026-07-20T10:00:00' }]

/**
 * 계약서 삭제 가능 여부(연결 근무 종료 후만, 도메인 규칙).
 * 보건증은 사장 쪽에서 항상 삭제 불가(원본 삭제는 알바생 권한).
 * 실제 API 연동 시 서버가 최종 검증(409)하므로, 이 함수는 버튼 노출용 UI 힌트로만 쓴다 — 교체 지점.
 */
export function isContractDeletable(document) {
  return document.docType === 'CONTRACT' && ['COMPLETED', 'NO_SHOW'].includes(document.shiftStatus)
}

/**
 * 문서 목록 조회 → { content[] } (명세 37).
 * @param {object} params workplaceId(사장), docType
 */
export async function listDocuments(params = {}) {
  if (USE_MOCK) return { content: mockDocuments.map((d) => ({ ...d })) }
  const { data } = await http.get('/documents', { params })
  return data
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

/** 문서 삭제 (명세 40). 계약서는 근무 종료 후만(409) */
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
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${base}/documents/${documentId}/file?mode=${mode}`
}

/** 문서 공유 현황 조회 → [{ workplaceId, workplaceName, sharedAt }] (명세 44) */
export async function getDocumentShares(documentId) {
  if (USE_MOCK) return mockShares.map((s) => ({ ...s }))
  const { data } = await http.get(`/documents/${documentId}/shares`)
  return data
}

/** 보건증 공유 → { shareId } (명세 42). MATCHED 지점만, 중복 409 */
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
