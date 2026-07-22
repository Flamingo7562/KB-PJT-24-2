<script setup>
/**
 * [H] 알바생 회원정보 변경  ·  /worker/mypage/profile  ·  WORKER
 * 이름·전화번호 수정.
 * 연계 API: GET /users/me · PATCH /users/me  →  @/services/users (getMe, updateMe)
 * 공통: AppField · BaseButton
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="회원정보 변경" />
    <main class="screen-body">
      <EmptyState message="알바생 회원정보 변경 화면 (TODO: 담당 H 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
