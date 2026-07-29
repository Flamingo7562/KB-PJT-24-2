<script setup>
/**
 * 근태 캘린더(월 단위) — 날짜별 근무 건수·상태를 한눈에 보여준다.
 *
 * 데이터는 부모가 목록 뷰와 **동일한 조회(listWorkCases)** 로 가져와 내려준다.
 * 이 컴포넌트는 받은 배열을 날짜별로 묶어 그리기만 하고, 직접 API 를 부르지 않는다.
 *
 * 사용:
 *   <AttendanceCalendar
 *     v-model:month-key="monthKey" v-model:selected-date="selectedDate"
 *     :work-cases="workCases" />
 *
 * 셀 표시 규칙
 *   - 날짜 숫자 + 근무 건수(2건 이상일 때만 숫자 뱃지)
 *   - 상태 점: 그날 근무들의 상태색을 최대 3개까지(중복 제거, @/constants/workCaseStatus 색)
 *   - 오늘은 테두리, 선택한 날짜는 채운 배경으로 구분
 */
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { computed } from 'vue'

import { workCaseStatusColor } from '@/constants/workCaseStatus'

// 캘린더 관련 함수 import
import {
  WEEKDAY_LABELS,
  buildMonthGrid,
  currentMonthKey,
  formatMonthLabel,
  groupByDateKey,
  shiftMonth,
  todayKey
} from '@/utils/calendar'

const props = defineProps({
  /** 보고 있는 달. 'YYYY-MM' */
  monthKey: { type: String, required: true },
  /** 이 달의 근무 목록(서버 조회 결과) */
  workCases: { type: Array, default: () => [] },
  /** 선택한 날짜. 'YYYY-MM-DD' 또는 null(선택 없음) */
  selectedDate: { type: String, default: null }
})

const emit = defineEmits(['update:monthKey', 'update:selectedDate'])

const today = todayKey()

// 한 셀에 찍는 상태 점의 최대 개수. 이보다 많으면 잘라내고 건수 뱃지로 전체 수를 알린다.
const MAX_DOTS = 3

/** 날짜 키 → 그날의 근무 배열. 목록 배열 하나만 다시 묶어 쓴다(추가 조회 없음). */
const byDate = computed(() => groupByDateKey(props.workCases, (w) => w.workDate))

/**
 * 화면에 그릴 셀 목록. 그리드(날짜 뼈대)에 그날의 근무 요약을 붙인다.
 * - count: 근무 건수
 * - dots: 상태색 배열(중복 제거 후 MAX_DOTS 까지)
 */
const cells = computed(() =>
  buildMonthGrid(props.monthKey).map((cell) => {
    const dayWorkCases = byDate.value[cell.dateKey] ?? []
    const statuses = [...new Set(dayWorkCases.map((w) => w.status))]
    return {
      ...cell,
      count: dayWorkCases.length,
      dots: statuses.slice(0, MAX_DOTS).map(workCaseStatusColor)
    }
  })
)

const monthLabel = computed(() => formatMonthLabel(props.monthKey))

/** 이번 달을 보고 있으면 '오늘' 버튼을 숨긴다(누를 이유가 없다). */
const isCurrentMonth = computed(() => props.monthKey === currentMonthKey())

/**
 * 달 이동. 달이 바뀌면 선택 날짜도 지운다 —
 * 보이지 않는 달의 날짜가 선택된 채로 남아 아래 목록만 엉뚱하게 보이는 것을 막는다.
 */
function moveMonth(delta) {
  emit('update:monthKey', shiftMonth(props.monthKey, delta))
  emit('update:selectedDate', null)
}

/** 이번 달 + 오늘 날짜로 한 번에 이동한다. */
function goToday() {
  emit('update:monthKey', currentMonthKey())
  emit('update:selectedDate', today)
}

/** 같은 날짜를 다시 누르면 선택을 해제한다(그 달 전체 보기로 돌아감). */
function selectDate(dateKey) {
  emit('update:selectedDate', props.selectedDate === dateKey ? null : dateKey)
}
</script>

