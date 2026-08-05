/**
 * 알바생 마이페이지 프로필 카드 렌더링 계약 테스트.
 * getMe·getBadge 는 서로 다른 Endpoint 다(뱃지는 #182 구현 전까지 404).
 * onMounted 에서 Promise.all 로 묶으면 badge 하나의 실패가 me 까지 함께 날려
 * `v-if="me && badge"` 게이트를 절대 통과하지 못한다 — 이 브랜치가 낳은 회귀다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' }
}))

vi.mock('@/services/users', () => ({
  getMe: vi.fn(),
  getBadge: vi.fn(),
  deleteMe: vi.fn()
}))

import { getBadge, getMe } from '@/services/users'
import WorkerMyPageView from '@/views/worker/WorkerMyPageView.vue'

const ME = { loginId: 'worker01', email: 'worker@test.com', name: '이알바', role: 'WORKER' }
const BADGE = {
  badgeType: 'TRUST_WORKER',
  level: 1,
  recentCount: 6,
  remainingToNextLevel: 9,
  criterionLabel: '성실근로',
  criterionDesc: '*성실근로란? 무단 결근·지각 없이 근무 완료'
}

describe('WorkerMyPageView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    getMe.mockReset()
    getBadge.mockReset()
  })

  it('뱃지 조회가 실패해도 프로필 카드는 뱃지 없이 그대로 보여준다', async () => {
    getMe.mockResolvedValue({ ...ME })
    getBadge.mockRejectedValue(new Error('Request failed with status code 404'))

    const wrapper = mount(WorkerMyPageView)
    await flushPromises()

    expect(wrapper.find('.profile-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('이알바')
    expect(wrapper.find('.badge-slot').exists()).toBe(false)
  })

  it('두 요청이 모두 성공하면 뱃지도 함께 보여준다', async () => {
    getMe.mockResolvedValue({ ...ME })
    getBadge.mockResolvedValue({ ...BADGE })

    const wrapper = mount(WorkerMyPageView)
    await flushPromises()

    expect(wrapper.find('.badge-slot').exists()).toBe(true)
    expect(wrapper.text()).toContain('성실근로')
  })
})
