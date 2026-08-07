import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import BankSelect from '@/components/wallet/BankSelect.vue'
import { BANKS, findBank } from '@/utils/constants'

describe('BankSelect', () => {
  it('승인된 숫자 3자리 은행 코드 다섯 개만 노출한다', () => {
    const codes = BANKS.map((bank) => bank.code)

    expect(codes).toEqual(['004', '088', '020', '081', '011'])
    expect(new Set(codes).size).toBe(codes.length)
    expect(codes.every((code) => /^\d{3}$/.test(code))).toBe(true)
    expect(findBank('KB')).toBeNull()
  })

  it('은행명 대신 canonical bankCode를 v-model 값으로 내보낸다', async () => {
    const wrapper = mount(BankSelect)

    await wrapper.find('button.bank').trigger('click')

    expect(wrapper.emitted('update:modelValue')).toEqual([['004']])
  })
})
