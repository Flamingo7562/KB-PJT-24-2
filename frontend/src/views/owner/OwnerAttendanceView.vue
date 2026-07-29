<script setup>
/**
 * [C] 사장 근태관리  ·  /owner/attendance  ·  OWNER  (탭 화면)
 * 근태 현황(채용중·근무중) + 근무 리스트(최신순·검색) + '근무 포지션 추가'(→ /owner/attendance/work-cases/new).
 * 지점 컨텍스트: useWorkplaceStore().selectedId 기준.
 * 연계 API: GET /workplaces/{id}/work-cases/summary · GET /workplaces/{id}/work-cases
 *          POST /work-cases/{id}/invitations (매칭전 항목의 연결 링크 복사)
 *   →  @/services/workCases (getWorkCaseSummary, listWorkCases, createInvite)
 * 공통: StatusChip(근무 상태) · EmptyState · 항목 클릭 → /owner/attendance/work-cases/:workCaseId
 *
 * 보기 방식(목록형 ↔ 캘린더)
 *   - 두 뷰 모두 **같은 조회(listWorkCases)** 결과를 쓴다. 캘린더일 때만 보고 있는 달로
 *     from/to 를 좁혀 요청하고, 결과를 날짜별로 묶어 그린다(@/utils/calendar).
 *   - 상태 필터·검색어는 뷰를 바꿔도 그대로 유지된다(같은 ref 를 공유).
 *   - 마지막으로 고른 뷰는 localStorage 에 남겨 재진입 시 복원한다(@/utils/storage).
 *   - 항목 클릭 이동 경로는 두 뷰가 동일하다(AttendanceWorkCaseList 를 공유).
 */
import { Plus, Search } from 'lucide-vue-next'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import AttendanceCalendar from '@/components/owner/AttendanceCalendar.vue'
import AttendanceViewToggle from '@/components/owner/AttendanceViewToggle.vue'
import AttendanceWorkCaseList from '@/components/owner/AttendanceWorkCaseList.vue'
import {
  WORK_CASE_SUMMARY,
  emptyWorkCaseSummary,
  workCaseStatusColor,
  workCaseStatusLabel
} from '@/constants/workCaseStatus'
import { createInvite, getWorkCaseSummary, listWorkCases } from '@/services/workCases'
import { useUiStore } from '@/stores/ui'
import { useWorkplaceStore } from '@/stores/workplace'
import {
  currentMonthKey,
  formatDateKeyWithWeekday,
  formatMonthLabel,
  monthRange
} from '@/utils/calendar'
import { copyText } from '@/utils/clipboard'
import { readPreference, writePreference } from '@/utils/storage'

const router = useRouter()
const ui = useUiStore()
const workplaceStore = useWorkplaceStore()

const summary = ref(emptyWorkCaseSummary())
const workCases = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref(null) // null(전체) | 8단계 상태 enum 중 하나(요약 카드 선택)

/* ---- 보기 방식(목록형 ↔ 캘린더) ------------------------------------------ */

// 저장 키. 다른 화면의 취향 값과 섞이지 않도록 화면 이름을 접두사로 둔다.
const VIEW_MODE_KEY = 'owner.attendance.viewMode'
const VIEW_MODES = ['list', 'calendar']

// 재진입 시 마지막 모드로 복원한다. 저장값이 이상하면(수동 편집 등) 목록형으로 되돌린다.
const savedViewMode = readPreference(VIEW_MODE_KEY)
const viewMode = ref(VIEW_MODES.includes(savedViewMode) ? savedViewMode : 'list')
const isCalendar = computed(() => viewMode.value === 'calendar')

const monthKey = ref(currentMonthKey()) // 캘린더가 보고 있는 달 'YYYY-MM'
const selectedDate = ref(null) // 캘린더에서 고른 날짜 'YYYY-MM-DD' | null

/**
 * 선택 지점 기준으로 요약·리스트를 다시 조회한다.
 * 검색어·상태는 서버 파라미터로만 넘긴다 — 프론트에서 목록을 재계산하지 않는다.
 * 캘린더 뷰일 때만 보고 있는 달(from~to)로 조회 범위를 좁힌다.
 * 요약(채용중·근무중 건수)은 상태 필터와 무관한 전체 집계라 그대로 둔다.
 */
