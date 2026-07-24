/**
 * 인증/회원 API 서비스.
 *
 * 백엔드 연동 전 목(mock) 반환. 백엔드 준비 시 USE_MOCK=false 로만 바꾼다.
 * 컴포넌트·스토어는 함수 시그니처만 의존한다(docs/rules/frontend.md).
 *
 * 인증은 Session(JSESSIONID) 전용 — accessToken 없음(저장·전송 금지).
 * 관련 API(명세 AUTH-001~006):
 *   GET  /api/auth/csrf   GET /api/auth/session
 *   GET  /api/auth/login-id-availability   GET /api/auth/email-availability
 *   POST /api/auth/signup   POST /api/auth/login   POST /api/auth/logout
 */
import http from '@/services/http'

const USE_MOCK = true

/**
 * CSRF 토큰 발급 (GET /api/auth/csrf → XSRF-TOKEN 쿠키).
 * 이후 모든 상태변경 요청은 http 인터셉터가 X-XSRF-TOKEN 을 첨부한다.
 */
export async function fetchCsrf() {
  if (USE_MOCK) return
  await http.get('/auth/csrf')
}

/**
 * 세션 복원 (GET /api/auth/session). 앱 시작·새로고침 시 인증·역할·사업장 설정 상태를 복구한다.
 * 로컬 저장 토큰·플래그로 인증을 판단하지 않는다(JSESSIONID 가 단일 기준).
 * @returns {Promise<{authenticated:boolean, role?:string, name?:string, needsWorkplaceSetup?:boolean}>}
 */
export async function getSession() {
  if (USE_MOCK) return { authenticated: false }
  // 미인증 시 401 이 정상 흐름이므로 전역 리다이렉트(G5)를 건너뛴다(부트스트랩 probe).
  const { data } = await http.get('/auth/session', { skipAuthRedirect: true })
  return data
}

/** 아이디 중복확인 → { available } (AUTH-001) */
export async function checkLoginId(loginId) {
  if (USE_MOCK) return { available: loginId !== 'taken' }
  const { data } = await http.get('/auth/login-id-availability', { params: { loginId } })
  return data
}

/** 이메일 중복확인 → { available } (AUTH-002) */
export async function checkEmail(email) {
  if (USE_MOCK) return { available: email !== 'taken@test.com' }
  const { data } = await http.get('/auth/email-availability', { params: { email } })
  return data
}

/**
 * 회원가입 → { userId } (명세 3, 201).
 * @param {object} payload loginId, password, passwordConfirm, name, email, phone?, role('OWNER'|'WORKER')
 */
export async function signup(payload) {
  if (USE_MOCK) return { userId: 1 }
  const { data } = await http.post('/auth/signup', payload)
  return data
}

/**
 * 로그인 → { role, name, needsWorkplaceSetup } (명세 4). accessToken 없음 — 인증은 Session 쿠키.
 * @param {object} payload loginId, password, role(로그인 페이지의 역할 토글 → expectedRole 로 전송)
 * 서버는 expectedRole 과 DB role 불일치 시 403 ROLE_MISMATCH 를 반환한다.
 */
export async function login({ loginId, password, role = 'OWNER' }) {
  if (USE_MOCK) {
    return {
      role,
      name: role === 'OWNER' ? '김사장' : '이알바',
      // 사장: 사업장 0개면 true. mock 은 아이디에 'new' 포함 시 첫 로그인 흉내.
      needsWorkplaceSetup: role === 'OWNER' && String(loginId).includes('new')
    }
  }
  const { data } = await http.post('/auth/login', { loginId, password, expectedRole: role })
  return data
}

/** 로그아웃 (명세 5) */
export async function logout() {
  if (USE_MOCK) return
  await http.post('/auth/logout')
}
