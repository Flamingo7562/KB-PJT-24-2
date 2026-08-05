/**
 * 지점 select 계약 테스트.
 * 목록에는 INACTIVE 가 함께 오지만 전역 작업 Context 로 선택할 수 있는 것은 ACTIVE
 * 뿐이다(API_SPEC.md:366). INACTIVE 가 옵션에 노출되면 사용자가 고를 수 있게 된다.
 */
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ path: '/owner/attendance' })
}))
vi.mock('@/services/workplaces', () => ({ listWorkplaces: vi.fn() }))
vi.mock('@/services/notifications', () => ({
  listNotifications: vi.fn().mockResolvedValue([]),
  markAllRead: vi.fn()
}))

import AppTopBar from '@/components/common/AppTopBar.vue'
import { useAuthStore } from '@/stores/auth'
import { useWorkplaceStore } from '@/stores/workplace'

describe('AppTopBar 지점 select', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('INACTIVE 사업장은 옵션에 나오지 않는다', async () => {
    useAuthStore().setUser({ name: '김사장', role: 'OWNER', needsWorkplaceSetup: false })
    const workplace = useWorkplaceStore()
    workplace.workplaces = [
      { workplaceId: 1, name: '폐점한 강남점', status: 'INACTIVE' },
      { workplaceId: 2, name: '홍대점', status: 'ACTIVE' }
    ]
    workplace.selectedId = 2
    workplace.loaded = true

    // AppTopBar 의 role 은 부모 레이아웃(OwnerTabLayout)이 정적으로 박아 넣는 필수 prop이라
    // auth 스토어에서 유도되지 않는다 — select 렌더 분기를 타려면 직접 전달해야 한다.
    const wrapper = mount(AppTopBar, {
      props: { role: 'OWNER' },
      global: { stubs: { LogoSymbol: true } }
    })

    const optionLabels = wrapper.findAll('option').map((o) => o.text())
    expect(optionLabels).toEqual(['홍대점'])
  })
})
