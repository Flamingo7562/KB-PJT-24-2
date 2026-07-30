import { describe, expect, it } from 'vitest'

import {
  WORK_CASE_STATUS,
  WORK_CASE_SUMMARY,
  emptyWorkCaseSummary
} from '@/constants/workCaseStatus'
import { getWorkCaseSummary, listWorkCases } from '@/services/workCases'

const PERSISTED_WORK_CASE_STATUSES = [
  'DRAFT',
  'ACCEPTED',
  'READY',
  'IN_PROGRESS',
  'COMPLETED',
  'NO_SHOW',
  'CANCELED'
]

describe('work-case status contract', () => {
  it('maps every persisted status without exposing INVITED', () => {
    expect(Object.keys(WORK_CASE_STATUS)).toEqual(PERSISTED_WORK_CASE_STATUSES)
    expect(WORK_CASE_STATUS).not.toHaveProperty('INVITED')
  })

  it('uses ACCEPTED instead of INVITED in the attendance summary', () => {
    expect(WORK_CASE_SUMMARY).toContainEqual({ key: 'accepted', status: 'ACCEPTED' })
    expect(WORK_CASE_SUMMARY).not.toContainEqual({ key: 'invited', status: 'INVITED' })
    expect(emptyWorkCaseSummary()).toEqual({
      draft: 0,
      accepted: 0,
      ready: 0,
      inProgress: 0,
      completed: 0,
      noShow: 0
    })
  })

  it('keeps mock list and summary responses free of INVITED', async () => {
    const [{ content }, summary] = await Promise.all([listWorkCases(1), getWorkCaseSummary(1)])

    expect(content.map(({ status }) => status)).not.toContain('INVITED')
    expect(summary).toHaveProperty('accepted')
    expect(summary).not.toHaveProperty('invited')
  })

  it('separates invitation issuance capability from DRAFT status', async () => {
    const { content } = await listWorkCases(1)
    const issuableDraft = content.find(({ workCaseId }) => workCaseId === 102)
    const draftWithActiveInvitation = content.find(({ workCaseId }) => workCaseId === 104)

    expect(issuableDraft).toMatchObject({
      status: 'DRAFT',
      canIssueInvitation: true
    })
    expect(draftWithActiveInvitation).toMatchObject({
      status: 'DRAFT',
      canIssueInvitation: false
    })
  })
})
