/**
 * 로그인 성공 후 라우팅 분기.
 *
 * 지금은 백엔드가 없어 규칙(요구사항)으로 고정 분기하지만, 함수를 분리해두어
 * 백엔드 연동 시 이 함수 내부만 API 응답(loginResponse) 기반으로 교체하면 된다.
 * 호출부(로그인 화면)는 이 함수의 반환값(경로)만 사용한다.
 */

/**
 * 사장 로그인 후 이동 경로.
 * 아직 사업장 등록 여부를 판별할 수 없어 무조건 사업장 등록 화면으로 보낸다.
 * TODO(백엔드 연동): loginResponse.needsWorkplaceSetup 기준으로 '/owner/home' 분기 추가.
 */
export function resolveOwnerLoginRedirect(/* loginResponse */) {
  return '/owner/workplaces/new'
}

/** 알바생 로그인 후 이동 경로. 초대 딥링크 복귀(redirect 쿼리)가 있으면 그곳으로. */
export function resolveWorkerLoginRedirect(redirectQuery /* , loginResponse */) {
  return redirectQuery || '/worker/home'
}
