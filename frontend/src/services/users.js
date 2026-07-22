/**
 * 회원(내 정보·뱃지) API 서비스.
 *
 * 관련 API(명세 6~10):
 *   GET /api/users/me   PATCH /api/users/me   PATCH /api/users/me/password
 *   DELETE /api/users/me   GET /api/users/me/badge
 */
import http from '@/services/http'

const USE_MOCK = true

const mockMe = {
  loginId: 'owner01',
  name: '김사장',
  email: 'owner@test.com',
  phone: '010-1234-5678',
  role: 'OWNER'
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

/** 내 정보 조회 (명세 6) */
export async function getMe() {
  if (USE_MOCK) return { ...mockMe }
  const { data } = await http.get('/users/me')
  return data
}

/** 내 정보 수정(이름·전화번호) (명세 7) */
export async function updateMe({ name, phone }) {
  if (USE_MOCK) return { ...mockMe, name, phone }
  const { data } = await http.patch('/users/me', { name, phone })
  return data
}

/** 비밀번호 변경 (명세 8). 현재 비밀번호 불일치 시 400 */
export async function changePassword({ currentPassword, newPassword }) {
  if (USE_MOCK) return
  await http.patch('/users/me/password', { currentPassword, newPassword })
}

/** 회원 탈퇴 (명세 9). 잔액·예치금·진행 근무 존재 시 409 */
export async function deleteMe({ password }) {
  if (USE_MOCK) return
  await http.delete('/users/me', { data: { password } })
}

/** 내 뱃지 조회 (명세 10) */
export async function getBadge() {
  if (USE_MOCK) return { ...mockBadge }
  const { data } = await http.get('/users/me/badge')
  return data
}
