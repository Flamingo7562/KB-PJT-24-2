import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import OwnerWorkCaseDetailView from '@/views/owner/workCase/OwnerWorkCaseDetailView.vue'

const push = vi.fn()
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { workCaseId: '42' } }),
  useRouter: () => ({ push })
}))

vi.mock('@/services/workCases', () => ({
  getWorkCase: vi.fn(),
  updateWorkCase: vi.fn(),
  deleteWorkCase: vi.fn(),
  createInvite: vi.fn(),
  reissueInvite: vi.fn()
}))
vi.mock('@/utils/clipboard', () => ({ copyText: vi.fn().mockResolvedValue(true) }))

import {
  createInvite,
  deleteWorkCase,
  getWorkCase,
  reissueInvite,
  updateWorkCase
} from '@/services/workCases'
import { useUiStore } from '@/stores/ui'

// startsAt 2026-08-01T00:00:00Z = KST 09:00. 아래 고정 시각(7/22)은 그보다 앞이라 "시작 전"이다.
const DRAFT_DETAIL = {
  workCaseId: 42,
  title: '주말 홀 서빙',
  workDate: '2026-08-01',
  startsAt: '2026-08-01T00:00:00Z',
  endsAt: '2026-08-01T09:00:00Z',
  breakMinutes: 60,
  breakPaid: false,
  dailyWage: 90000,
  status: 'DRAFT',
  termsVersion: 3,
  workplaceName: '강남점',
  worker: null,
  latestInvitation: null,
  contract: null,
  attendance: { checkedInAt: null, checkedOutAt: null },
  escrow: null,
  settlement: null
}

const PENDING_INVITATION = {
  status: 'PENDING',
  termsVersion: 3,
  expiresAt: '2026-08-01T00:00:00Z'
}

function mountView() {
  return mount(OwnerWorkCaseDetailView, { global: { stubs: { teleport: true } } })
}

function toastMessages() {
  return useUiStore().toasts.map((t) => t.message)
}

