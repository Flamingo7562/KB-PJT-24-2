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

const TWO_ACTIVE = [
  { workplaceId: 1, name: '홍대점', status: 'ACTIVE' },
  { workplaceId: 2, name: '신촌점', status: 'ACTIVE' }
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

  it('선택값이 더 이상 ACTIVE 를 가리키지 않으면 첫 ACTIVE 로 교체한다', async () => {
    // 이전 세션 등에서 남은 stale 선택값(INACTIVE 가 된 사업장)을 흉내낸다.
    listWorkplaces.mockResolvedValue(envelope(INACTIVE_FIRST))
    const store = useWorkplaceStore()
    store.selectedId = 1 // INACTIVE 인 1번을 가리키고 있던 상태

    await store.load()

    expect(store.selectedId).toBe(2)
  })

  it('ACTIVE 를 가리키는 선택값은 force 재조회에도 그대로 유지된다', async () => {
    listWorkplaces.mockResolvedValue(envelope(TWO_ACTIVE))
    const store = useWorkplaceStore()
    await store.load()
    // 자동선택은 첫 ACTIVE(1번)를 고른다 — 사용자가 두 번째 지점을 명시적으로 선택한다.
    store.select(2)

    await store.load({ force: true })

    // 매 load 마다 첫 ACTIVE 로 되돌리는 구현이었다면 여기서 1로 밀려나 실패한다.
    expect(store.selectedId).toBe(2)
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
