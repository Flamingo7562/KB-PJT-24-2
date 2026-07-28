<script setup>
/**
 * [C] 사장 근태관리  ·  /owner/attendance  ·  OWNER  (탭 화면)
 * 근태 현황(채용중·근무중) + 근무 리스트(최신순·검색) + '근무 포지션 추가'(→ /owner/attendance/work-cases/new).
 * 지점 컨텍스트: useWorkplaceStore().selectedId 기준.
 * 연계 API: GET /workplaces/{id}/work-cases/summary · GET /workplaces/{id}/work-cases
 *          POST /work-cases/{id}/invitations (매칭전 항목의 연결 링크 복사)
 *   →  @/services/workCases (getWorkCaseSummary, listWorkCases, createInvite)
 * 공통: StatusChip(근무 상태) · EmptyState · 항목 클릭 → /owner/attendance/work-cases/:workCaseId
 */
import { Link2, Plus, Search } from 'lucide-vue-next'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import StatusChip from '@/components/common/StatusChip.vue'
import {
  WORK_CASE_SUMMARY,
  emptyWorkCaseSummary,
  isDraft,
  workCaseStatusColor,
  workCaseStatusLabel
} from '@/constants/workCaseStatus'
import { createInvite, getWorkCaseSummary, listWorkCases } from '@/services/workCases'
import { useUiStore } from '@/stores/ui'
import { useWorkplaceStore } from '@/stores/workplace'
import { copyText } from '@/utils/clipboard'
import { formatDate, formatTimeRange } from '@/utils/format'

const router = useRouter()
const ui = useUiStore()
const workplaceStore = useWorkplaceStore()

const summary = ref(emptyWorkCaseSummary())
const workCases = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref(null) // null(전체) | 8단계 상태 enum 중 하나(요약 카드 선택)

/**
 * 선택 지점 기준으로 요약·리스트를 다시 조회한다.
 * 검색어·상태는 서버 파라미터로만 넘긴다 — 프론트에서 목록을 재계산하지 않는다.
 * 요약(채용중·근무중 건수)은 상태 필터와 무관한 전체 집계라 그대로 둔다.
 */
async function load() {
  const workplaceId = workplaceStore.selectedId
  if (workplaceId == null) return

  loading.value = true
  try {
    const [summaryRes, listRes] = await Promise.all([
      getWorkCaseSummary(workplaceId),
      listWorkCases(workplaceId, {
        keyword: keyword.value.trim() || undefined,
        status: statusFilter.value ?? undefined
      })
    ])
    summary.value = summaryRes
    workCases.value = listRes.content ?? []
  } catch {
    ui.toast('근태 정보를 불러오지 못했어요.', { type: 'danger' })
  } finally {
    loading.value = false
  }
}

// 지점 목록이 준비되면 selectedId 가 채워지고, 그때 watcher 가 조회한다.
onMounted(() => workplaceStore.load())
watch(() => workplaceStore.selectedId, load, { immediate: true })

// 입력할 때마다 다시 조회하되, 매 글자 요청하지 않도록 잠깐 기다린다.
let searchTimer = null
watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(load, 300)
})
onUnmounted(() => clearTimeout(searchTimer))

/** 엔터로 제출하면 대기 없이 바로 조회한다. */
function onSearchSubmit() {
  clearTimeout(searchTimer)
  load()
}

/** 같은 상태를 다시 누르면 필터를 해제한다(전체 보기). */
function toggleStatus(status) {
  statusFilter.value = statusFilter.value === status ? null : status
  load()
}

const isSearching = computed(() => keyword.value.trim() !== '')
const isFiltered = computed(() => isSearching.value || statusFilter.value !== null)

// 상태 라벨·색은 상수 단일 소스만 사용(컴포넌트에 문자열 하드코딩 금지).
const statusLabel = (status) => workCaseStatusLabel(status)
const statusColor = (status) => workCaseStatusColor(status)

const listTitle = computed(() =>
  statusFilter.value ? `${statusLabel(statusFilter.value)} 근무` : '근무 목록'
)

const copyingId = ref(null) // 링크 생성 중인 근무(중복 클릭 방지)

/**
 * 매칭전 근무의 알바생 연결 링크를 만들어 클립보드에 복사한다.
 * 링크는 1회성·유효기간이며, 확정 후에는 서버가 생성을 막는다(docs/rules/api.md).
 */
async function onCopyInvite(workCaseId) {
  copyingId.value = workCaseId
  try {
    const { inviteUrl } = await createInvite(workCaseId)
    if (await copyText(inviteUrl)) {
      ui.toast('연결 링크를 복사했어요.', { type: 'success' })
    } else {
      // 브라우저가 복사를 막은 경우 — 링크를 띄워 직접 복사할 수 있게 한다.
      ui.toast(`복사가 막혔어요. 링크: ${inviteUrl}`, { type: 'warning', duration: 6000 })
    }
  } catch {
    ui.toast('링크를 만들지 못했어요.', { type: 'danger' })
  } finally {
    copyingId.value = null
  }
}

