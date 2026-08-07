/** 근무 초대 조회·수락 API — 인증된 WORKER의 /invitations/{token} 딥링크 전용. */
import http, { idempotentPost } from '@/services/http'

/** 인증 오류는 초대 화면이 WORKER 로그인 복귀 경로를 보존해 직접 처리한다. */
export async function getInvite(token) {
  const { data } = await http.get(`/invitations/${token}`, { skipAuthRedirect: true })
  return data
}

/**
 * Body 없는 최종 동의 → { workCaseId, escrowStatus: 'HELD' }.
 * 호출자가 한 수락 의도 동안 같은 Key를 보존해야 네트워크 결과가 불명확해도 안전하게 Replay된다.
 * @param {string} token
 * @param {{ idempotencyKey: string }} options
 */
export async function confirmInvite(token, { idempotencyKey }) {
  const { data } = await idempotentPost(`/invitations/${token}/accept`, undefined, {
    idempotencyKey,
    config: { skipAuthRedirect: true }
  })
  return data
}
