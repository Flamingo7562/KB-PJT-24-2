import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'
import OwnerHomeView from '@/views/owner/OwnerHomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 진입: 온보딩·역할 선택 (게스트 전용)
    {
      path: '/',
      name: 'onboarding',
      component: HomeView,
      meta: { guestOnly: true }
    },

    // ---- 사장(OWNER) ----
    {
      path: '/owner',
      redirect: '/owner/home'
    },
    {
      path: '/owner/home',
      name: 'owner-home',
      component: OwnerHomeView,
      meta: { requiresAuth: true, role: 'OWNER' }
    },

    // ---- 알바생(WORKER) : 라우트 골격만. 화면은 후속 이슈에서 추가 ----
    // { path: '/worker/home', name: 'worker-home', component: WorkerHomeView,
    //   meta: { requiresAuth: true, role: 'WORKER' } },

    // 정의되지 않은 경로는 온보딩으로
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

/**
 * 라우터 가드 (라우팅테이블 G1~G3, G7).
 * 프론트 가드는 UX용이며, 실제 권한·상태 검증은 서버가 최종 수행한다(G6).
 */
router.beforeEach((to) => {
  const auth = useAuthStore()

  // G3: 게스트 전용 화면에 로그인 상태로 접근 → 역할 홈으로
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return auth.homeRoute()
  }

  // G1: 인증 필요 화면에 비로그인 접근 → 온보딩으로(목적지 보존)
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { path: '/', query: { redirect: to.fullPath } }
  }

  // G2: 역할 불일치 → 각자 홈으로
  if (to.meta.role && auth.role !== to.meta.role) {
    return auth.homeRoute()
  }

  // G7: 사장 첫 로그인(사업장 0개)이면 사업장 등록으로 강제 이동
  // TODO: /owner/workplaces/new 라우트 추가 시 아래 리다이렉트 활성화
  // if (auth.role === 'OWNER' && auth.user?.needsWorkplaceSetup && to.path !== '/owner/workplaces/new') {
  //   return '/owner/workplaces/new'
  // }

  return true
})

export default router
