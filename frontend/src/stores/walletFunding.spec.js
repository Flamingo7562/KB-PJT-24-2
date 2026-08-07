import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { useWalletFundingStore } from '@/stores/walletFunding'

describe('walletFunding store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('PIN과 내부 ID 없이 확인 화면에 필요한 초안만 메모리에 보존한다', () => {
    const store = useWalletFundingStore()

    store.setDraft({ bankCode: '004', accountNo: '170000000001', amount: 100_000 })

    expect(store.draft).toEqual({
      bankCode: '004',
      accountNo: '170000000001',
      amount: 100_000
    })
    expect(Object.keys(store.draft).sort()).toEqual(['accountNo', 'amount', 'bankCode'])
  })

  it('화면 이탈 시 민감 초안을 폐기한다', () => {
    const store = useWalletFundingStore()
    store.setDraft({ bankCode: '004', accountNo: '170000000001', amount: 100_000 })

    store.clearDraft()

    expect(store.draft).toBeNull()
  })
})
