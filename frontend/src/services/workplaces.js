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
import { normalizePhone } from '@/utils/validators'

const USE_MOCK = true

/**
 * 승인 전화번호 계약(`users.phone`과 독립된 `workplaces.phone`)은 구분 문자 없는 숫자만
 * 저장·반환한다. 화면은 하이픈이 들어간 표시 형식을 유지하므로 전송 직전에 정규화한다.
 * phone 을 보내지 않는 부분 수정 요청은 키를 만들지 않고 그대로 통과시킨다.
 */
function withNormalizedPhone(payload) {
  if (!payload || payload.phone === undefined) return payload
  return { ...payload, phone: normalizePhone(payload.phone) }
}

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
  const body = withNormalizedPhone(payload)
  if (USE_MOCK) {
    const workplaceId = Date.now()
    mockWorkplaces.push({
      workplaceId,
      name: body.name,
      address: body.address,
      radiusMeters: body.radiusM ?? 100
    })
    return { workplaceId }
  }
  const { data } = await http.post('/workplaces', body)
  return data
}

/** 사업장 수정 (명세 13). 본인 소유 검증(서버) */
export async function updateWorkplace(workplaceId, payload) {
  const body = withNormalizedPhone(payload)
  if (USE_MOCK) {
    const target = mockWorkplaces.find((w) => w.workplaceId === workplaceId)
    if (target) Object.assign(target, body)
    return { workplaceId, ...body }
  }
  const { data } = await http.patch(`/workplaces/${workplaceId}`, body)
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

/**
 * 지점 출퇴근 QR 조회 → { qrToken }.
 *
 * 정적 QR — 지점마다 값이 고정이라 만료·재발급 주기가 없다. 출력해 매장에 부착하는 용도.
 * 프론트는 받은 토큰을 그대로 표시하며, 지점을 바꿀 때만 다시 조회한다.
 * 토큰 발급·검증은 서버 책임이고, 대리 출근 차단은 스캔 시 GPS 반경 검증이 담당한다.
 * TODO(백엔드 연동): 엔드포인트 위치·응답 필드 확정 후 USE_MOCK 해제.
 */
export async function getWorkplaceQr(workplaceId) {
  if (USE_MOCK) {
    // 정적 QR 이므로 같은 지점은 항상 같은 토큰이 나와야 한다(시각 기반 값 금지).
    return { qrToken: `mock-qr-${workplaceId}` }
  }
  const { data } = await http.get(`/workplaces/${workplaceId}/qr`)
  return data
}
