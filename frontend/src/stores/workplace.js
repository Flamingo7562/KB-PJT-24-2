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
  const hasWorkplace = computed(() => workplaces.value.length > 0)

  /** 사업장 목록 로드(최초 1회). 선택값이 없으면 첫 지점 선택 */
  async function load({ force = false } = {}) {
    if (loaded.value && !force) return
    workplaces.value = await listWorkplaces()
    loaded.value = true
    if (selectedId.value == null && workplaces.value.length > 0) {
      selectedId.value = workplaces.value[0].workplaceId
    }
  }

  /** 작업 지점 선택 */
  function select(workplaceId) {
    selectedId.value = workplaceId
  }

  return { workplaces, selectedId, loaded, selected, hasWorkplace, load, select }
})
