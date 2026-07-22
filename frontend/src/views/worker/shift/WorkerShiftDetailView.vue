<script setup>
/**
 * [F] 알바생 근무 정보 상세  ·  /worker/work/shifts/:shiftId  ·  WORKER(본인 근무)
 * 근무 정보 확인(제목·날짜·시간·휴게·일급·정산 상태).
 * 연계 API: GET /shifts/{id}  →  @/services/shifts (getShift)
 * route.params.shiftId 사용. 공통: StatusChip · 문의하기 모달 · 신고 진입.
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="근무 정보" />
    <main class="screen-body">
      <EmptyState message="알바생 근무 상세 화면 (TODO: 담당 F 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
