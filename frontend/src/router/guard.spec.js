/**
 * 라우터 가드 회귀 테스트 (라우팅테이블 G1~G7).
 * 프론트 가드는 UX 용이며 실제 권한 검증은 서버가 한다(G6). 여기서 고정하는 것은
 * "가드가 스스로 무한 이동을 만들지 않는다"는 성질이다. G7 은 needsWorkplaceSetup 이
 * true 인 동안 /owner 경로를 등록 화면으로 되돌리므로, 등록 화면 자신은 예외여야 한다.
 */
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

vi.mock('@/services/auth', () => ({
  fetchCsrf: vi.fn(),
  getSession: vi.fn(),
  login: vi.fn(),
  logout: vi.fn()
}))
vi.mock('@/services/workplaces', () => ({ listWorkplaces: vi.fn() }))
vi.mock('@/services/notifications', () => ({
  listNotifications: vi.fn().mockResolvedValue([]),
  markAllRead: vi.fn()
}))

import appRouter, { routeGuard } from '@/router'
import { useAuthStore } from '@/stores/auth'

const Blank = { template: '<div />' }

/**
 * 실제 라우트 정의와 가드를 그대로 쓰되, 화면 컴포넌트는 비워 mount 비용을 없앤다.
 * getRoutes()가 주는 평탄화 목록은 부모(OWNER/WORKER 탭 레이아웃)의 meta 를 자식에 병합하지
 * 않는다 — 중첩 matched 배열의 meta 병합은 resolve() 시점에만 일어나므로, children 구조를
 * 그대로 살려야 실제 앱과 같은 meta 병합 결과(G1·G2)를 얻는다.
 */
function toBlankRoute(route) {
  const blank = { path: route.path, name: route.name, meta: route.meta, component: Blank }
  if (route.redirect !== undefined) blank.redirect = route.redirect
  if (route.children) blank.children = route.children.map(toBlankRoute)
  return blank
}

function buildRouter() {
  const routes = appRouter.options.routes.map(toBlankRoute)
  const router = createRouter({ history: createMemoryHistory(), routes })
  router.beforeEach(routeGuard)
  return router
}

describe('라우터 가드', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('비로그인으로 보호 경로에 가면 온보딩으로 복귀 경로와 함께 보낸다', async () => {
    const router = buildRouter()
    await router.push('/owner/attendance')

    expect(router.currentRoute.value.path).toBe('/')
    expect(router.currentRoute.value.query.redirect).toBe('/owner/attendance')
  })

  it('OWNER 가 WORKER 전용 경로에 가면 OWNER 홈으로 되돌린다', async () => {
    const auth = useAuthStore()
    auth.setUser({ name: '김사장', role: 'OWNER', needsWorkplaceSetup: false })

    const router = buildRouter()
    await router.push('/worker/home')

    expect(router.currentRoute.value.path).toBe('/owner/home')
  })

  it('사업장 설정이 필요한 OWNER 는 등록 화면으로 모인다', async () => {
    const auth = useAuthStore()
    auth.setUser({ name: '김사장', role: 'OWNER', needsWorkplaceSetup: true })

    const router = buildRouter()
    await router.push('/owner/attendance')

    expect(router.currentRoute.value.path).toBe('/owner/workplaces/new')
  })

  it('등록 화면 자신은 G7 에 걸려 무한 이동하지 않는다', async () => {
    const auth = useAuthStore()
    auth.setUser({ name: '김사장', role: 'OWNER', needsWorkplaceSetup: true })

    const router = buildRouter()
    await router.push('/owner/workplaces/new')

    expect(router.currentRoute.value.path).toBe('/owner/workplaces/new')
  })

  it('정의되지 않은 경로는 온보딩으로 숨기지 않고 404 로 남는다', async () => {
    const router = buildRouter()
    await router.push('/does-not-exist')

    expect(router.currentRoute.value.name).toBe('not-found')
  })

  it('충전 확인 화면은 URL에 계좌 정보를 넣지 않는 OWNER 보호 라우트다', () => {
    const resolved = appRouter.resolve({ name: 'owner-charge-confirm' })

    expect(resolved.path).toBe('/owner/wallet/charge/confirm')
    expect(resolved.meta).toMatchObject({ requiresAuth: true, role: 'OWNER' })
    expect(resolved.query).toEqual({})
    expect(resolved.params).toEqual({})
  })
})
