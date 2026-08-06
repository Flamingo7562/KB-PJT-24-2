/**
 * 사장 마이페이지 프로필 카드 렌더링 계약 테스트.
 * getMe·getBadge 는 서로 다른 Endpoint 다(뱃지는 #182 구현 전까지 404).
 * onMounted 에서 Promise.all 로 묶으면 badge 하나의 실패가 me 까지 함께 날려
 * `v-if="me && badge"` 게이트를 절대 통과하지 못한다 — 이 브랜치가 낳은 회귀다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const push = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' }
}))

vi.mock('@/services/users', () => ({
  getMe: vi.fn(),
  getBadge: vi.fn(),
  deleteMe: vi.fn()
}))

import { getBadge, getMe } from '@/services/users'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
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

/** Teleport 를 stub 해 탈퇴 Modal 내용을 wrapper 안에서 찾을 수 있게 한다. */
function mountView() {
  return mount(OwnerMyPageView, { global: { stubs: { teleport: true } } })
}

function findByText(wrapper, selector, text) {
  const found = wrapper.findAll(selector).find((el) => el.text().trim() === text)
  if (!found) throw new Error(`'${text}' ${selector} 를 찾지 못했습니다`)
  return found
}

describe('OwnerMyPageView', () => {
  let logout
  let toastSpy

  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockClear()
    getMe.mockReset()
    getBadge.mockReset()
    logout = vi.spyOn(useAuthStore(), 'logout').mockResolvedValue()
    toastSpy = vi.spyOn(useUiStore(), 'toast')
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

  it('로그아웃을 누르면 세션을 정리하고 온보딩으로 이동한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    await findByText(wrapper, 'button', '로그아웃').trigger('click')
    await flushPromises()

    expect(logout).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith('/')
  })

  it('로그아웃 서버 호출이 실패해도 온보딩으로 이동하고 실패를 알린다', async () => {
    // authStore.logout() 은 실패해도 로컬 상태를 이미 비운 뒤라 화면에 남으면 상태와 어긋난다.
    logout.mockRejectedValue({ response: { data: { message: '로그아웃에 실패했습니다.' } } })
    const wrapper = mountView()
    await flushPromises()

    await findByText(wrapper, 'button', '로그아웃').trigger('click')
    await flushPromises()

    expect(push).toHaveBeenCalledWith('/')
    expect(toastSpy).toHaveBeenCalledWith(
      '로그아웃에 실패했습니다.',
      expect.objectContaining({ type: 'danger' })
    )
  })

  it('회원 탈퇴 링크는 그대로 동작한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    await findByText(wrapper, 'button', '회원 탈퇴').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('탈퇴하면 되돌릴 수 없어요')
  })
})
