import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import WorkerWithdrawView from '@/views/worker/wallet/WorkerWithdrawView.vue'

const back = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ back }) }))

vi.mock('@/services/wallet', () => ({ withdrawWallet: vi.fn() }))
vi.mock('@/services/worker', () => ({ getWorkerHome: vi.fn() }))

import { withdrawWallet } from '@/services/wallet'
import { getWorkerHome } from '@/services/worker'

describe('WorkerWithdrawView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    back.mockClear()
    withdrawWallet.mockReset()
    withdrawWallet.mockResolvedValue({ availableBalance: 220000, txId: 1 })
    getWorkerHome.mockResolvedValue({
      wallet: { availableBalance: 320000 },
      todayWorkCase: { status: 'NONE' },
      earning: null
    })
  })

  it('가용 잔액을 초과하면 출금 버튼이 비활성화된다', async () => {
    const wrapper = mount(WorkerWithdrawView)
    await flushPromises() // 안심지갑 잔액 320,000 로드

    const submit = () => wrapper.find('button.submit')
    expect(submit().attributes('disabled')).toBeDefined()

    await wrapper.find('button.bank').trigger('click') // 은행 선택(첫 은행)
    // 입력 필드: [0] 계좌번호, [1] 금액
    const [accountInput, amountInput] = wrapper.findAll('input')
    await accountInput.setValue('110222333') // 계좌번호(8자리 이상)

    await amountInput.setValue('500000') // 잔액 초과
    expect(submit().attributes('disabled')).toBeDefined()

    await amountInput.setValue('100000') // 잔액 이내
    expect(submit().attributes('disabled')).toBeUndefined()
  })

  it('유효한 입력이면 확인 모달에서 출금을 요청하고 뒤로 이동한다', async () => {
    // 확인 모달(BaseModal)은 Teleport 로 body 에 렌더되므로 stub 으로 인라인 렌더한다.
    const wrapper = mount(WorkerWithdrawView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    await wrapper.find('button.bank').trigger('click')
    const [accountInput, amountInput] = wrapper.findAll('input')
    await accountInput.setValue('110222333')
    await amountInput.setValue('100000')

    // 출금하기 → 아직 API 호출 없이 확인 모달만 연다.
    await wrapper.find('button.submit').trigger('click')
    await flushPromises()
    expect(withdrawWallet).not.toHaveBeenCalled()

    // 모달의 '출금하기'(두 번째 modal-btn) 확인 → 실제 출금 요청.
    const confirmButtons = wrapper.findAll('button.modal-btn')
    await confirmButtons[confirmButtons.length - 1].trigger('click')
    await flushPromises()

    expect(withdrawWallet).toHaveBeenCalledWith(
      expect.objectContaining({ bankCode: 'KB', accountNo: '110222333', amount: 100000 })
    )
    expect(back).toHaveBeenCalled()
  })
})
