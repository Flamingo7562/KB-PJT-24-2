import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  replace: vi.fn(),
  getInvite: vi.fn(),
  confirmInvite: vi.fn(),
  getWorkCase: vi.fn(),
  loadTransactions: vi.fn(),
  toast: vi.fn(),
  newIdempotencyKey: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { token: 'invite-token-159' },
    fullPath: '/invitations/invite-token-159'
  }),
  useRouter: () => ({ replace: mocks.replace })
}))

vi.mock('@/services/invites', () => ({
  getInvite: mocks.getInvite,
  confirmInvite: mocks.confirmInvite
}))
vi.mock('@/services/http', () => ({ newIdempotencyKey: mocks.newIdempotencyKey }))
vi.mock('@/services/workCases', () => ({ getWorkCase: mocks.getWorkCase }))
vi.mock('@/services/documents', () => ({
  serverDocumentFileUrl: (documentId, mode) => `/api/documents/${documentId}/file?mode=${mode}`
}))
vi.mock('@/stores/wallet', () => ({
  useWalletStore: () => ({ loadTransactions: mocks.loadTransactions })
}))
vi.mock('@/stores/ui', () => ({
  useUiStore: () => ({ toast: mocks.toast })
}))

import InviteConfirmView from '@/views/invite/InviteConfirmView.vue'

const invite = {
  title: '주말 홀 서빙',
  workplaceName: '강남점',
  startsAt: '2026-08-20T01:00:00Z',
  endsAt: '2026-08-20T09:00:00Z',
  breakMinutes: 60,
  breakPaid: false,
  dailyWage: 120000,
  termsVersion: 3,
  expiresAt: '2026-08-19T01:00:00Z',
  ownerBadge: { badgeType: 'TRUST_OWNER', level: 2 }
}

const workCase = {
  workCaseId: 159,
  title: invite.title,
  workplaceName: invite.workplaceName,
  startsAt: invite.startsAt,
  endsAt: invite.endsAt,
  contract: { documentId: 5159, sourceTermsVersion: 3 }
}

function mountView() {
  return mount(InviteConfirmView, {
    global: {
      plugins: [createPinia()],
      stubs: { AppBackHeader: true, TrustBadge: true }
    }
  })
}

describe('InviteConfirmView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mocks.getInvite.mockResolvedValue(invite)
    mocks.confirmInvite.mockResolvedValue({ workCaseId: 159, escrowStatus: 'HELD' })
    mocks.getWorkCase.mockResolvedValue(workCase)
    mocks.loadTransactions.mockResolvedValue()
    mocks.newIdempotencyKey.mockReturnValue('accept-intent-159')
  })

  it('전체 조건을 읽기 전용으로 표시하고 Canvas 서명을 만들지 않는다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('주말 홀 서빙')
    expect(wrapper.text()).toContain('120,000원')
    expect(wrapper.text()).toContain('조건 버전')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.find('canvas').exists()).toBe(false)
    expect(wrapper.text()).toContain('서명 이미지나 별도 파일은 전송되지 않습니다.')
  })

  it('동의 후 Body 없는 수락을 호출하고 근무·지갑·계약을 재조회한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('input[type="checkbox"]').setValue(true)
    const acceptButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('동의하고 근무 확정'))
    await acceptButton.trigger('click')
    await flushPromises()

    expect(mocks.confirmInvite).toHaveBeenCalledWith('invite-token-159', {
      idempotencyKey: 'accept-intent-159'
    })
    expect(mocks.getWorkCase).toHaveBeenCalledWith(159)
    expect(mocks.loadTransactions).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('근무가 확정되었습니다')
    expect(wrapper.get('a.contract-link').attributes('href')).toBe(
      '/api/documents/5159/file?mode=view'
    )
  })

  it('서버 결과가 불명확하면 사용자의 재확인에서도 같은 멱등 Key를 유지한다', async () => {
    mocks.confirmInvite
      .mockRejectedValueOnce({ response: { status: 500, data: { code: 'INTERNAL_ERROR' } } })
      .mockResolvedValueOnce({ workCaseId: 159, escrowStatus: 'HELD' })

    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('input[type="checkbox"]').setValue(true)

    let acceptButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('동의하고 근무 확정'))
    await acceptButton.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('같은 요청으로 다시 확인')
    acceptButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('같은 요청으로 다시 확인'))
    await acceptButton.trigger('click')
    await flushPromises()

    expect(mocks.confirmInvite).toHaveBeenNthCalledWith(1, 'invite-token-159', {
      idempotencyKey: 'accept-intent-159'
    })
    expect(mocks.confirmInvite).toHaveBeenNthCalledWith(2, 'invite-token-159', {
      idempotencyKey: 'accept-intent-159'
    })
    expect(mocks.newIdempotencyKey).toHaveBeenCalledTimes(1)
  })

  it('인증이 만료된 조회는 Token 경로를 보존해 WORKER 로그인으로 복귀시킨다', async () => {
    mocks.getInvite.mockRejectedValueOnce({
      response: { status: 401, data: { code: 'AUTH_REQUIRED' } }
    })

    mountView()
    await flushPromises()

    expect(mocks.replace).toHaveBeenCalledWith({
      path: '/worker/login',
      query: { redirect: '/invitations/invite-token-159' }
    })
  })
})
