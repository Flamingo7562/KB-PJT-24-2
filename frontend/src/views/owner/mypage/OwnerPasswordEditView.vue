<script setup>
/**
 * [E] 사장 비밀번호 변경  ·  /owner/mypage/password  ·  OWNER
 * 현재 비밀번호 확인 후 새 비밀번호 설정(불일치 시 400).
 * 연계 API: PATCH /users/me/password  →  @/services/users (changePassword)
 * 공통: AppField · BaseButton · @/utils/validators (passwordRule, passwordsMatch)
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="비밀번호 변경" />
    <main class="screen-body">
      <EmptyState message="사장 비밀번호 변경 화면 (TODO: 담당 E 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
