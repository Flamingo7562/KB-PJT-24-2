/**
 * 로그인 성공 후 라우팅 분기.
 *
 * 호출부(로그인 화면)는 이 함수의 반환값(경로)만 사용한다.
 */

/**
 * 사장 로그인 후 이동 경로.
 * `needsWorkplaceSetup` 은 서버가 요청 시점 DB 로 계산한 값이다(API_SPEC.md:233).
 * true 면 복귀 경로보다 사업장 등록이 우선한다 — 어차피 G7 가드가 되돌리기 때문이다.
 */
export function resolveOwnerLoginRedirect(loginResponse, redirectQuery) {
  if (loginResponse?.needsWorkplaceSetup === true) return '/owner/workplaces/new'
  return redirectQuery || '/owner/home'
}

/**
 * 알바생 로그인 후 이동 경로. 초대 딥링크 복귀(redirect 쿼리)가 있으면 그곳으로.
 * WORKER 는 사업장 등록으로 강제 이동하지 않는다.
 */
export function resolveWorkerLoginRedirect(redirectQuery /* , loginResponse */) {
  const candidate = Array.isArray(redirectQuery) ? redirectQuery[0] : redirectQuery
  if (typeof candidate !== 'string') return '/worker/home'

  // 초대 복귀는 승인된 내부 경로 하나만 허용한다. //host, scheme, Query·Fragment는 버린다.
  if (!/^\/invitations\/[A-Za-z0-9_-]+$/.test(candidate)) return '/worker/home'
  return candidate
}
