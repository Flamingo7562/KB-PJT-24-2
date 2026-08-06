/**
 * 알바생 마이페이지 프로필 카드 렌더링 계약 테스트.
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

import { deleteMe, getBadge, getMe } from '@/services/users'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
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

/** Teleport 를 stub 해 탈퇴 Modal 내용을 wrapper 안에서 찾을 수 있게 한다. */
function mountView() {
  return mount(WorkerMyPageView, { global: { stubs: { teleport: true } } })
}

function findByText(wrapper, selector, text) {
  const found = wrapper.findAll(selector).find((el) => el.text().trim() === text)
  if (!found) throw new Error(`'${text}' ${selector} 를 찾지 못했습니다`)
  return found
}

describe('WorkerMyPageView', () => {
  let logout
  let toastSpy

  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockClear()
    getMe.mockReset()
    getBadge.mockReset()
    deleteMe.mockReset()
    logout = vi.spyOn(useAuthStore(), 'logout').mockResolvedValue()
    toastSpy = vi.spyOn(useUiStore(), 'toast')
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

  it('로그아웃 요청 중에는 버튼이 비활성화되고 완료되면 다시 활성화된다', async () => {
    let resolveLogout
    logout.mockReturnValue(
      new Promise((resolve) => {
        resolveLogout = resolve
      })
    )
    const wrapper = mountView()
    await flushPromises()

    await findByText(wrapper, 'button', '로그아웃').trigger('click')
    await flushPromises()

    expect(findByText(wrapper, 'button', '로그아웃').attributes('disabled')).toBeDefined()

    resolveLogout()
    await flushPromises()

    expect(findByText(wrapper, 'button', '로그아웃').attributes('disabled')).toBeUndefined()
  })

  it('로그아웃이 실패해도 버튼은 다시 활성화된다', async () => {
    let rejectLogout
    logout.mockReturnValue(
      new Promise((_resolve, reject) => {
        rejectLogout = reject
      })
    )
    const wrapper = mountView()
    await flushPromises()

    await findByText(wrapper, 'button', '로그아웃').trigger('click')
    await flushPromises()

    expect(findByText(wrapper, 'button', '로그아웃').attributes('disabled')).toBeDefined()

    rejectLogout({ response: { data: { message: '로그아웃에 실패했습니다.' } } })
    await flushPromises()

    expect(findByText(wrapper, 'button', '로그아웃').attributes('disabled')).toBeUndefined()
  })

  describe('회원 탈퇴 오류 귀속', () => {
    async function openModalAndFillPassword(wrapper, password = 'current-pw1') {
      await findByText(wrapper, 'button', '회원 탈퇴').trigger('click')
      await flushPromises()
      await wrapper.find('input[type="password"]').setValue(password)
    }

    it('필드를 지목하지 않은 실패는 비밀번호 오류로 표시하지 않는다', async () => {
      deleteMe.mockRejectedValue({ response: { status: 500, data: {} } })

      const wrapper = mountView()
      await flushPromises()
      await openModalAndFillPassword(wrapper)
      await findByText(wrapper, 'button', '탈퇴하기').trigger('click')
      await flushPromises()

      expect(wrapper.find('.msg.error').exists()).toBe(false)
      expect(toastSpy).toHaveBeenCalledWith('탈퇴 처리 중 오류가 발생했어요.', { type: 'danger' })
    })

    it('서버가 비밀번호 필드를 지목하면 그 필드 아래에 표시한다', async () => {
      const fieldError = { field: 'password', reason: '비밀번호가 올바르지 않습니다.' }
      deleteMe.mockRejectedValue({
        response: { status: 400, data: { fieldErrors: [fieldError] } },
        fieldErrors: [fieldError]
      })

      const wrapper = mountView()
      await flushPromises()
      await openModalAndFillPassword(wrapper)
      await findByText(wrapper, 'button', '탈퇴하기').trigger('click')
      await flushPromises()

      expect(wrapper.find('.msg.error').text()).toBe('비밀번호가 올바르지 않습니다.')
    })

    it('fieldErrors 없이 message 만 있으면 필드가 아니라 폼 레벨 토스트로 보여준다', async () => {
      deleteMe.mockRejectedValue({
        response: { status: 409, data: { message: '진행 중인 근무가 있어 탈퇴할 수 없어요.' } }
      })

      const wrapper = mountView()
      await flushPromises()
      await openModalAndFillPassword(wrapper)
      await findByText(wrapper, 'button', '탈퇴하기').trigger('click')
      await flushPromises()

      expect(wrapper.find('.msg.error').exists()).toBe(false)
      expect(toastSpy).toHaveBeenCalledWith('진행 중인 근무가 있어 탈퇴할 수 없어요.', {
        type: 'danger'
      })
    })
  })
})
