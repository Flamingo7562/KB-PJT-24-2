import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import OwnerChargeView from '@/views/owner/wallet/OwnerChargeView.vue'

const back = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ back }) }))

vi.mock('@/services/wallet', () => ({
  chargeWallet: vi.fn(),
  fetchTransactions: vi.fn(),
  fetchWallet: vi.fn()
}))

import { chargeWallet, fetchWallet } from '@/services/wallet'

describe('OwnerChargeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    back.mockClear()
    chargeWallet.mockReset().mockResolvedValue({
      fundingOrderId: 10,
      status: 'COMPLETED',
      bankTransactionId: 20
    })
    fetchWallet.mockReset().mockResolvedValue({
      currency: 'KRW',
      availableBalance: 1_350_000,
      lockedBalance: 480_000
    })
  })

  it('canonical 은행 코드와 정규화 계좌번호만 Mock 충전 경계에 전달한다', async () => {
    const wrapper = mount(OwnerChargeView)

    await wrapper.find('button.bank').trigger('click')
    const [accountInput, amountInput] = wrapper.findAll('input')
    await accountInput.setValue('170-0000-00001')
    await amountInput.setValue('100000')
    await wrapper.find('button.submit').trigger('click')
    await flushPromises()

    const payload = chargeWallet.mock.calls[0][0]
    expect(payload).toEqual({ bankCode: '004', accountNo: '170000000001', amount: 100000 })
    expect(payload).not.toHaveProperty('bankAccountId')
    expect(payload).not.toHaveProperty('walletId')
    expect(payload).not.toHaveProperty('userId')
    expect(fetchWallet).toHaveBeenCalledOnce()
    expect(back).toHaveBeenCalled()
  })

  it('1억원을 초과한 금액은 제출하지 못한다', async () => {
    const wrapper = mount(OwnerChargeView)

    await wrapper.find('button.bank').trigger('click')
    const [accountInput, amountInput] = wrapper.findAll('input')
    await accountInput.setValue('170000000001')
    await amountInput.setValue('100000001')

    expect(wrapper.find('button.submit').attributes('disabled')).toBeDefined()
  })
})
