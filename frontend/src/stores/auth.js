import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { fetchCsrf, getSession, login as loginApi, logout as logoutApi } from '@/services/auth'

/**
 * 로그인 사용자 상태. 인증은 Session(JSESSIONID) 전용 — 저장 토큰 없음.
 * 비로그인 시 user=null(온보딩/로그인 화면).
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const ready = ref(false) // 세션 부트스트랩(G0) 완료 여부

  const isAuthenticated = computed(() => user.value !== null)
  const role = computed(() => user.value?.role ?? null)
  const needsWorkplaceSetup = computed(() => user.value?.needsWorkplaceSetup === true)

  /** 역할별 홈 경로 */
  function homeRoute() {
    if (role.value === 'OWNER') return '/owner/home'
    if (role.value === 'WORKER') return '/worker/home'
    return '/'
  }

  /**
   * G0 — 앱 시작 시 세션 복원. GET /api/auth/session 결과만으로 인증 상태를 판단한다.
   * 로컬 저장 토큰·플래그는 사용하지 않는다. CSRF 쿠키도 함께 준비한다(이후 상태변경 요청 대비).
   * 라우터 가드는 이 함수 완료 후 실행되어야 한다(main.js).
   */
  async function bootstrap() {
    try {
      await fetchCsrf()
      const session = await getSession()
      user.value = session?.authenticated
        ? {
            name: session.name,
            role: session.role,
            needsWorkplaceSetup: session.needsWorkplaceSetup ?? false
          }
        : null
    } catch {
      user.value = null
    } finally {
      ready.value = true
    }
  }

  /**
   * 로그인. 서비스 응답으로 사용자를 채운다(토큰 저장 없음 — Session 쿠키가 서버에서 설정됨).
   * @param {object} credentials loginId, password, role(로그인 페이지의 역할 토글 → expectedRole)
   * @returns 로그인 응답(needsWorkplaceSetup 등 후처리에 사용)
   */
  async function login(credentials) {
    const res = await loginApi(credentials)
    user.value = {
      name: res.name,
      role: res.role,
      needsWorkplaceSetup: res.needsWorkplaceSetup ?? false
    }
    return res
  }

  function setUser(next) {
    user.value = next
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      user.value = null
    }
  }

  return {
    user,
    ready,
    isAuthenticated,
    role,
    needsWorkplaceSetup,
    homeRoute,
    bootstrap,
    login,
    setUser,
    logout
  }
})
