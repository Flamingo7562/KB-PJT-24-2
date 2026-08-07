import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import OwnerWorkCaseNewView from '@/views/owner/workCase/OwnerWorkCaseNewView.vue'
import { useWorkplaceStore } from '@/stores/workplace'

const push = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))

vi.mock('@/services/workCases', () => ({ createWorkCase: vi.fn() }))
vi.mock('@/services/workplaces', () => ({ listWorkplaces: vi.fn() }))

import { createWorkCase } from '@/services/workCases'
import { listWorkplaces } from '@/services/workplaces'

function fillForm(wrapper) {
  const [title, workDate, startTime, endTime, breakMinutes, dailyWage] = [
    wrapper.find('input[type="text"]'),
    wrapper.find('input[type="date"]'),
    wrapper.find('input[type="time"]'),
    wrapper.findAll('input[type="time"]')[1],
    wrapper.find('input[placeholder="0"]'),
    wrapper.find('input[placeholder="원 단위로 입력"]')
  ]
  return { title, workDate, startTime, endTime, breakMinutes, dailyWage }
}

describe('OwnerWorkCaseNewView', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    push.mockClear()
    createWorkCase.mockReset()
    listWorkplaces.mockReset().mockResolvedValue({
      content: [{ workplaceId: 7, name: '강남점', status: 'ACTIVE' }]
    })
    const workplaceStore = useWorkplaceStore()
    await workplaceStore.load()
  })

  it('승인된 7개 필드만 보내고 성공하면 근태관리로 돌아간다', async () => {
    createWorkCase.mockResolvedValue({ workCaseId: 1 })
    const wrapper = mount(OwnerWorkCaseNewView)
    await flushPromises()

    const f = fillForm(wrapper)
    await f.title.setValue('주말 홀 서빙')
    await f.workDate.setValue('2026-08-10')
    await f.startTime.setValue('09:00')
    await f.endTime.setValue('18:00')
    await f.breakMinutes.setValue('60')
    await f.dailyWage.setValue('90000')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(createWorkCase).toHaveBeenCalledWith(7, {
      title: '주말 홀 서빙',
      workDate: '2026-08-10',
      startTime: '09:00',
      endTime: '18:00',
      breakMinutes: 60,
      breakPaid: false,
      dailyWage: 90000
    })
    expect(push).toHaveBeenCalledWith('/owner/attendance')
  })

  it('서버 fieldErrors를 같은 이름의 폼 필드 오류로 표시한다', async () => {
    createWorkCase.mockRejectedValue({
      response: {
        data: {
          fieldErrors: [{ field: 'dailyWage', reason: '일급은 1원 이상이어야 합니다.' }]
        }
      }
    })
    const wrapper = mount(OwnerWorkCaseNewView)
    await flushPromises()

    const f = fillForm(wrapper)
    await f.title.setValue('주말 홀 서빙')
    await f.workDate.setValue('2026-08-10')
    await f.startTime.setValue('09:00')
    await f.endTime.setValue('18:00')
    await f.dailyWage.setValue('90000')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('일급은 1원 이상이어야 합니다.')
  })
})
