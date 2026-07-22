/**
 * 인증/회원 API 서비스.
 *
 * 백엔드 연동 전 목(mock) 반환. 백엔드 준비 시 USE_MOCK=false 로만 바꾼다.
 * 컴포넌트·스토어는 함수 시그니처만 의존한다(docs/rules/frontend.md).
 *
 * 관련 API(명세 1~5):
 *   GET  /api/auth/check-login-id   GET /api/auth/check-email
 *   POST /api/auth/signup   POST /api/auth/login   POST /api/auth/logout
 */
import http from '@/services/http'

const USE_MOCK = true

/** 아이디 중복확인 → { available } (명세 1) */
export async function checkLoginId(loginId) {
  if (USE_MOCK) return { available: loginId !== 'taken' }
  const { data } = await http.get('/auth/check-login-id', { params: { loginId } })
  return data
}

/** 이메일 중복확인 → { available } (명세 2) */
export async function checkEmail(email) {
  if (USE_MOCK) return { available: email !== 'taken@test.com' }
  const { data } = await http.get('/auth/check-email', { params: { email } })
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
 * 로그인 → { accessToken, role, name, needsWorkplaceSetup } (명세 4).
 * @param {object} payload loginId, password, role(mock 전용: 로그인 페이지의 역할 힌트)
 * 실제 API 는 role 을 서버가 결정한다. mock 은 이 힌트로 역할을 흉내낸다.
 */
export async function login({ loginId, password, role = 'OWNER' }) {
  if (USE_MOCK) {
    return {
      accessToken: 'mock-token',
      role,
      name: role === 'OWNER' ? '김사장' : '이알바',
      // 사장: 사업장 0개면 true. mock 은 아이디에 'new' 포함 시 첫 로그인 흉내.
      needsWorkplaceSetup: role === 'OWNER' && String(loginId).includes('new')
    }
  }
  const { data } = await http.post('/auth/login', { loginId, password })
  return data
}

/** 로그아웃 (명세 5) */
export async function logout() {
  if (USE_MOCK) return
  await http.post('/auth/logout')
}
