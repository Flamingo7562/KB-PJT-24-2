/**
 * 회원(내 정보·뱃지) API 서비스.
 *
 * 승인 계약: docs/specs/API_SPEC.md '내 프로필' 절, REQUIREMENTS AUTH-008, DEC-PROFILE-IMMUTABLE
 *   GET /api/users/me   PATCH /api/users/me   PATCH /api/users/me/password
 *   POST /api/users/me/withdrawal   GET /api/users/me/badge
 */
import http from '@/services/http'
import { USE_MOCK } from '@/services/mockFlag'
import { normalizePhone } from '@/utils/validators'

// 승인 응답 필드만 담는다. 전화번호는 구분 문자 없는 정규화 형식으로 주고받고 화면에서 포맷한다.
const mockMe = {
  loginId: 'owner01',
  email: 'owner@test.com',
  name: '김사장',
  phone: '01012345678',
  role: 'OWNER',
  status: 'ACTIVE'
}

// 사장=TRUST_OWNER(안심거래) / 알바생=TRUST_WORKER(성실근로).
// level 0 = 이력 5건 미만(미부여). remainingToNextLevel = 다음 등급까지 남은 건수.
const mockBadge = {
  badgeType: 'TRUST_OWNER',
  level: 2,
  recentCount: 12,
  remainingToNextLevel: 3,
  criterionLabel: '안심거래',
  criterionDesc: '*안심거래란? 임금분쟁 신고 없이 정상 정산 완료'
}

/** 내 정보 조회 (AUTH-008) */
export async function getMe() {
  if (USE_MOCK) return { ...mockMe }
  const { data } = await http.get('/users/me')
  return data
}

/**
 * 내 정보 수정 (AUTH-008). PATCH Body 는 `phone` 만 허용한다.
 * `loginId`·`email`·`name`·`role`·`status` 를 함께 보내면 서버가 무시하지 않고
 * 400 VALIDATION_ERROR 로 거부하므로 절대 싣지 않는다.
 */
export async function updateMe({ phone }) {
  const normalized = normalizePhone(phone)
  if (USE_MOCK) return { ...mockMe, phone: normalized }
  const { data } = await http.patch('/users/me', { phone: normalized })
  return data
}

/** 비밀번호 변경 (명세 8). 현재 비밀번호 불일치 시 400 */
export async function changePassword({ currentPassword, newPassword }) {
  if (USE_MOCK) return
  // TODO(#187): AUTH-009 구현 전까지 실 경로는 404 다.
  await http.patch('/users/me/password', { currentPassword, newPassword })
}

/** 회원 탈퇴 (USER-004). 잔액·예치금·진행 근무 존재 시 409 */
export async function deleteMe({ password }) {
  if (USE_MOCK) return
  // TODO(#188): USER-004 구현 전까지 실 경로는 404 다.
  await http.post('/users/me/withdrawal', { password })
}

/** 내 뱃지 조회 (명세 10) */
export async function getBadge() {
  if (USE_MOCK) return { ...mockBadge }
  // TODO(#182): 명세 10 구현 전까지 실 경로는 404 다.
  const { data } = await http.get('/users/me/badge')
  return data
}
