import axios from 'axios'

/**
 * 공통 HTTP 클라이언트 (docs/rules/frontend.md 'HTTP · 인증' 절 기준).
 *
 * - 인증은 Session(JSESSIONID) 전용 — accessToken 저장·전송 금지. withCredentials 로 쿠키 동봉.
 * - CSRF: XSRF-TOKEN 쿠키를 읽어 모든 상태변경(POST/PUT/PATCH/DELETE)에 X-XSRF-TOKEN 자동 첨부.
 * - 성공 응답 { data } 언래핑: 서비스는 본문(response.data)만 받는다.
 * - 오류 { code, message, traceId, fieldErrors } 필드를 axios error 에 부착(원본 error.response 보존).
 * - 401(AUTH_REQUIRED/SESSION_EXPIRED): 세션 상태를 버리고 온보딩(/)으로 이동(G5).
 *
 * ※ 현재 서비스는 USE_MOCK=true 라 실제 요청은 발화하지 않는다(교체 시 변경 지점 한정).
 */
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10_000,
  withCredentials: true // JSESSIONID 쿠키 동봉 (accessToken 미사용)
})

const MUTATING_METHODS = ['post', 'put', 'patch', 'delete']

/** document.cookie 에서 name 쿠키 값을 읽는다(없으면 null). */
function readCookie(name) {
  if (typeof document === 'undefined') return null
  const match = document.cookie.match(new RegExp('(?:^|;\\s*)' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[1]) : null
}

/**
 * CSRF 요청 인터셉터.
 * XSRF-TOKEN 쿠키(GET /api/auth/csrf 로 발급)를 상태변경 요청 헤더 X-XSRF-TOKEN 에 자동 첨부.
 */
http.interceptors.request.use((config) => {
  const method = (config.method || 'get').toLowerCase()
  if (MUTATING_METHODS.includes(method)) {
    const token = readCookie('XSRF-TOKEN')
    if (token) config.headers.set('X-XSRF-TOKEN', token)
  }
  return config
})

/**
 * 응답 인터셉터.
 * 성공: 본문(response.data)만 반환 — 서비스는 { data } 를 벗겨 payload 를 얻는다.
 * 실패: 401 이면 세션을 버리고 온보딩으로 이동(G5). 표준 오류 필드(code/traceId/fieldErrors)를
 *       axios error 에 부착해 폼 매핑을 돕는다(원본 error.response 는 그대로 보존).
 */
http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const res = error.response

    // G5 — 401: 세션 상태를 버리고 온보딩으로. 복귀를 위해 현재 경로를 redirect 로 보존한다.
    //   (저장 토큰이 없으므로 폐기할 것이 없다. 세션 부트스트랩 probe 는 skipAuthRedirect 로 제외.)
    //   순환 import(http ← services ← stores)를 피하려 하드 리다이렉트한다 — 이때 Pinia 상태도 초기화된다.
    if (res?.status === 401 && !error.config?.skipAuthRedirect && typeof window !== 'undefined') {
      const here = window.location.pathname + window.location.search
      if (window.location.pathname !== '/') {
        window.location.assign(`/?redirect=${encodeURIComponent(here)}`)
      }
    }

    // 표준 오류 본문 필드를 편의상 error 에 부착(폼 fieldErrors 매핑용). error.response 는 유지.
    const body = res?.data
    if (body && typeof body === 'object') {
      error.code = body.code
      error.traceId = body.traceId
      error.fieldErrors = Array.isArray(body.fieldErrors) ? body.fieldErrors : []
    }
    return Promise.reject(error)
  }
)

/** 폼 오류 매핑 헬퍼: fieldErrors[{field, reason}] → { field: reason }(입력 필드 매핑용). */
export function fieldErrorMap(error) {
  const list = error?.fieldErrors ?? error?.response?.data?.fieldErrors ?? []
  return list.reduce((acc, { field, reason }) => {
    if (field) acc[field] = reason
    return acc
  }, {})
}

/** 멱등키(UUID) 생성. 충전·출금·초대수락·정산승인 등 금융 변경 요청 헤더에 사용. */
export function newIdempotencyKey() {
  const webCrypto = globalThis.crypto
  if (webCrypto && typeof webCrypto.randomUUID === 'function') {
    return webCrypto.randomUUID()
  }
  // 폴백: RFC4122 v4 유사(crypto.randomUUID 미지원 환경 대비).
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

/**
 * 멱등 POST — 하나의 Idempotency-Key 로 전송하고, 네트워크/5xx 일시 오류 시 같은 키로 재시도한다.
 * 서버가 응답한 4xx(검증·상태충돌·중복 등)는 재시도하지 않는다(docs/rules/api.md 멱등성).
 * 더블클릭·네트워크 재시도로 인한 중복 반영(중복 충전·출금·지급)을 방지한다.
 */
export async function idempotentPost(url, body = null, { retries = 2, config = {} } = {}) {
  const key = newIdempotencyKey()
  let lastError
  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      return await http.post(url, body, {
        ...config,
        headers: { ...(config.headers || {}), 'Idempotency-Key': key }
      })
    } catch (error) {
      lastError = error
      const status = error?.response?.status
      const transient = status === undefined || status >= 500 // 무응답(네트워크) 또는 서버 오류만 재시도
      if (!transient || attempt === retries) throw error
    }
  }
  throw lastError
}

export default http
