import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { RouterLinkStub } from '@vue/test-utils'

import OnboardingView from '@/views/OnboardingView.vue'

// 역할 선택 단계(③)는 ?step=auth 진입을 가정해 소개 슬라이드(①②)를 건너뛴다.
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, useRoute: () => ({ query: { step: 'auth' } }) }
})

describe('OnboardingView', () => {
  const factory = () =>
    mount(OnboardingView, {
      global: { stubs: { RouterLink: RouterLinkStub } }
    })

  it('역할 토글과 로그인·회원가입 진입을 제공한다', () => {
    const wrapper = factory()
    // 역할 토글 2개(사장님/알바생) + CTA 링크 2개(로그인/회원가입)
    expect(wrapper.findAll('.role')).toHaveLength(2)
    expect(wrapper.findAllComponents(RouterLinkStub)).toHaveLength(2)
  })

  it('역할 토글 시 CTA 경로가 역할별로 바뀐다', async () => {
    const wrapper = factory()
    // 기본: 사장
    let links = wrapper.findAllComponents(RouterLinkStub)
    expect(links[0].props('to')).toBe('/owner/login')
    expect(links[1].props('to')).toBe('/owner/signup')

    // 알바생으로 토글
    await wrapper.findAll('.role')[1].trigger('click')
    links = wrapper.findAllComponents(RouterLinkStub)
    expect(links[0].props('to')).toBe('/worker/login')
    expect(links[1].props('to')).toBe('/worker/signup')
  })
})
