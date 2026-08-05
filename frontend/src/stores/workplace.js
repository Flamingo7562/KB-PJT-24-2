import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { listWorkplaces } from '@/services/workplaces'

/**
 * 사장 지점(사업장) 컨텍스트 — 전역.
 *
 * 네비 지점 select 의 원본이며, 근태관리·문서함·QR 화면이 `selectedId` 를 기준으로
 * 데이터를 조회한다(사장 홈의 지갑 영역은 전 지점 합산이라 이 select 와 무관).
 * 사업장은 사장 전 기능의 전제 컨텍스트다(라우팅 G7).
 */
export const useWorkplaceStore = defineStore('workplace', () => {
  const workplaces = ref([]) // [{ workplaceId, name, address }]
  const selectedId = ref(null)
  const loaded = ref(false)

  const selected = computed(
    () => workplaces.value.find((w) => w.workplaceId === selectedId.value) ?? null
  )

  /** 전역 작업 Context 로 선택할 수 있는 사업장은 ACTIVE 뿐이다(API_SPEC.md:366). */
  const activeWorkplaces = computed(() => workplaces.value.filter((w) => w.status === 'ACTIVE'))

  /** 목록 존재 여부. 사업장 관리 화면은 INACTIVE 도 보여줘야 하므로 전체 기준이다. */
  const hasWorkplace = computed(() => workplaces.value.length > 0)

  /** 지점 기준 기능(QR·근태·문서함)이 동작 가능한지. 선택 가능한 사업장이 있어야 한다. */
  const hasActiveWorkplace = computed(() => activeWorkplaces.value.length > 0)

  /**
   * 사업장 목록 로드(최초 1회, force 시 재조회). 승인 Page Envelope 의 content 를 목록으로 쓴다.
   * 선택값이 없거나 더 이상 ACTIVE 를 가리키지 않으면 첫 ACTIVE 사업장으로 다시 고른다 —
   * INACTIVE 를 선택 상태로 두면 이후 지점 기준 조회(QR·근태·문서함)가 전부 잘못된
   * 사업장으로 나간다. 이미 ACTIVE 를 가리키는 선택값은 그대로 둔다 — 매 refresh 마다
   * 사용자의 명시적 선택을 되돌리면 안 된다.
   */
  async function load({ force = false } = {}) {
    if (loaded.value && !force) return
    const { content } = await listWorkplaces()
    workplaces.value = content
    loaded.value = true
    const selectionIsActive = activeWorkplaces.value.some((w) => w.workplaceId === selectedId.value)
    if (!selectionIsActive) {
      selectedId.value = activeWorkplaces.value[0]?.workplaceId ?? null
    }
  }

  /** 작업 지점 선택 */
  function select(workplaceId) {
    selectedId.value = workplaceId
  }

  /** 로그아웃 시 Context 를 비운다. Pinia setup store 에는 $reset 이 없다. */
  function reset() {
    workplaces.value = []
    selectedId.value = null
    loaded.value = false
  }

  return {
    workplaces,
    selectedId,
    loaded,
    selected,
    activeWorkplaces,
    hasWorkplace,
    hasActiveWorkplace,
    load,
    select,
    reset
  }
})
