import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  WORK_CASE_STATUS,
  WORK_CASE_SUMMARY,
  emptyWorkCaseSummary
} from '@/constants/workCaseStatus'

// workCases 서비스는 #158부터 mockFlag(VITE_USE_MOCK) 기반 opt-in이라, Mock 데이터
// 자체를 검증하는 아래 테스트는 mockFlag를 직접 주입해 실행 환경과 무관하게 고정한다.
async function importWithMock() {
  vi.resetModules()
  vi.doMock('@/services/mockFlag', () => ({ USE_MOCK: true }))
  return import('@/services/workCases')
}

// ck_work_cases_status(V202607311429) 가 허용하는 8개. 순서는 그 제약의 나열 순서다.
const PERSISTED_WORK_CASE_STATUSES = [
  'DRAFT',
  'ACCEPTED',
  'READY',
  'IN_PROGRESS',
  'CHECK_OUT_MISSING',
  'COMPLETED',
  'NO_SHOW',
  'CANCELED'
]

describe('work-case status contract', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.doUnmock('@/services/mockFlag')
  })

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
    const { getWorkCaseSummary, listWorkCases } = await importWithMock()
    const [{ content }, summary] = await Promise.all([listWorkCases(1), getWorkCaseSummary(1)])

    expect(content.map(({ status }) => status)).not.toContain('INVITED')
    expect(summary).toHaveProperty('accepted')
    expect(summary).not.toHaveProperty('invited')
  })

  // 실제 API는 목록 Item에 canIssueInvitation을 두지 않는다(AttendanceWorkCaseList가
  // status===DRAFT로 판단). 이 Mock 데이터의 필드는 UI가 더 이상 읽지 않지만, 시나리오
  // 다양성(발급 가능/이미 발급됨 DRAFT)을 남겨 향후 재도입 시 참고할 수 있게 둔다.
  it('keeps distinct DRAFT scenarios in the mock fixture', async () => {
    const { listWorkCases } = await importWithMock()
    const { content } = await listWorkCases(1)
    const issuableDraft = content.find(({ workCaseId }) => workCaseId === 102)
    const draftWithActiveInvitation = content.find(({ workCaseId }) => workCaseId === 104)

    expect(issuableDraft).toMatchObject({ status: 'DRAFT' })
    expect(draftWithActiveInvitation).toMatchObject({ status: 'DRAFT' })
  })
})
