<script setup>
/**
 * [C] 근무 포지션 등록  ·  /owner/attendance/shifts/new  ·  OWNER
 * 제목·날짜·시작/종료시간·휴게시간(유급/무급)·일급 입력 → status=OPEN 생성.
 * 지점 컨텍스트: useWorkplaceStore().selectedId.
 * 연계 API: POST /workplaces/{id}/shifts  →  @/services/shifts (createShift)
 * 공통: AppField · BaseButton · @/utils/validators
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="근무 포지션 추가" />
    <main class="screen-body">
      <EmptyState message="근무 포지션 등록 화면 (TODO: 담당 C 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
