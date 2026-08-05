/**
 * 사업장 Store 계약 테스트.
 * 목록에는 ACTIVE 와 INACTIVE 가 함께 오지만(API_SPEC.md:363), 전역 작업 Context 로
 * 선택할 수 있는 것은 ACTIVE 뿐이다(API_SPEC.md:366). 자동선택이 INACTIVE 를 집으면
 * 이후 모든 지점 기준 조회가 잘못된 사업장으로 나간다.
 */
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/services/workplaces', () => ({ listWorkplaces: vi.fn() }))

import { listWorkplaces } from '@/services/workplaces'
import { useWorkplaceStore } from '@/stores/workplace'

function envelope(content) {
  return {
    content,
    page: { number: 0, size: 100, totalElements: content.length, totalPages: 1 }
  }
}

const INACTIVE_FIRST = [
  { workplaceId: 1, name: '폐점한 강남점', status: 'INACTIVE' },
  { workplaceId: 2, name: '홍대점', status: 'ACTIVE' }
]

describe('workplace store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    listWorkplaces.mockReset()
  })

  it('Page Envelope 의 content 를 목록으로 쓴다', async () => {
    listWorkplaces.mockResolvedValue(envelope(INACTIVE_FIRST))
    const store = useWorkplaceStore()

    await store.load()

    expect(store.workplaces).toHaveLength(2)
  })

  it('자동선택은 INACTIVE 를 건너뛰고 첫 ACTIVE 를 고른다', async () => {
    listWorkplaces.mockResolvedValue(envelope(INACTIVE_FIRST))
    const store = useWorkplaceStore()

    await store.load()

    expect(store.selectedId).toBe(2)
  })

  it('ACTIVE 가 없으면 아무것도 선택하지 않는다', async () => {
    listWorkplaces.mockResolvedValue(
      envelope([{ workplaceId: 1, name: '폐점한 강남점', status: 'INACTIVE' }])
    )
    const store = useWorkplaceStore()

    await store.load()

    expect(store.selectedId).toBeNull()
    expect(store.hasActiveWorkplace).toBe(false)
    expect(store.hasWorkplace).toBe(true)
  })

  it('activeWorkplaces 는 ACTIVE 만 담는다', async () => {
    listWorkplaces.mockResolvedValue(envelope(INACTIVE_FIRST))
    const store = useWorkplaceStore()

    await store.load()

    expect(store.activeWorkplaces.map((w) => w.workplaceId)).toEqual([2])
  })

  it('reset 은 목록·선택·로드 상태를 모두 비운다', async () => {
    listWorkplaces.mockResolvedValue(envelope(INACTIVE_FIRST))
    const store = useWorkplaceStore()
    await store.load()

    store.reset()

    expect(store.workplaces).toEqual([])
    expect(store.selectedId).toBeNull()
    expect(store.loaded).toBe(false)
  })
})
