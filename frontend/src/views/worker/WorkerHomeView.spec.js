import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import WorkerHomeView from '@/views/worker/WorkerHomeView.vue'

const push = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))

vi.mock('@/services/worker', () => ({ getWorkerHome: vi.fn() }))

import { getWorkerHome } from '@/services/worker'

const homePayload = {
  wallet: { balance: 320000 },
  todayShift: {
    status: 'LATE',
    title: '주말 홀 서빙',
    workplaceName: '카페 봄',
    startTime: '10:00',
    endTime: '18:00'
  },
  earning: {
    dailyWage: 90000,
    totalMinutes: 480,
    unpaidBreakMinutes: 60,
    lateMinutes: 15,
    lateDeduction: 3214,
    accruedAmount: 34526,
    progressRatio: 0.42,
    expectedNetAmount: 83912
  }
}

describe('WorkerHomeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockClear()
    getWorkerHome.mockResolvedValue(structuredClone(homePayload))
  })

  it('안심지갑 잔액·오늘의 알바·확보 안심금액을 표시한다', async () => {
    const wrapper = mount(WorkerHomeView)
    await flushPromises()

    expect(wrapper.text()).toContain('320,000원') // 안심지갑 잔액
    expect(wrapper.text()).toContain('주말 홀 서빙') // 오늘의 알바
    expect(wrapper.text()).toContain('확보 안심금액') // 서버 earning 카드
  })

  it('오늘 근무가 없으면 확보 안심금액 카드를 숨긴다', async () => {
    getWorkerHome.mockResolvedValue({
      wallet: { balance: 0 },
      todayShift: { status: 'NONE' },
      earning: null
    })
    const wrapper = mount(WorkerHomeView)
    await flushPromises()

    expect(wrapper.text()).not.toContain('확보 안심금액')
    expect(wrapper.text()).toContain('오늘은 예정된 알바가 없어요.')
  })

  it('출금 버튼은 출금 화면으로 이동한다', async () => {
    const wrapper = mount(WorkerHomeView)
    await flushPromises()

    await wrapper.get('.wallet-card button').trigger('click')
    expect(push).toHaveBeenCalledWith('/worker/wallet/withdraw')
  })
})
