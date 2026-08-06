/**
 * AppField 계약 테스트.
 * digits-only 필드는 한글 IME 조합을 취소하려고 내부적으로 blur/focus 를 호출한다.
 * 그 내부 blur 가 부모로 새어 나가면 전화번호 칸에 한글을 치는 순간 필드가 touched 가
 * 되어 입력 중에 검증이 돈다 — 실시간 검증 계약의 "입력 중 조용함"이 깨진다.
 */
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AppField from '@/components/common/AppField.vue'

describe('AppField blur', () => {
  it('사용자가 필드를 떠나면 blur 를 올린다', async () => {
    const wrapper = mount(AppField, { props: { label: '이메일' } })

    await wrapper.find('input').trigger('blur')

    expect(wrapper.emitted('blur')).toHaveLength(1)
  })

  // jsdom 은 el.blur() 를 실제 document.activeElement 에서만 blur 이벤트로 반영한다.
  // attachTo 없이 mount 만 하면 input 이 activeElement 가 되지 않아 compositionstart 핸들러
  // 내부의 el.blur() 가 아무 이벤트도 못 올리는 조용한 무동작이 된다 — 그러면 이 테스트는
  // 구현을 지워도 항상 통과하는 껍데기가 된다. document.body 에 붙이고 실제로 focus 시켜
  // el.blur() 가 진짜 blur 이벤트를 올리는 조합 취소 구간을 재현한다.
  it('IME 조합 취소로 생기는 내부 blur 는 올리지 않는다', async () => {
    const wrapper = mount(AppField, {
      props: { label: '전화번호', digitsOnly: true },
      attachTo: document.body
    })
    const input = wrapper.find('input')
    input.element.focus()
    expect(document.activeElement).toBe(input.element)

    // 조합 시작 → 컴포넌트가 el.blur(); el.focus() 로 조합을 취소한다.
    await input.trigger('compositionstart')

    expect(wrapper.emitted('blur')).toBeUndefined()
    wrapper.unmount()
  })

  it('조합 취소 뒤에도 진짜 blur 는 다시 올린다', async () => {
    const wrapper = mount(AppField, {
      props: { label: '전화번호', digitsOnly: true },
      attachTo: document.body
    })
    const input = wrapper.find('input')
    input.element.focus()

    await input.trigger('compositionstart')
    await input.trigger('blur')

    expect(wrapper.emitted('blur')).toHaveLength(1)
    wrapper.unmount()
  })
})

describe('AppField success', () => {
  it('success 를 주면 문구를 보여준다', () => {
    const wrapper = mount(AppField, {
      props: { label: '아이디', success: '사용 가능한 아이디입니다' }
    })

    expect(wrapper.text()).toContain('사용 가능한 아이디입니다')
  })

  it('error 가 있으면 success 보다 error 를 보여준다', () => {
    const wrapper = mount(AppField, {
      props: { label: '아이디', success: '사용 가능합니다', error: '이미 사용 중입니다' }
    })

    expect(wrapper.text()).toContain('이미 사용 중입니다')
    expect(wrapper.text()).not.toContain('사용 가능합니다')
  })
})