const goDetail = (workCaseId) => router.push(`/owner/attendance/work-cases/${workCaseId}`)
const goNew = () => router.push('/owner/attendance/work-cases/new')
</script>

<template>
  <div class="attendance">
    <!-- 상태별 요약 6종 — 카드를 누르면 해당 상태만, 다시 누르면 전체를 본다 -->
    <section class="summary">
      <button
        v-for="bucket in WORK_CASE_SUMMARY"
        :key="bucket.key"
        type="button"
        class="stat"
        :class="{ active: statusFilter === bucket.status }"
        :aria-pressed="statusFilter === bucket.status"
        @click="toggleStatus(bucket.status)"
      >
        <span class="stat-label">{{ statusLabel(bucket.status) }}</span>
        <strong class="stat-value" :style="{ color: statusColor(bucket.status) }">
          {{ summary[bucket.key] ?? 0 }}
        </strong>
      </button>
    </section>

    <form class="search" @submit.prevent="onSearchSubmit">
      <Search :size="16" class="search-icon" />
      <input
        v-model="keyword"
        class="search-input"
        type="search"
        placeholder="근무 제목·알바생 검색"
        aria-label="근무 검색"
      />
    </form>

    <section class="list-section">
      <h2 class="list-title">{{ listTitle }}</h2>

      <p v-if="loading" class="loading">불러오는 중…</p>

      <EmptyState
        v-else-if="workCases.length === 0 && isFiltered"
        message="조건에 맞는 근무가 없습니다."
      >
        검색어를 바꾸거나 위 카드를 다시 눌러 전체를 확인해보세요.
      </EmptyState>

      <EmptyState v-else-if="workCases.length === 0" message="등록된 근무가 없습니다.">
        아래 버튼으로 첫 근무 포지션을 추가해보세요.
      </EmptyState>

      <ul v-else class="list">
        <li v-for="workCase in workCases" :key="workCase.workCaseId" class="item">
          <button type="button" class="item-btn" @click="goDetail(workCase.workCaseId)">
            <div class="item-head">
              <span class="item-title">{{ workCase.title }}</span>
              <StatusChip :status="workCase.status" kind="workCase" />
            </div>
            <p class="item-when">
              {{ formatDate(workCase.workDate) }} ·
              {{ formatTimeRange(workCase.startTime, workCase.endTime) }}
            </p>
            <p class="item-worker">
              {{ workCase.workerName ?? '아직 매칭된 알바생이 없어요' }}
            </p>
          </button>

          <!-- 작성중(DRAFT)에서만 노출 — 확정 후에는 서버가 링크 생성을 막는다 -->
          <button
            v-if="isDraft(workCase.status)"
            type="button"
            class="copy-btn"
            :disabled="copyingId === workCase.workCaseId"
            @click="onCopyInvite(workCase.workCaseId)"
          >
            <Link2 :size="14" />
            {{ copyingId === workCase.workCaseId ? '링크 만드는 중…' : '연결 링크 복사' }}
          </button>
        </li>
      </ul>
    </section>

    <button type="button" class="fab" @click="goNew">
      <Plus :size="18" />
      근무 포지션 추가
    </button>
  </div>
</template>

<style scoped>
.attendance {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

/* ---- 근태 현황 요약(6종 그리드) ---- */
.summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-sm);
}
.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
/* 선택된 상태 카드 — 지금 어떤 목록을 보고 있는지 표시 */
.stat.active {
  border-color: var(--color-owner);
  background: var(--color-owner-weak);
}
.stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
/* 값 색은 상태색(상수)으로 인라인 바인딩한다 */
.stat-value {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
}

/* ---- 검색 ---- */
.search {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}
.search-icon {
  flex-shrink: 0;
  color: var(--color-text-sub);
}
.search-input {
  flex: 1;
  min-width: 0;
  padding: var(--space-md) 0;
  border: none;
  background: none;
  font-size: var(--text-md);
}
.search-input:focus {
  outline: none;
}

/* ---- 근무 리스트 ---- */
.list-title {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
  /* Bootstrap Reboot 의 ul padding-left 무효화 */
  padding: 0;
}
.item {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.item-btn {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  width: 100%;
  padding: var(--space-md) var(--space-lg);
  text-align: left;
}
/* 카드 하단 액션 — 매칭전 근무의 연결 링크 복사 */
.copy-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  width: 100%;
  padding: var(--space-sm);
  border-top: 1px solid var(--color-border);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-owner);
}
.copy-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.item-title {
  font-size: var(--text-md);
  font-weight: var(--weight-medium);
  color: var(--color-text);
}
.item-when,
.item-worker {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.loading {
  padding: var(--space-xl) 0;
  text-align: center;
  font-size: var(--text-md);
  color: var(--color-text-sub);
}

/* ---- 근무 포지션 추가 ---- */
.fab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  width: 100%;
  padding: var(--space-md);
  background: var(--color-owner);
  color: var(--color-on-primary);
  border-radius: var(--radius-sm);
  font-weight: var(--weight-medium);
}
</style>
