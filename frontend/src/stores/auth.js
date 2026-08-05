import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { fetchCsrf, getSession, login as loginApi, logout as logoutApi } from '@/services/auth'
import { useWorkplaceStore } from '@/stores/workplace'

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

  /** 세션 상태를 서버 기준으로 다시 읽는다. CSRF 는 건드리지 않는다. */
  async function refreshSession() {
    const session = await getSession()
    user.value = session?.authenticated
      ? {
          name: session.name,
          role: session.role,
          needsWorkplaceSetup: session.needsWorkplaceSetup ?? false
        }
      : null
    return session
  }

  /**
   * G0 — 앱 시작 시 세션 복원. GET /api/auth/session 결과만으로 인증 상태를 판단한다.
   * 로컬 저장 토큰·플래그는 사용하지 않는다(JSESSIONID 가 단일 기준).
   * 라우터 가드는 이 함수 완료 후 실행되어야 한다(main.js).
   */
  async function bootstrap() {
    try {
      await fetchCsrf()
      await refreshSession()
    } catch {
      user.value = null
    } finally {
      ready.value = true
    }
  }

  /**
   * 로그인. 서버가 Session ID 를 교체하면서 CSRF Token 도 회전하므로, 성공 직후
   * CSRF 를 다시 준비하지 않으면 첫 상태변경 요청이 403 이 된다(API_SPEC.md:91).
   * 실패 경로에서는 Session 이 바뀌지 않았으므로 재준비하지 않는다.
   */
  async function login(credentials) {
    const res = await loginApi(credentials)
    user.value = {
      name: res.name,
      role: res.role,
      needsWorkplaceSetup: res.needsWorkplaceSetup ?? false
    }
    await fetchCsrf()
    return res
  }

  function setUser(next) {
    user.value = next
  }

  /**
   * 로그아웃. 서버 Session 무효화가 성공한 뒤에만 CSRF 를 재준비한다 — 실패 경로에서는
   * Session 이 아직 살아 있으므로 재준비하지 않는다(login()과 대칭).
   * 클라이언트 상태(user, 사업장 Context)는 서버 호출 성공·실패와 무관하게 항상 비운다.
   * 사업장 Context 를 비우지 않으면 다른 계정으로 로그인했을 때 이전 사장의 목록이 남는다.
   * CSRF 재준비(재시도 없는 GET)가 실패해도 로그아웃 자체를 실패로 취급하지 않는다 — 서버
   * Session 은 이미 무효화됐고 클라이언트 상태도 이미 비워졌으므로, 호출자에게는 로그아웃이
   * 끝난 것으로 보여야 한다.
   */
  async function logout() {
    try {
      await logoutApi()
    } finally {
      user.value = null
      useWorkplaceStore().reset()
    }
    try {
      await fetchCsrf()
    } catch {
      // 재준비 실패는 로그아웃 실패가 아니다 — 삼키고 다음 요청에서 403 이면 그때 재시도된다.
    }
  }

  return {
    user,
    ready,
    isAuthenticated,
    role,
    needsWorkplaceSetup,
    homeRoute,
    refreshSession,
    bootstrap,
    login,
    setUser,
    logout
  }
})
