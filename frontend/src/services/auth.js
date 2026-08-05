/**
 * 인증/회원 API 서비스.
 *
 * 실제 Session·CSRF 흐름에 연결되어 있다. Mock 은 개발 환경에서 VITE_USE_MOCK=true 로만 켠다.
 * 컴포넌트·스토어는 함수 시그니처만 의존한다.
 *
 * 인증은 Session(JSESSIONID) 전용 — accessToken 없음(저장·전송 금지).
 * 승인 계약: docs/specs/API_SPEC.md 'Session, CSRF와 로컬 CORS' · '인증·회원' 절
 *   GET  /api/auth/csrf   GET /api/auth/session
 *   GET  /api/auth/login-id-availability   GET /api/auth/email-availability
 *   POST /api/auth/signup   POST /api/auth/login   POST /api/auth/logout
 *
 * 정규화는 이 계층에서 한 번만 적용한다. 가용성 조회와 가입·로그인이 서로 다른 값을 보내면
 * 사전 확인을 통과한 아이디가 최종 요청에서 중복으로 거부될 수 있다.
 */
import http from '@/services/http'
import { USE_MOCK } from '@/services/mockFlag'
import { normalizeEmail, normalizeLoginId, normalizeName, normalizePhone } from '@/utils/validators'

/**
 * CSRF 준비 (GET /api/auth/csrf → 204 No Content + XSRF-TOKEN 쿠키).
 * 이후 모든 상태변경 요청은 http 인터셉터가 X-XSRF-TOKEN 을 첨부한다.
 * 승인 계약상 호출 시점은 앱 최초 실행, 로그인 성공 직후, 로그아웃 성공 직후 세 곳이다.
 * 로그인·로그아웃도 CSRF 검증 대상이며, 검증 실패(403 FORBIDDEN) 후 요청을 자동 재실행하지 않는다.
 */
export async function fetchCsrf() {
  if (USE_MOCK) return
  await http.get('/auth/csrf')
}

/**
 * 세션 복원 (GET /api/auth/session). 앱 시작·새로고침 시 인증·역할·사업장 설정 상태를 복구한다.
 * 로컬 저장 토큰·플래그로 인증을 판단하지 않는다(JSESSIONID 가 단일 기준).
 * 승인 계약상 공개 부트스트랩 API 라서 비인증 상태도 200 `{ authenticated: false }` 로 응답한다.
 * @returns {Promise<{authenticated:boolean, role?:string, name?:string, needsWorkplaceSetup?:boolean}>}
 */
export async function getSession() {
  if (USE_MOCK) return { authenticated: false }
  // 부트스트랩 probe 는 전역 401 리다이렉트(G5)에서 제외한다. 계약상 401 이 나올 수 없지만
  // 설정 오류로 401 이 오면 온보딩으로 튕기며 무한 이동이 될 수 있어 방어적으로 유지한다.
  const { data } = await http.get('/auth/session', { skipAuthRedirect: true })
  return data
}

/** 아이디 중복확인 → { available } (AUTH-003). 가입과 동일한 정규화 값으로 조회한다. */
export async function checkLoginId(loginId) {
  const normalized = normalizeLoginId(loginId)
  if (USE_MOCK) return { available: normalized !== 'taken' }
  const { data } = await http.get('/auth/login-id-availability', {
    params: { loginId: normalized }
  })
  return data
}

/** 이메일 중복확인 → { available } (AUTH-003). 가입과 동일한 정규화 값으로 조회한다. */
export async function checkEmail(email) {
  const normalized = normalizeEmail(email)
  if (USE_MOCK) return { available: normalized !== 'taken@test.com' }
  const { data } = await http.get('/auth/email-availability', { params: { email: normalized } })
  return data
}

/**
 * 회원가입 → { userId } (201).
 * 승인 Body 필드만 보낸다: loginId, password, passwordConfirm, name, email, phone?, role.
 * 비밀번호에는 정규화를 적용하지 않는다(trim·대소문자 변환 금지).
 * @param {object} payload loginId, password, passwordConfirm, name, email, phone?, role('OWNER'|'WORKER')
 */
export async function signup({ loginId, password, passwordConfirm, name, email, phone, role }) {
  const normalizedPhone = normalizePhone(phone)
  const body = {
    loginId: normalizeLoginId(loginId),
    password,
    passwordConfirm,
    name: normalizeName(name),
    email: normalizeEmail(email),
    role
  }
  // phone 은 선택 필드다. 비어 있으면 키 자체를 보내지 않는다.
  if (normalizedPhone) body.phone = normalizedPhone

  if (USE_MOCK) return { userId: 1 }
  const { data } = await http.post('/auth/signup', body)
  return data
}

/**
 * 로그인 → { role, name, needsWorkplaceSetup } (명세 4). accessToken 없음 — 인증은 Session 쿠키.
 * @param {object} payload loginId, password, role(로그인 페이지의 역할 토글 → expectedRole 로 전송)
 * 서버는 expectedRole 과 DB role 불일치 시 403 ROLE_MISMATCH 를 반환한다.
 */
export async function login({ loginId, password, role = 'OWNER' }) {
  // 가입 때 소문자로 저장하므로 로그인도 같은 정규화를 거쳐야 대소문자를 섞어 입력해도 인증된다.
  const normalizedLoginId = normalizeLoginId(loginId)
  if (USE_MOCK) {
    return {
      role,
      name: role === 'OWNER' ? '김사장' : '이알바',
      // 사장: 사업장 0개면 true. mock 은 아이디에 'new' 포함 시 첫 로그인 흉내.
      needsWorkplaceSetup: role === 'OWNER' && normalizedLoginId.includes('new')
    }
  }
  const { data } = await http.post('/auth/login', {
    loginId: normalizedLoginId,
    password,
    expectedRole: role
  })
  return data
}

/** 로그아웃 (명세 5) */
export async function logout() {
  if (USE_MOCK) return
  await http.post('/auth/logout')
}
