import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { login as loginApi, logout as logoutApi } from '@/services/auth'

// TODO(로그인 구현 전 임시): 로그인 화면 없이 화면을 확인하려고 계정을 시드한다.
// 확인할 화면의 역할에 맞춰 DEV_SEED.role 만 바꾸면 된다('OWNER' | 'WORKER').
// 로그인/온보딩이 붙으면 이 시드를 제거하고 초기값을 null 로 바꾼다.
const DEV_SEED = {
  name: '김사장',
  role: 'OWNER', // 'OWNER' | 'WORKER'
  needsWorkplaceSetup: false // 사장 사업장 0개 여부(G7)
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref({ ...DEV_SEED })
  const token = ref(localStorage.getItem('accessToken') || null)

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
   * 로그인. 서비스 응답으로 사용자·토큰을 채운다.
   * @param {object} credentials loginId, password, role(로그인 페이지의 역할 힌트)
   * @returns 로그인 응답(needsWorkplaceSetup 등 후처리에 사용)
   */
  async function login(credentials) {
    const res = await loginApi(credentials)
    user.value = {
      name: res.name,
      role: res.role,
      needsWorkplaceSetup: res.needsWorkplaceSetup ?? false
    }
    token.value = res.accessToken ?? null
    if (token.value) localStorage.setItem('accessToken', token.value)
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
      token.value = null
      localStorage.removeItem('accessToken')
    }
  }

  return {
    user,
    token,
    isAuthenticated,
    role,
    needsWorkplaceSetup,
    homeRoute,
    login,
    setUser,
    logout
  }
})