<template>
  <section class="calendar">
    <!-- 월 이동 헤더 -->
    <header class="cal-head">
      <button type="button" class="cal-nav" aria-label="이전 달" @click="moveMonth(-1)">
        <ChevronLeft :size="18" />
      </button>

      <div class="cal-title-box">
        <h2 class="cal-title" aria-live="polite">{{ monthLabel }}</h2>
        <button v-if="!isCurrentMonth" type="button" class="cal-today-btn" @click="goToday">
          오늘
        </button>
      </div>

      <button type="button" class="cal-nav" aria-label="다음 달" @click="moveMonth(1)">
        <ChevronRight :size="18" />
      </button>
    </header>

    <!-- 요일 헤더(일요일 시작). 토·일은 색으로 구분한다. -->
    <div class="cal-weekdays" aria-hidden="true">
      <span
        v-for="(label, index) in WEEKDAY_LABELS"
        :key="label"
        class="cal-weekday"
        :class="{ sun: index === 0, sat: index === 6 }"
      >
        {{ label }}
      </span>
    </div>

    <!-- 날짜 그리드 — 근무가 있는 날만 누를 수 있다 -->
    <div class="cal-grid">
      <button
        v-for="cell in cells"
        :key="cell.dateKey"
        type="button"
        class="cal-cell"
        :class="{
          outside: !cell.inMonth,
          today: cell.dateKey === today,
          selected: cell.dateKey === selectedDate,
          sun: cell.weekday === 0,
          sat: cell.weekday === 6
        }"
        :disabled="cell.count === 0"
        :aria-pressed="cell.dateKey === selectedDate"
        :aria-label="`${cell.day}일, 근무 ${cell.count}건`"
        @click="selectDate(cell.dateKey)"
      >
        <span class="cal-day">{{ cell.day }}</span>

        <!-- 상태색 점 — 그날 어떤 상태의 근무가 섞여 있는지 -->
        <span class="cal-dots">
          <i
            v-for="(color, index) in cell.dots"
            :key="index"
            class="cal-dot"
            :style="{ background: color }"
          />
        </span>

        <!-- 2건 이상일 때만 건수를 숫자로 —  한 건이면 점 하나로 충분하다 -->
        <span v-if="cell.count > 1" class="cal-count">{{ cell.count }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.calendar {
  padding: var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

/* ---- 월 이동 헤더 ---- */
.cal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.cal-nav {
  display: inline-flex;
  padding: var(--space-xs);
  color: var(--color-text-sub);
}
.cal-title-box {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.cal-title {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.cal-today-btn {
  padding: 2px var(--space-sm);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

/* ---- 요일 헤더 ---- */
.cal-weekdays,
.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}
.cal-weekdays {
  margin-top: var(--space-md);
}
.cal-weekday {
  padding-bottom: var(--space-xs);
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

/* ---- 날짜 셀 ---- */
.cal-cell {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  /* 정사각형에 가깝게 — 좁은 모바일 화면에서도 7칸이 무너지지 않는다 */
  min-height: 44px;
  padding: var(--space-xs) 0;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  font-size: var(--text-md);
  color: var(--color-text);
}
/* 근무 없는 날 — 클릭 불가지만 흐릿하게만 두고 숫자는 계속 읽히게 한다 */
.cal-cell:disabled {
  cursor: default;
}
/* 지난달·다음달 날짜 */
.cal-cell.outside .cal-day {
  color: var(--color-border);
}
.cal-cell.sun .cal-day {
  color: var(--color-danger);
}
.cal-cell.sat .cal-day {
  color: var(--color-owner);
}
.cal-cell.outside.sun .cal-day,
.cal-cell.outside.sat .cal-day {
  opacity: 0.4;
}
/* 오늘 — 테두리로만 표시(선택과 구분) */
.cal-cell.today {
  border-color: var(--color-owner);
}
/* 선택한 날짜 — 배경 틴트 */
.cal-cell.selected {
  background: var(--color-owner-weak);
  border-color: var(--color-owner);
}

.cal-day {
  line-height: 1.2;
}

/* 상태 점 — 점이 없어도 높이를 차지해 셀 높이가 들쭉날쭉하지 않게 한다 */
.cal-dots {
  display: flex;
  gap: 2px;
  min-height: 5px;
}
.cal-dot {
  width: 5px;
  height: 5px;
  border-radius: var(--radius-pill);
}

/* 건수 뱃지 — 셀 오른쪽 위 모서리 */
.cal-count {
  position: absolute;
  top: 0;
  right: 2px;
  font-size: 10px;
  font-weight: var(--weight-bold);
  color: var(--color-owner);
}
</style>
