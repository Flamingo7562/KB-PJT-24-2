<script setup>
/**
 * [C] 근무 상세  ·  /owner/attendance/shifts/:shiftId  ·  OWNER
 * 근무 상세 + 매칭 알바생 성실 뱃지. 수정·삭제·연결 링크 복사는 매칭전(OPEN)만.
 * 확정(날인) 후 수정·삭제 버튼 숨김 — 서버도 409.
 * 연계 API: GET /shifts/{id} · PATCH /shifts/{id} · DELETE /shifts/{id} · POST /shifts/{id}/invites
 *   →  @/services/shifts (getShift, updateShift, deleteShift, createInvite)
 * route.params.shiftId 사용. 공통: TrustBadge(알바생 뱃지) · StatusChip · BaseModal(삭제 확인)
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="근무 상세" />
    <main class="screen-body">
      <EmptyState message="근무 상세 화면 (TODO: 담당 C 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
