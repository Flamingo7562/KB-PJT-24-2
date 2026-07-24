import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

/*
 * 라우팅 테이블(.ai-local) 전 30라우트.
 * - 탭 화면(홈·근태/근로·문서·QR)은 역할별 탭 레이아웃의 자식으로 둔다(상단바+하단탭 공유).
 * - 서브 화면(충전·상세·마이 하위 등)·인증·딥링크는 top-level 라우트 + AppBackHeader.
 * - meta: requiresAuth / role('OWNER'|'WORKER') / guestOnly / invite.
 * 전 view 는 lazy import — 팀원은 배정된 view 파일만 채운다(router 는 미수정).
 */

const OWNER = { requiresAuth: true, role: 'OWNER' }
const WORKER = { requiresAuth: true, role: 'WORKER' }

const routes = [
  // ───────── 진입 / 인증 ─────────
  {
    path: '/',
    name: 'onboarding',
    component: () => import('@/views/OnboardingView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/owner/login',
    name: 'owner-login',
    component: () => import('@/views/auth/OwnerLoginView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/owner/signup',
    name: 'owner-signup',
    component: () => import('@/views/auth/OwnerSignupView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/worker/login',
    name: 'worker-login',
    component: () => import('@/views/auth/WorkerLoginView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/worker/signup',
    name: 'worker-signup',
    component: () => import('@/views/auth/WorkerSignupView.vue'),
    meta: { guestOnly: true }
  },

  // ───────── 사장: 탭 화면 (상단바 + 하단탭) ─────────
  {
    path: '/owner',
    component: () => import('@/layouts/OwnerTabLayout.vue'),
    meta: OWNER,
    children: [
      { path: '', redirect: '/owner/home' },
      {
        path: 'home',
        name: 'owner-home',
        component: () => import('@/views/owner/OwnerHomeView.vue')
      },
      {
        path: 'attendance',
        name: 'owner-attendance',
        component: () => import('@/views/owner/OwnerAttendanceView.vue')
      },
      {
        path: 'documents',
        name: 'owner-documents',
        component: () => import('@/views/owner/OwnerDocumentsView.vue')
      },
      { path: 'qr', name: 'owner-qr', component: () => import('@/views/owner/OwnerQrView.vue') }
    ]
  },
  // ───────── 사장: 서브 화면 (뒤로가기 헤더) ─────────
  {
    path: '/owner/workplaces/new',
    name: 'owner-workplace-new',
    component: () => import('@/views/owner/workplace/OwnerWorkplaceNewView.vue'),
    meta: OWNER
  },
  {
    path: '/owner/wallet/charge',
    name: 'owner-charge',
    component: () => import('@/views/owner/wallet/OwnerChargeView.vue'),
    meta: OWNER
  },
  {
    path: '/owner/wallet/withdraw',
    name: 'owner-withdraw',
    component: () => import('@/views/owner/wallet/OwnerWithdrawView.vue'),
    meta: OWNER
  },
  {
    path: '/owner/attendance/work-cases/new',
    name: 'owner-work-case-new',
    component: () => import('@/views/owner/workCase/OwnerWorkCaseNewView.vue'),
    meta: OWNER
  },
  {
    path: '/owner/attendance/work-cases/:workCaseId',
    name: 'owner-work-case-detail',
    component: () => import('@/views/owner/workCase/OwnerWorkCaseDetailView.vue'),
    meta: OWNER
  },
  {
    path: '/owner/documents/:documentId',
    name: 'owner-document-viewer',
    component: () => import('@/views/owner/OwnerDocumentViewerView.vue'),
    meta: OWNER
  },
  {
    path: '/owner/mypage',
    name: 'owner-mypage',
    component: () => import('@/views/owner/OwnerMyPageView.vue'),
    meta: OWNER
  },
  {
    path: '/owner/mypage/workplaces',
    name: 'owner-workplaces',
    component: () => import('@/views/owner/workplace/OwnerWorkplaceManageView.vue'),
    meta: OWNER
  },
  {
    path: '/owner/mypage/profile',
    name: 'owner-profile',
    component: () => import('@/views/owner/mypage/OwnerProfileEditView.vue'),
    meta: OWNER
  },
  {
    path: '/owner/mypage/password',
    name: 'owner-password',
    component: () => import('@/views/owner/mypage/OwnerPasswordEditView.vue'),
    meta: OWNER
  },

  // ───────── 알바생: 탭 화면 (상단바 + 하단탭) ─────────
  {
    path: '/worker',
    component: () => import('@/layouts/WorkerTabLayout.vue'),
    meta: WORKER,
    children: [
      { path: '', redirect: '/worker/home' },
      {
        path: 'home',
        name: 'worker-home',
        component: () => import('@/views/worker/WorkerHomeView.vue')
      },
      {
        path: 'work',
        name: 'worker-work',
        component: () => import('@/views/worker/WorkerWorkView.vue')
      },
      {
        path: 'scan',
        name: 'worker-scan',
        component: () => import('@/views/worker/WorkerScanView.vue')
      },
      {
        path: 'documents',
        name: 'worker-documents',
        component: () => import('@/views/worker/WorkerDocumentsView.vue')
      }
    ]
  },
  // ───────── 알바생: 서브 화면 (뒤로가기 헤더) ─────────
  {
    path: '/worker/wallet/withdraw',
    name: 'worker-withdraw',
    component: () => import('@/views/worker/wallet/WorkerWithdrawView.vue'),
    meta: WORKER
  },
  {
    path: '/worker/work/work-cases/:workCaseId',
    name: 'worker-work-case-detail',
    component: () => import('@/views/worker/workCase/WorkerWorkCaseDetailView.vue'),
    meta: WORKER
  },
  {
    path: '/worker/work/work-cases/:workCaseId/report',
    name: 'worker-report',
    component: () => import('@/views/worker/workCase/WorkerReportView.vue'),
    meta: WORKER
  },
  {
    path: '/worker/documents/:documentId',
    name: 'worker-document-viewer',
    component: () => import('@/views/worker/WorkerDocumentViewerView.vue'),
    meta: WORKER
  },
  {
    path: '/worker/mypage',
    name: 'worker-mypage',
    component: () => import('@/views/worker/WorkerMyPageView.vue'),
    meta: WORKER
  },
  {
    path: '/worker/mypage/profile',
    name: 'worker-profile',
    component: () => import('@/views/worker/mypage/WorkerProfileEditView.vue'),
    meta: WORKER
  },
  {
    path: '/worker/mypage/password',
    name: 'worker-password',
    component: () => import('@/views/worker/mypage/WorkerPasswordEditView.vue'),
    meta: WORKER
  },

  // ───────── 딥링크: 근무 확정 (WORKER 전용) ─────────
  {
    path: '/invitations/:token',
    name: 'invite-confirm',
    component: () => import('@/views/invite/InviteConfirmView.vue'),
    meta: { requiresAuth: true, role: 'WORKER', invite: true }
  },

  // ───────── 오류 화면 (의미 분리: 403 ≠ 404, catch-all 을 /로 리다이렉트하지 않는다) ─────────
  {
    path: '/forbidden',
    name: 'forbidden',
    component: () => import('@/views/error/ForbiddenView.vue')
  },
  // 정의되지 않은 경로는 404 화면(온보딩 리다이렉트 금지 — 잘못된 주소를 홈으로 숨기지 않는다)
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

/**
 * 라우터 가드 (라우팅테이블 G1~G7).
 * 프론트 가드는 UX용이며, 실제 권한·상태 검증은 서버가 최종 수행한다(G6).
 */
router.beforeEach((to) => {
  const auth = useAuthStore()
  const ui = useUiStore()

  // G3: 게스트 전용(온보딩·로그인·회원가입)에 로그인 상태로 접근 → 역할 홈
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return auth.homeRoute()
  }

  // G1: 인증 필요 화면에 비로그인 접근 → 목적지 보존 후 이동
  //     (초대 딥링크는 알바생 로그인으로, 그 외는 온보딩으로 — G4)
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    if (to.meta.invite) return { path: '/worker/login', query: { redirect: to.fullPath } }
    return { path: '/', query: { redirect: to.fullPath } }
  }

  // G4: 초대 딥링크는 WORKER 전용 — OWNER 접근 차단
  if (to.meta.invite && auth.isAuthenticated && auth.role === 'OWNER') {
    ui.toast('알바생 전용 링크입니다.', { type: 'warning' })
    return auth.homeRoute()
  }

  // G2: 역할 불일치 → 각자 홈 + 안내 토스트
  if (to.meta.role && auth.isAuthenticated && auth.role !== to.meta.role) {
    ui.toast('접근 권한이 없어 홈으로 이동했어요.', { type: 'warning' })
    return auth.homeRoute()
  }

  // G7: 사장 첫 로그인(사업장 0개)이면 사업장 등록 화면으로 강제 이동
  if (
    auth.isAuthenticated &&
    auth.role === 'OWNER' &&
    auth.needsWorkplaceSetup &&
    to.path.startsWith('/owner') &&
    to.path !== '/owner/workplaces/new'
  ) {
    return '/owner/workplaces/new'
  }

  return true
})

export default router