describe('OwnerWorkCaseDetailView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockClear()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-22T00:00:00Z'))
    getWorkCase.mockReset().mockResolvedValue({ ...DRAFT_DETAIL })
    updateWorkCase.mockReset().mockResolvedValue(undefined)
    deleteWorkCase.mockReset().mockResolvedValue(undefined)
    createInvite.mockReset().mockResolvedValue({
      inviteUrl: 'https://app/invitations/abc',
      expiresAt: '2026-08-01T00:00:00Z'
    })
    reissueInvite.mockReset().mockResolvedValue({
      inviteUrl: 'https://app/invitations/new',
      expiresAt: '2026-08-01T00:00:00Z'
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('근무 조건을 근무지 기준 시각과 조건 버전으로 표시한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('09:00 ~ 18:00')
    expect(wrapper.text()).toContain('v3')
  })

  it('수정 실패의 breakMinutes 서버 오류를 휴게시간 필드에 표시한다', async () => {
    updateWorkCase.mockRejectedValue({
      response: {
        data: {
          fieldErrors: [{ field: 'breakMinutes', reason: '휴게시간은 0분 이상이어야 합니다.' }]
        }
      }
    })
    const wrapper = mountView()
    await flushPromises()

    await wrapper
      .findAll('button')
      .find((button) => button.text() === '수정')
      .trigger('click')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(updateWorkCase).toHaveBeenCalled()
    expect(wrapper.text()).toContain('휴게시간은 0분 이상이어야 합니다.')
  })

  it('초대·계약·예치·근태 근거가 없으면 진행 현황을 감춘다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.progress').exists()).toBe(false)
  })

  it('계약·예치·근태 근거를 진행 현황에 표시한다', async () => {
    getWorkCase.mockResolvedValue({
      ...DRAFT_DETAIL,
      status: 'IN_PROGRESS',
      worker: { workerId: 4, name: '이알바' },
      latestInvitation: { status: 'ACCEPTED', termsVersion: 3, expiresAt: null },
      contract: {
        contractId: 7,
        documentId: 9,
        sourceTermsVersion: 3,
        acceptedAt: '2026-07-25T01:00:00Z'
      },
      escrow: { status: 'HELD', amount: 90000 },
      attendance: { checkedInAt: '2026-08-01T00:05:00Z', checkedOutAt: null },
      settlement: { status: 'WAITING', amount: 90000, dueAt: null, completedAt: null }
    })
    const wrapper = mountView()
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('수락됨')
    expect(text).toContain('2026.07.25 10:00') // 계약 확정(KST)
    expect(text).toContain('예치중')
    expect(text).toContain('09:05') // 출근(KST)
    expect(text).toContain('정산대기') // settlements.status=WAITING 이 한글로 매핑돼야 한다
    expect(text).not.toContain('WAITING')
    expect(wrapper.get('.contract-link').attributes('href')).toBe('/api/documents/9/file?mode=view')
  })

  it('기한이 지난 PENDING 초대는 상태만이 아니라 기한 경과를 함께 알린다', async () => {
    vi.setSystemTime(new Date('2026-08-02T00:00:00Z'))
    getWorkCase.mockResolvedValue({ ...DRAFT_DETAIL, latestInvitation: PENDING_INVITATION })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('기한 지남')
  })

  it('유효한 초대가 있을 때만 새 링크로 교체를 제안한다', async () => {
    getWorkCase.mockResolvedValue({ ...DRAFT_DETAIL, latestInvitation: PENDING_INVITATION })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('새 링크로 교체')
  })

  it('유효한 초대가 없으면 교체 버튼을 감춘다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).not.toContain('새 링크로 교체')
  })

  it('교체를 확인하면 reissue 를 호출하고 이전 링크 무효를 알린다', async () => {
    getWorkCase.mockResolvedValue({ ...DRAFT_DETAIL, latestInvitation: PENDING_INVITATION })
    const wrapper = mountView()
    await flushPromises()

    const openButton = wrapper.findAll('button').find((b) => b.text().includes('새 링크로 교체'))
    await openButton.trigger('click')
    const confirm = wrapper.findAll('button').find((b) => b.text().includes('교체하기'))
    await confirm.trigger('click')
    await flushPromises()

    expect(reissueInvite).toHaveBeenCalledWith(42)
    expect(toastMessages().join(' ')).toContain('이전 링크는 더 이상 쓸 수 없어요')
  })

  it('발급 성공 시 만료 시각을 함께 안내한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    const issueButton = wrapper.findAll('button').find((b) => b.text().includes('연결 링크 발급'))
    await issueButton.trigger('click')
    await flushPromises()

    expect(createInvite).toHaveBeenCalledWith(42)
    expect(toastMessages().join(' ')).toContain('2026.08.01 09:00까지 유효해요')
  })

  it('발급이 WORK_CASE_LOCKED 로 거절되면 사유를 구분해 안내한다', async () => {
    createInvite.mockRejectedValue({ code: 'WORK_CASE_LOCKED' })
    const wrapper = mountView()
    await flushPromises()

    const issueButton = wrapper.findAll('button').find((b) => b.text().includes('연결 링크 발급'))
    await issueButton.trigger('click')
    await flushPromises()

    expect(toastMessages().join(' ')).toContain('시작 시각이 지난 근무는 링크를 발급할 수 없어요')
  })

  it('초대 이력이 없으면 삭제로 안내한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('삭제'))
      .trigger('click')
    const confirm = wrapper.findAll('button.modal-btn, button').filter((b) => b.text() === '삭제')
    await confirm[confirm.length - 1].trigger('click')
    await flushPromises()

    expect(deleteWorkCase).toHaveBeenCalledWith(42)
    expect(toastMessages().join(' ')).toContain('근무를 삭제했어요')
  })

  // 서버는 초대 이력이 있으면 행을 지우지 않고 CANCELED 로 전이한다. 두 경로 모두 204 다.
  it('초대 이력이 있으면 취소 처리로 안내한다', async () => {
    getWorkCase.mockResolvedValue({
      ...DRAFT_DETAIL,
      latestInvitation: { status: 'REVOKED', termsVersion: 3, expiresAt: null }
    })
    const wrapper = mountView()
    await flushPromises()

    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('삭제'))
      .trigger('click')
    const confirm = wrapper.findAll('button').filter((b) => b.text() === '삭제')
    await confirm[confirm.length - 1].trigger('click')
    await flushPromises()

    expect(toastMessages().join(' ')).toContain('취소 처리했어요')
  })
})
