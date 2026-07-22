<script setup>
/**
 * [F] 임금분쟁 신고  ·  /worker/work/shifts/:shiftId/report  ·  WORKER(본인 근무)
 * 경위서 작성·제출. 기록·알림용 — 정산 영향 없음. 제출 시 사장 알림(WAGE_REPORTED).
 * 연계 API: POST /worker/shifts/{id}/reports  →  @/services/shifts (createReport)
 * route.params.shiftId 사용. 공통: BaseButton · 제출 후 useUiStore().toast + 뒤로가기.
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="임금분쟁 신고" />
    <main class="screen-body">
      <EmptyState message="임금분쟁 신고 화면 (TODO: 담당 F 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
