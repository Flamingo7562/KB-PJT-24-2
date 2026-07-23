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
  { workplaceId: 1, name: '강남점', address: '서울 강남구 테헤란로 1', radiusMeters: 100 },
  { workplaceId: 2, name: '홍대점', address: '서울 마포구 양화로 100', radiusMeters: 150 }
]

/** 사업장 목록 조회 → [{ workplaceId, name, address, radiusMeters }] (명세 12) */
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
  if (USE_MOCK) {
    const workplaceId = Date.now()
    mockWorkplaces.push({
      workplaceId,
      name: payload.name,
      address: payload.address,
      radiusMeters: payload.radiusM ?? 100
    })
    return { workplaceId }
  }
  const { data } = await http.post('/workplaces', payload)
  return data
}

/** 사업장 수정 (명세 13). 본인 소유 검증(서버) */
export async function updateWorkplace(workplaceId, payload) {
  if (USE_MOCK) {
    const target = mockWorkplaces.find((w) => w.workplaceId === workplaceId)
    if (target) Object.assign(target, payload)
    return { workplaceId, ...payload }
  }
  const { data } = await http.patch(`/workplaces/${workplaceId}`, payload)
  return data
}

/** 사업장 삭제(soft delete) (명세 14). 진행 중 근무 존재 시 409 */
export async function deleteWorkplace(workplaceId) {
  if (USE_MOCK) {
    const idx = mockWorkplaces.findIndex((w) => w.workplaceId === workplaceId)
    if (idx >= 0) mockWorkplaces.splice(idx, 1)
    return
  }
  await http.delete(`/workplaces/${workplaceId}`)
}
