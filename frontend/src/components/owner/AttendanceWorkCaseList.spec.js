import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import AttendanceWorkCaseList from '@/components/owner/AttendanceWorkCaseList.vue'

// 근무 시각은 Asia/Seoul 벽시계가 유일한 의미다. UTC 자정 = KST 09:00 이므로 아래 Fixture 는
// 09:00~15:00 근무다. 실행 머신의 TZ 와 무관하게 같은 결과가 나와야 한다.
const DRAFT_WORK_CASE = {
  workCaseId: 1,
  title: '평일 주방 보조',
  workDate: '2026-07-23',
  startsAt: '2026-07-23T00:00:00Z',
  endsAt: '2026-07-23T06:00:00Z',
  status: 'DRAFT',
  worker: null
}

function mountList(workCase) {
  return mount(AttendanceWorkCaseList, {
    props: { workCases: [workCase] }
  })
}

describe('AttendanceWorkCaseList invitation issuance', () => {
  beforeEach(() => {
    // 서버는 "근무 시작 전"까지 발급을 허용한다. 시간이 흘러도 결과가 바뀌지 않도록 고정한다.
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-22T00:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // WorkCaseListItemResponse 에는 capability 필드가 없다. 서버(InvitationIssueServiceImpl)가
  // DRAFT · 미매칭 · 시작 전 세 조건을 모두 보므로 버튼 노출도 같은 조건을 따라야 한다.
  it('shows and emits the issuance action while all three server conditions hold', async () => {
    const wrapper = mountList(DRAFT_WORK_CASE)

    await wrapper.get('.copy-btn').trigger('click')

    expect(wrapper.emitted('copy-invite')).toEqual([[DRAFT_WORK_CASE.workCaseId]])
  })

  it('hides the issuance action once the work case is no longer DRAFT', () => {
    const wrapper = mountList({ ...DRAFT_WORK_CASE, status: 'ACCEPTED' })

    expect(wrapper.find('.copy-btn').exists()).toBe(false)
  })

  it('hides the issuance action when a worker is already matched', () => {
    const wrapper = mountList({
      ...DRAFT_WORK_CASE,
      worker: { workerId: 9, name: '이알바' }
    })

    expect(wrapper.find('.copy-btn').exists()).toBe(false)
  })

  // DRAFT 만 보고 노출하면 여기서 항상 409 WORK_CASE_LOCKED 가 되는 버튼이 보인다.
  it('hides the issuance action after the work case start time has passed', () => {
    vi.setSystemTime(new Date('2026-07-23T05:00:00Z'))

    const wrapper = mountList(DRAFT_WORK_CASE)

    expect(wrapper.find('.copy-btn').exists()).toBe(false)
  })
})

describe('AttendanceWorkCaseList item content', () => {
  it('renders the matched worker name and the workplace-local time range', () => {
    const wrapper = mountList({
      ...DRAFT_WORK_CASE,
      status: 'ACCEPTED',
      worker: { workerId: 9, name: '이알바' }
    })

    expect(wrapper.text()).toContain('이알바')
    expect(wrapper.text()).toContain('09:00 ~ 15:00')
  })

  it('shows a placeholder when no worker is matched yet', () => {
    const wrapper = mountList(DRAFT_WORK_CASE)

    expect(wrapper.text()).toContain('아직 매칭된 알바생이 없어요')
  })
})
