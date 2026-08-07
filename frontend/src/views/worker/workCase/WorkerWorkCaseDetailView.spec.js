import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import WorkerWorkCaseDetailView from '@/views/worker/workCase/WorkerWorkCaseDetailView.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { workCaseId: '42' } }),
  useRouter: () => ({ push: vi.fn() })
}))
vi.mock('@/services/workCases', () => ({ getOwnerContact: vi.fn(), getWorkCase: vi.fn() }))

import { getWorkCase } from '@/services/workCases'

describe('WorkerWorkCaseDetailView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    getWorkCase.mockReset().mockResolvedValue({
      workCaseId: 42,
      title: '주말 홀 서빙',
      workplaceName: '강남점',
      workDate: '2026-08-20',
      startsAt: '2026-08-20T01:00:00Z',
      endsAt: '2026-08-20T09:00:00Z',
      breakMinutes: 60,
      breakPaid: false,
      dailyWage: 120000,
      status: 'ACCEPTED',
      contract: { documentId: 99, sourceTermsVersion: 3 },
      settlement: { status: 'WAITING' }
    })
  })

  it('실제 상세 DTO의 KST 시각과 같은 계약 최종본 URL을 표시한다', async () => {
    const wrapper = mount(WorkerWorkCaseDetailView)
    await flushPromises()

    expect(wrapper.text()).toContain('10:00 ~ 18:00')
    expect(wrapper.text()).toContain('정산대기')
    expect(wrapper.get('.contract-link').attributes('href')).toBe(
      '/api/documents/99/file?mode=view'
    )
  })
})
