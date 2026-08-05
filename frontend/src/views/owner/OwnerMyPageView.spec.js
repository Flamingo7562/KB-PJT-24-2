/**
 * 사장 마이페이지 프로필 카드 렌더링 계약 테스트.
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
import OwnerMyPageView from '@/views/owner/OwnerMyPageView.vue'

const ME = { loginId: 'owner01', email: 'owner@test.com', name: '김사장', role: 'OWNER' }
const BADGE = {
  badgeType: 'TRUST_OWNER',
  level: 2,
  recentCount: 12,
  remainingToNextLevel: 3,
  criterionLabel: '안심거래',
  criterionDesc: '*안심거래란? 임금분쟁 신고 없이 정상 정산 완료'
}

describe('OwnerMyPageView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    getMe.mockReset()
    getBadge.mockReset()
  })

  it('뱃지 조회가 실패해도 프로필 카드는 뱃지 없이 그대로 보여준다', async () => {
    getMe.mockResolvedValue({ ...ME })
    getBadge.mockRejectedValue(new Error('Request failed with status code 404'))

    const wrapper = mount(OwnerMyPageView)
    await flushPromises()

    expect(wrapper.find('.profile-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('김사장')
    expect(wrapper.find('.badge-slot').exists()).toBe(false)
  })

  it('두 요청이 모두 성공하면 뱃지도 함께 보여준다', async () => {
    getMe.mockResolvedValue({ ...ME })
    getBadge.mockResolvedValue({ ...BADGE })

    const wrapper = mount(OwnerMyPageView)
    await flushPromises()

    expect(wrapper.find('.badge-slot').exists()).toBe(true)
    expect(wrapper.text()).toContain('안심거래')
  })

  it('내 정보 조회가 실패하면 프로필 카드를 보여주지 않는다', async () => {
    getMe.mockRejectedValue(new Error('Request failed with status code 401'))
    getBadge.mockResolvedValue({ ...BADGE })

    const wrapper = mount(OwnerMyPageView)
    await flushPromises()

    expect(wrapper.find('.profile-card').exists()).toBe(false)
  })
})
