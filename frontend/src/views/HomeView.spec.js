import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import HomeView from '@/views/HomeView.vue'

describe('HomeView', () => {
  it('프로젝트 주제와 주요 기능을 표시한다', () => {
    const wrapper = mount(HomeView)

    expect(wrapper.get('h1').text()).toContain('Gig Hub')
    expect(wrapper.findAll('article')).toHaveLength(3)
  })
})
