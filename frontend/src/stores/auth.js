import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

// TODO(로그인 구현 전 임시): 로그인 화면·API 가 없어 화면 확인용으로 계정을 시드한다.
// 확인할 화면의 역할에 맞춰 SEED_ROLE 만 바꾸면 된다('OWNER' | 'WORKER').
// 로그인/온보딩이 붙으면 이 시드를 제거하고 초기값을 null 로 바꾼다.
const SEED_ROLE = 'WORKER'
const SEED_NAME = SEED_ROLE === 'OWNER' ? '김사장' : '박알바'

/**
 * 로그인 사용자 상태.
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref({
    name: SEED_NAME,
    role: SEED_ROLE, // 'OWNER' | 'WORKER'
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
