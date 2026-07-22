<script setup>
/**
 * [H] 알바생 비밀번호 변경  ·  /worker/mypage/password  ·  WORKER
 * 현재 비밀번호 확인 후 새 비밀번호 설정.
 * 연계 API: PATCH /users/me/password  →  @/services/users (changePassword)
 * 공통: AppField · BaseButton · @/utils/validators
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="비밀번호 변경" />
    <main class="screen-body">
      <EmptyState message="알바생 비밀번호 변경 화면 (TODO: 담당 H 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
