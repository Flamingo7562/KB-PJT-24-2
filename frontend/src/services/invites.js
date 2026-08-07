/**
 * 근무 확정(초대 링크) API 서비스 — 알바생 딥링크 /invitations/{token}.
 *
 * Body 없는 최종 동의 = 매칭·계약 생성·에스크로 예치. 확정 후 근무 변경·취소 불가.
 * 초대 Token과 멱등 Key는 응답·저장소·로그에 남기지 않는다.
 *
 * 관련 API(명세 INVITE-002/003):
 *   GET  /api/invitations/{token}   POST /api/invitations/{token}/accept
 */
import http, { idempotentPost } from '@/services/http'

/** 인증 WORKER의 초대 근무 조건 조회 (INVITE-002). */
export async function getInvite(token) {
  const { data } = await http.get(`/invitations/${token}`)
  return data
}

/**
 * Body 없는 근무 수락 → { workCaseId, escrowStatus: 'HELD' } (INVITE-003).
 *
 * 호출자가 한 번의 사용자 의도에 같은 Key를 보존한다. 내부 자동 재시도뿐 아니라 결과가
 * 불확실한 뒤 사용자가 다시 확인할 때도 같은 Key를 넘겨 중복 계약·예치를 막는다.
 * @param {string} token
 * @param {{idempotencyKey:string}} options
 */
export async function confirmInvite(token, { idempotencyKey }) {
  const { data } = await idempotentPost(`/invitations/${token}/accept`, undefined, {
    idempotencyKey,
    // 결과 불확실 상태는 화면이 기억하고 사용자의 명시적 Replay로만 같은 Key를 다시 보낸다.
    retries: 0
  })
  return data
}
