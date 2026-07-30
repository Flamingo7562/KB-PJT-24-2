/**
 * 문서함 API 서비스 — 근로계약서(자동 생성) + 보건증(업로드·공유).
 *
 * 업로드(서버 검증, 위반 403): 알바생 HEALTH_CERT 만. 계약서는 확정 시 자동 생성되며 업로드 경로가 없다.
 * 삭제(위반 403): 알바생 본인 보건증만. 계약서는 근무일로부터 3년 보존 대상이라 삭제할 수 없고(근로기준법
 *   제42조·시행령 제22조 — 기산일은 근로관계가 끝난 날), 사장 문서함에는 삭제 기능이 없다.
 *   허용 형식 jpg/png/pdf.
 * 보건증 공유 대상 = 확정·시작 전(ACCEPTED/READY) 근무 보유 지점만. 원본 삭제 시 공유 REVOKED.
 * 공유는 근무 종료 시 서버가 자동 해제한다(document_shares.expires_at = work_cases.ends_at).
 *
 * 관련 API(명세 37~44):
 *   GET/POST /api/documents   PATCH/DELETE /api/documents/{documentId}
 *   GET /api/documents/{documentId}/file
 *   GET/POST /api/documents/{documentId}/shares
 *   DELETE /api/documents/{documentId}/shares/{workplaceId}
 */
import http from '@/services/http'

const USE_MOCK = true

// source: OWN(내 문서) / SHARED(공유받음, 사장 문서함).
//
// DB(documents 테이블) ↔ 이 mock 필드 대응 — 서버 연동 시 이름이 달라 매핑이 필요하다:
//   issuedDate  ↔ issued_on
//   expiryDate  ↔ expires_on (보건증 발급일+1년. 서버가 계산해 저장하며 발급일 수정 시 함께 갱신)
//   docType     ↔ document_type ('CONTRACT'↔'EMPLOYMENT_CONTRACT', 'HEALTH_CERT'↔'HEALTH_CERTIFICATE')
//   workCaseId  ↔ work_case_id (계약서는 항상 근무에 연결된다 — 자동 생성본만 존재)
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
    createdAt: '2026-06-01T10:00:00'
  }
]

const mockShares = [{ workplaceId: 1, workplaceName: '강남점', sharedAt: '2026-07-20T10:00:00' }]

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
 * 보건증 업로드 → { documentId } (명세 38). multipart. 알바생 전용(계약서 업로드 경로 없음).
 * @param {FormData} formData docType, file(jpg/png/pdf), issuedDate(필수)
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

/** 보건증 삭제 (명세 40). 본인 소유 보건증만 — 계약서는 근무일로부터 3년 보존 대상이라 삭제 불가(403) */
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