async function load() {
  const workplaceId = workplaceStore.selectedId
  if (workplaceId == null) return

  // 캘린더는 한 달치만 필요하다. 목록형은 기존대로 기간 제한 없이 최신순 전체를 본다.
  const range = isCalendar.value ? monthRange(monthKey.value) : {}

  loading.value = true
  try {
    const [summaryRes, listRes] = await Promise.all([
      getWorkCaseSummary(workplaceId),
      listWorkCases(workplaceId, {
        keyword: keyword.value.trim() || undefined,
        status: statusFilter.value ?? undefined,
        from: range.from,
        to: range.to
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

// 뷰를 바꾸면 조회 범위(달 제한 유무)가 달라지므로 다시 조회하고, 고른 뷰를 저장한다.
watch(viewMode, (mode) => {
  writePreference(VIEW_MODE_KEY, mode)
  load()
})

// 캘린더에서 달을 옮기면 그 달을 다시 조회한다.
watch(monthKey, load)

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

/**
 * 캘린더에서 고른 날짜의 근무 — 조회 결과를 그대로 거른 것이라 목록형과 같은 데이터다.
 * 하루 안에서는 시작 시간 순으로 보여준다(그날의 흐름대로 읽히게).
 */
const selectedDayWorkCases = computed(() => {
  if (!selectedDate.value) return []
  return workCases.value
    .filter((workCase) => workCase.workDate === selectedDate.value)
    .sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)))
})

/** 선택한 날짜 제목: "2026.07.22 (수) · 2건" */
const selectedDayTitle = computed(() =>
  selectedDate.value
    ? `${formatDateKeyWithWeekday(selectedDate.value)} · ${selectedDayWorkCases.value.length}건`
    : ''
)

const monthLabel = computed(() => formatMonthLabel(monthKey.value))

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

    <!-- 보기 방식 전환 — 상태 필터·검색어는 그대로 두고 표시 방법만 바꾼다 -->
    <AttendanceViewToggle v-model="viewMode" />

    <p v-if="loading" class="loading">불러오는 중…</p>

    <!-- ① 목록형 뷰 -->
    <section v-else-if="!isCalendar" class="list-section">
      <h2 class="list-title">{{ listTitle }}</h2>

      <EmptyState
        v-if="workCases.length === 0 && isFiltered"
        message="조건에 맞는 근무가 없습니다."
      >
        검색어를 바꾸거나 위 카드를 다시 눌러 전체를 확인해보세요.
      </EmptyState>

      <EmptyState v-else-if="workCases.length === 0" message="등록된 근무가 없습니다.">
        아래 버튼으로 첫 근무 포지션을 추가해보세요.
      </EmptyState>

      <AttendanceWorkCaseList
        v-else
        :work-cases="workCases"
        :copying-id="copyingId"
        @select="goDetail"
        @copy-invite="onCopyInvite"
      />
    </section>

    <!-- ② 캘린더 뷰 — 월 그리드 + 선택한 날짜의 근무 목록 -->
    <section v-else class="calendar-section">
      <AttendanceCalendar
        v-model:month-key="monthKey"
        v-model:selected-date="selectedDate"
        :work-cases="workCases"
      />

      <!-- 그 달에 근무가 아예 없을 때(필터 때문일 수도 있어 문구를 나눈다) -->
      <EmptyState
        v-if="workCases.length === 0 && isFiltered"
        :message="`${monthLabel}에는 조건에 맞는 근무가 없습니다.`"
      >
        검색어를 바꾸거나 위 카드를 다시 눌러 전체를 확인해보세요.
      </EmptyState>

      <EmptyState
        v-else-if="workCases.length === 0"
        :message="`${monthLabel}에는 등록된 근무가 없습니다.`"
      >
        좌우 화살표로 다른 달을 보거나, 아래 버튼으로 근무를 추가해보세요.
      </EmptyState>

      <!-- 날짜를 아직 고르지 않은 상태 — 무엇을 하면 되는지 알려준다 -->
      <EmptyState v-else-if="!selectedDate" message="날짜를 선택해보세요.">
        근무가 있는 날에 색 점이 표시돼요. 날짜를 누르면 그날의 근무를 볼 수 있어요.
      </EmptyState>

      <!-- 고른 날짜의 근무 목록 — 항목 모양·이동 경로는 목록형과 동일하다 -->
      <div v-else class="day-section">
        <h2 class="list-title">{{ selectedDayTitle }}</h2>

        <EmptyState v-if="selectedDayWorkCases.length === 0" message="이 날짜에는 근무가 없습니다.">
          다른 날짜를 선택해보세요.
        </EmptyState>

        <AttendanceWorkCaseList
          v-else
          :work-cases="selectedDayWorkCases"
          :copying-id="copyingId"
          :show-date="false"
          @select="goDetail"
          @copy-invite="onCopyInvite"
        />
      </div>
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
.loading {
  padding: var(--space-xl) 0;
  text-align: center;
  font-size: var(--text-md);
  color: var(--color-text-sub);
}

/* ---- 캘린더 뷰(달력 + 선택한 날짜 목록) ---- */
.calendar-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
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
