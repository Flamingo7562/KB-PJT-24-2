<script setup>
/**
 * [H] 알바생 마이페이지  ·  /worker/mypage  ·  WORKER
 * 성실근로자 뱃지 카드(등급 + 성실근로 N건 남음 + 정의 소자) + 회원정보/비밀번호 변경 진입 + 회원 탈퇴.
 * 연계 API: GET /users/me · GET /users/me/badge · DELETE /users/me
 *   →  @/services/users (getMe, getBadge, deleteMe)
 * 진입: /worker/mypage/{profile,password}. 공통: TrustBadge(role='worker').
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="마이페이지" />
    <main class="screen-body">
      <EmptyState message="알바생 마이페이지 화면 (TODO: 담당 H 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
