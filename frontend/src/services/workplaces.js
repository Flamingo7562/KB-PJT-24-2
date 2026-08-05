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
import { USE_MOCK } from '@/services/mockFlag'
import { normalizePhone } from '@/utils/validators'

/** 승인 Page Query 경계 — size 는 1~100 이다(API_SPEC.md '페이지네이션'). */
const MAX_PAGE_SIZE = 100

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
  {
    workplaceId: 1,
    businessRegistrationNumber: '1234567890',
    name: '강남점',
    representativeName: '김사장',
    roadAddress: '서울 강남구 테헤란로 1',
    detailAddress: '2층',
    phone: '0212345678',
    radiusMeters: 100,
    status: 'ACTIVE'
  },
  {
    workplaceId: 2,
    businessRegistrationNumber: '9876543210',
    name: '홍대점',
    representativeName: '김사장',
    roadAddress: '서울 마포구 양화로 100',
    detailAddress: '',
    phone: '023334444',
    radiusMeters: 100,
    status: 'ACTIVE'
  }
]

/**
 * 사업장 목록 조회 (명세 12). 공통 Page Envelope `{ content, page }` 를 그대로 반환한다.
 * 목록에는 ACTIVE 와 INACTIVE 가 함께 오고 DELETED 는 오지 않는다. 작업 Context 로
 * 선택할 수 있는 것은 ACTIVE 뿐이므로 필터는 소비하는 Store 가 책임진다.
 * 네비 지점 select 는 전체 목록이 필요한데 페이지네이션 UI 는 M2 범위 밖이라
 * 승인 최대값으로 한 번에 읽는다.
 */
export async function listWorkplaces({ page = 0, size = MAX_PAGE_SIZE } = {}) {
  const boundedSize = Math.min(size, MAX_PAGE_SIZE)
  if (USE_MOCK) {
    return {
      content: mockWorkplaces.map((w) => ({ ...w })),
      page: {
        number: 0,
        size: boundedSize,
        totalElements: mockWorkplaces.length,
        totalPages: 1
      }
    }
  }
  const { data } = await http.get('/workplaces', { params: { page, size: boundedSize } })
  return data
}

/**
 * 사업장 등록 → { workplaceId } (명세 11).
 * 승인 Body 는 businessRegistrationNumber, name, representativeName, roadAddress,
 * detailAddress?, phone, latitude?, longitude? 뿐이다. radius 는 받지 않으며 서버가
 * 100m 를 적용한다. 좌표는 지오코딩 미연동이라 보내지 않는다(둘 다 생략은 유효하다).
 */
export async function createWorkplace({
  businessRegistrationNumber,
  name,
  representativeName,
  roadAddress,
  detailAddress,
  phone
}) {
  const body = {
    businessRegistrationNumber,
    name,
    representativeName,
    roadAddress,
    phone: normalizePhone(phone)
  }
  // 선택 필드다. 비어 있으면 키 자체를 만들지 않는다.
  const trimmedDetail = detailAddress?.trim()
  if (trimmedDetail) body.detailAddress = trimmedDetail

  if (USE_MOCK) {
    const workplaceId = Date.now()
    mockWorkplaces.push({
      workplaceId,
      radiusMeters: 100,
      status: 'ACTIVE',
      detailAddress: '',
      ...body
    })
    return { workplaceId }
  }
  const { data } = await http.post('/workplaces', body)
  return data
}

/**
 * 사업장 수정 (명세 13). 허용 필드는 name, roadAddress, detailAddress, phone 이다.
 * TODO(#145 후속): 수정·Soft Delete 는 #145 제외 범위라 실 Endpoint 가 아직 없다.
 */
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

/**
 * 사업장 삭제(soft delete) (명세 14). 진행 중 근무 존재 시 409
 * TODO(#145 후속): 수정·Soft Delete 는 #145 제외 범위라 실 Endpoint 가 아직 없다.
 */
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
 * TODO(#162): 엔드포인트 위치·응답 필드 확정 후 USE_MOCK 해제.
 */
export async function getWorkplaceQr(workplaceId) {
  if (USE_MOCK) {
    // 정적 QR 이므로 같은 지점은 항상 같은 토큰이 나와야 한다(시각 기반 값 금지).
    return { qrToken: `mock-qr-${workplaceId}` }
  }
  const { data } = await http.get(`/workplaces/${workplaceId}/qr`)
  return data
}
