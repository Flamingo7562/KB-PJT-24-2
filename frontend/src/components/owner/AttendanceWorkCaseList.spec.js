import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AttendanceWorkCaseList from '@/components/owner/AttendanceWorkCaseList.vue'

const DRAFT_WORK_CASE = {
  workCaseId: 1,
  title: '평일 주방 보조',
  workDate: '2026-07-23',
  startTime: '09:00',
  endTime: '15:00',
  status: 'DRAFT',
  workerName: null
}

function mountList(canIssueInvitation) {
  return mount(AttendanceWorkCaseList, {
    props: {
      workCases: [{ ...DRAFT_WORK_CASE, canIssueInvitation }]
    }
  })
}

describe('AttendanceWorkCaseList invitation capability', () => {
  it('shows and emits the issuance action when the server allows it', async () => {
    const wrapper = mountList(true)

    await wrapper.get('.copy-btn').trigger('click')

    expect(wrapper.emitted('copy-invite')).toEqual([[DRAFT_WORK_CASE.workCaseId]])
  })

  it('does not infer issuance permission from DRAFT status', () => {
    const wrapper = mountList(false)

    expect(wrapper.find('.copy-btn').exists()).toBe(false)
  })
})
