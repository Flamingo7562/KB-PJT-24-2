import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10_000,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN'
})

/**
 * G5 — 401 인터셉터.
 * API 401(미인증/만료) 수신 시 토큰을 폐기하고 온보딩(/)으로 이동한다.
 * 복귀를 위해 현재 경로를 redirect 쿼리로 보존한다.
 *
 * 순환 import(http ← services ← stores ← http)를 피하려고 라우터/스토어를 직접
 * 참조하지 않고 하드 리다이렉트한다. SPA 내비게이션이 필요하면 이 핸들러를
 * main.js 에서 주입하도록 바꿀 수 있다(교체 지점).
 * ※ 현재 서비스는 USE_MOCK=true 라 실제로는 발화하지 않는다.
 */
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      localStorage.removeItem('accessToken')
      const here = window.location.pathname + window.location.search
      if (window.location.pathname !== '/') {
        window.location.assign(`/?redirect=${encodeURIComponent(here)}`)
      }
    }
    return Promise.reject(error)
  }
)

export default http
