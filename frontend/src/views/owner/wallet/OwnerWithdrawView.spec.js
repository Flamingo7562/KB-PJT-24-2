import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import OwnerWithdrawView from '@/views/owner/wallet/OwnerWithdrawView.vue'

const back = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ back }) }))

vi.mock('@/services/wallet', () => ({
  fetchTransactions: vi.fn(),
  fetchWallet: vi.fn(),
  withdrawWallet: vi.fn()
}))

import { fetchWallet, withdrawWallet } from '@/services/wallet'

describe('OwnerWithdrawView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    back.mockClear()
    fetchWallet.mockReset().mockResolvedValue({
      currency: 'KRW',
      availableBalance: 320_000,
      lockedBalance: 0
    })
    withdrawWallet.mockReset().mockResolvedValue({
      withdrawalRequestId: 11,
      status: 'COMPLETED',
      bankTransactionId: 21
    })
  })

  it('확인 뒤 canonical 출금 Body를 보내고 공용 지갑을 재조회한다', async () => {
    const wrapper = mount(OwnerWithdrawView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    await wrapper.find('button.bank').trigger('click')
    const [accountInput, amountInput] = wrapper.findAll('input')
    await accountInput.setValue('170-0000-00001')
    await amountInput.setValue('100000')
    await wrapper.find('button.submit').trigger('click')
    const confirmButtons = wrapper.findAll('button.modal-btn')
    await confirmButtons[confirmButtons.length - 1].trigger('click')
    await flushPromises()

    expect(withdrawWallet).toHaveBeenCalledWith({
      bankCode: '004',
      accountNo: '170000000001',
      amount: 100000
    })
    expect(fetchWallet).toHaveBeenCalledTimes(2)
    expect(back).toHaveBeenCalled()
  })
})
