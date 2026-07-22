import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

/**
 * 로그인 사용자 상태.
 *
 * TODO(로그인 구현 전 임시): 로그인 화면·API 가 없어 화면 확인용으로 사장 계정을
 * 시드해 둔다. 로그인 기능이 붙으면 초기값을 null 로 바꾸고 login() 에서 채운다.
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref({
    name: '김사장',
    role: 'OWNER', // 'OWNER' | 'WORKER'
    needsWorkplaceSetup: false // 사장 사업장 0개 여부(G7)
  })

  const isAuthenticated = computed(() => user.value !== null)
  const role = computed(() => user.value?.role ?? null)

  /** 역할별 홈 경로 */
  function homeRoute() {
    if (role.value === 'OWNER') return '/owner/home'
    if (role.value === 'WORKER') return '/worker/home'
    return '/'
  }

  function setUser(next) {
    user.value = next
  }

  function logout() {
    user.value = null
  }

  return { user, isAuthenticated, role, homeRoute, setUser, logout }
})
