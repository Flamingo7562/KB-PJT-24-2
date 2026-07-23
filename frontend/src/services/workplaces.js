/**
 * 사업장(지점) API 서비스 — 사장 전용.
 *
 * 사업장은 사장 전 기능의 전제 컨텍스트(라우팅 G7). 네비 지점 select 의 원본.
 *
 * 관련 API(명세 11~14):
 *   GET /api/workplaces   POST /api/workplaces
 *   PATCH /api/workplaces/{workplaceId}   DELETE /api/workplaces/{workplaceId}
 */
import http from '@/services/http'

const USE_MOCK = true

const mockWorkplaces = [
  { workplaceId: 1, name: '강남점', address: '서울 강남구 테헤란로 1' },
  { workplaceId: 2, name: '홍대점', address: '서울 마포구 양화로 100' }
]

/** 사업장 목록 조회 → [{ workplaceId, name, address }] (명세 12) */
export async function listWorkplaces() {
  if (USE_MOCK) return mockWorkplaces.map((w) => ({ ...w }))
  const { data } = await http.get('/workplaces')
  return data
}

/**
 * 사업장 등록 → { workplaceId } (명세 11). 복수 등록 가능.
 * @param {object} payload businessNumber, name, representativeName, address, phone, latitude?, longitude?, radiusM?
 */
export async function createWorkplace(payload) {
  if (USE_MOCK) return { workplaceId: Date.now() }
  const { data } = await http.post('/workplaces', payload)
  return data
}

/** 사업장 수정 (명세 13). 본인 소유 검증(서버) */
export async function updateWorkplace(workplaceId, payload) {
  if (USE_MOCK) return { workplaceId, ...payload }
  const { data } = await http.patch(`/workplaces/${workplaceId}`, payload)
  return data
}

/** 사업장 삭제(soft delete) (명세 14). 진행 중 근무 존재 시 409 */
export async function deleteWorkplace(workplaceId) {
  if (USE_MOCK) return
  await http.delete(`/workplaces/${workplaceId}`)
}

/**
 * 지점 출퇴근 QR 발급 → { qrToken, expiresAt }.
 *
 * 토큰 생성·유효기간 판정은 서버가 한다(1회성·만료 시 410 — docs/rules/api.md).
 * 프론트는 받은 토큰을 표시하고 만료 전에 다시 발급받기만 한다.
 * TODO(백엔드 연동): 엔드포인트 위치·응답 필드 확정 후 USE_MOCK 해제.
 */
export async function getWorkplaceQr(workplaceId) {
  if (USE_MOCK) {
    return {
      qrToken: `mock-qr-${workplaceId}-${Date.now().toString(36)}`,
      expiresAt: new Date(Date.now() + 30_000).toISOString()
    }
  }
  const { data } = await http.get(`/workplaces/${workplaceId}/qr`)
  return data
}
