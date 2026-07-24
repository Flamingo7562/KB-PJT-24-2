/**
 * 근무 확정(초대 링크) API 서비스 — 알바생 딥링크 /invitations/{token}.
 *
 * 확정(전자서명 날인) = 에스크로 예치. 확정 후 근무 변경·취소 불가.
 * 만료·사용 토큰 410, 사장 잔액 부족 409.
 *
 * 관련 API(명세 INVITE-002/003):
 *   GET  /api/invitations/{token}   POST /api/invitations/{token}/accept
 */
import http, { idempotentPost } from '@/services/http'

const USE_MOCK = true

// 자동 입력될 근무 내역 + 사장 안심 뱃지
const mockInvite = {
  title: '주말 홀 서빙',
  workDate: '2026-07-27',
  startTime: '10:00',
  endTime: '18:00',
  breakMinutes: 60,
  breakPaid: false,
  dailyWage: 90000,
  workplaceName: '강남점',
  ownerBadge: { badgeType: 'TRUST_OWNER', level: 2 }
}

/** 초대 근무 내역 조회 (INVITE-002). 만료·사용 토큰 410 */
export async function getInvite(token) {
  if (USE_MOCK) return { ...mockInvite, token }
  const { data } = await http.get(`/invitations/${token}`)
  return data
}

/**
 * 근무 확정(서명 제출) → { workCaseId, escrowStatus } (INVITE-003).
 * 매칭+예치+계약서 생성 단일 트랜잭션. 잔액 부족 409.
 * Idempotency-Key(UUID) 필수 — 재시도 시 동일 키로 중복 계약·중복 예치 방지.
 * @param {string} token
 * @param {object} payload signatureImage(base64 또는 multipart)
 */
export async function confirmInvite(token, { signatureImage }) {
  if (USE_MOCK) return { workCaseId: 999, escrowStatus: 'HOLD' }
  const { data } = await idempotentPost(`/invitations/${token}/accept`, { signatureImage })
  return data
}
