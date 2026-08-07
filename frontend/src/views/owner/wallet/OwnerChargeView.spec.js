import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import OwnerChargeView from '@/views/owner/wallet/OwnerChargeView.vue'

const push = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))

vi.mock('@/services/wallet', () => ({
  chargeWallet: vi.fn(),
  fetchTransactions: vi.fn(),
  fetchWallet: vi.fn()
}))

import { chargeWallet, fetchWallet } from '@/services/wallet'
import { useWalletFundingStore } from '@/stores/walletFunding'

describe('OwnerChargeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockReset().mockResolvedValue()
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

  it('canonical 은행 코드와 정규화 계좌번호를 메모리 초안으로만 전달한다', async () => {
    const wrapper = mount(OwnerChargeView)
    const fundingStore = useWalletFundingStore()

    await wrapper.find('button.bank').trigger('click')
    const [accountInput, amountInput] = wrapper.findAll('input')
    await accountInput.setValue('170-0000-00001')
    await amountInput.setValue('100000')
    await wrapper.find('button.submit').trigger('click')

    expect(fundingStore.draft).toEqual({
      bankCode: '004',
      accountNo: '170000000001',
      amount: 100000
    })
    expect(chargeWallet).not.toHaveBeenCalled()
    expect(fetchWallet).not.toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith({ name: 'owner-charge-confirm' })
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
