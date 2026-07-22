<script setup>
/**
 * [C] 사장 근태관리  ·  /owner/attendance  ·  OWNER  (탭 화면)
 * 근태 현황(채용중·근무중) + 근무 리스트(최신순·검색) + '근무 포지션 추가'(→ /owner/attendance/shifts/new).
 * 지점 컨텍스트: useWorkplaceStore().selectedId 기준.
 * 연계 API: GET /workplaces/{id}/shifts/summary · GET /workplaces/{id}/shifts
 *   →  @/services/shifts (getShiftSummary, listShifts)
 * 공통: StatusChip(근무 상태) · EmptyState · 항목 클릭 → /owner/attendance/shifts/:shiftId
 */
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <EmptyState message="사장 근태관리 화면 (TODO: 담당 C 구현)" />
</template>
