import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import StatusChip from '@/components/common/StatusChip.vue'
import TrustBadge from '@/components/common/TrustBadge.vue'

/**
 * 공통 UI 키트 스모크 테스트 — 각 컴포넌트가 컴파일·마운트되는지 확인한다.
 * (화면 담당이 신뢰하고 조립할 수 있도록 공용 기반을 그린 상태로 유지)
 */
describe('공통 UI 키트 스모크', () => {
  it('BaseButton — 슬롯 라벨과 variant', () => {
    const w = mount(BaseButton, { props: { variant: 'owner' }, slots: { default: '충전' } })
    expect(w.text()).toContain('충전')
    expect(w.classes()).toContain('btn--owner')
  })

  it('AppField — v-model 입력 이벤트', async () => {
    const w = mount(AppField, { props: { label: '이메일', modelValue: '' } })
    await w.get('input').setValue('a@b.com')
    expect(w.emitted('update:modelValue')?.[0]).toEqual(['a@b.com'])
  })

  it('StatusChip — 상태 라벨 표기', () => {
    const w = mount(StatusChip, { props: { status: 'DRAFT', kind: 'workCase' } })
    expect(w.text()).toContain('작성중')
  })

  it('TrustBadge — 등급 뱃지 렌더', () => {
    const w = mount(TrustBadge, { props: { role: 'worker', level: 2 } })
    expect(w.find('img').exists()).toBe(true)
  })

  it('TrustBadge — level 0 은 이력 쌓는 중', () => {
    const w = mount(TrustBadge, { props: { role: 'owner', level: 0, showRemaining: true } })
    expect(w.text()).toContain('이력 쌓는 중')
  })

  it('BaseModal — 닫힘 상태에서 마운트', () => {
    const w = mount(BaseModal, { props: { open: false, title: '확인' } })
    expect(w.exists()).toBe(true)
  })
})
