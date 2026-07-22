<script setup>
/**
 * [F] 알바생 근로관리  ·  /worker/work  ·  WORKER  (탭 화면)
 * 근무 히스토리 리스트(상태 뱃지, ? 아이콘 → 문의/신고).
 * 연계 API: GET /worker/shifts  →  @/services/worker (listWorkerShifts)
 * 공통: StatusChip(근무/정산 상태) · 항목 클릭 → /worker/work/shifts/:shiftId
 *   ? 아이콘 → 문의하기 모달(BaseBottomSheet, getOwnerContact) / 신고 → /worker/work/shifts/:id/report
 */
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <EmptyState message="알바생 근로관리 화면 (TODO: 담당 F 구현)" />
</template>
