import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import WorkerWorkView from '@/views/worker/WorkerWorkView.vue'

const push = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))

vi.mock('@/services/worker', () => ({ listWorkerShifts: vi.fn() }))
vi.mock('@/services/shifts', () => ({ getOwnerContact: vi.fn() }))

import { getOwnerContact } from '@/services/shifts'
import { listWorkerShifts } from '@/services/worker'

const sampleShift = {
  shiftId: 101,
  workplaceName: '강남점',
  workDate: '2026-07-22',
  time: '10:00 ~ 18:00',
  dailyWage: 90000,
  status: 'IN_PROGRESS',
  settleStatus: 'HOLD'
}

describe('WorkerWorkView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockClear()
    getOwnerContact.mockResolvedValue({ ownerName: '김사장', phone: '010-1234-5678' })
  })

  it('근무 내역을 목록으로 렌더한다', async () => {
    listWorkerShifts.mockResolvedValueOnce({ content: [sampleShift], totalPages: 1 })
    const wrapper = mount(WorkerWorkView)
    await flushPromises()

    expect(wrapper.findAll('.shift')).toHaveLength(1)
    expect(wrapper.text()).toContain('강남점')
    expect(wrapper.text()).toContain('90,000원')
  })

  it('내역이 없으면 빈 상태를 보여준다', async () => {
    listWorkerShifts.mockResolvedValueOnce({ content: [], totalPages: 1 })
    const wrapper = mount(WorkerWorkView)
    await flushPromises()

    expect(wrapper.findAll('.shift')).toHaveLength(0)
    expect(wrapper.text()).toContain('아직 근무 내역이 없어요.')
  })

  it('항목을 누르면 상세로 이동한다', async () => {
    listWorkerShifts.mockResolvedValueOnce({ content: [sampleShift], totalPages: 1 })
    const wrapper = mount(WorkerWorkView)
    await flushPromises()

    await wrapper.find('.shift-main').trigger('click')
    expect(push).toHaveBeenCalledWith('/worker/work/shifts/101')
  })

  it('? 아이콘은 문의 시트를 열고 사장 연락처를 불러온다', async () => {
    listWorkerShifts.mockResolvedValueOnce({ content: [sampleShift], totalPages: 1 })
    const wrapper = mount(WorkerWorkView)
    await flushPromises()

    await wrapper.find('.help-btn').trigger('click')
    await flushPromises()
    expect(getOwnerContact).toHaveBeenCalledWith(101)
    expect(document.body.textContent).toContain('010-1234-5678')
  })
})
