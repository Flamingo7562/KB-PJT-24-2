<script setup>
/**
 * [E] 사장 마이페이지  ·  /owner/mypage  ·  OWNER
 * 안심일터 뱃지 카드(등급 + 안심거래 N건 남음 + 정의 소자) + 회원정보/비밀번호 변경 진입
 * + 사업장 관리 진입 + 회원 탈퇴.
 * 연계 API: GET /users/me · GET /users/me/badge · DELETE /users/me
 *   →  @/services/users (getMe, getBadge, deleteMe)
 * 진입: /owner/mypage/{profile,password,workplaces}. 공통: TrustBadge(role='owner')
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="마이페이지" />
    <main class="screen-body">
      <EmptyState message="사장 마이페이지 화면 (TODO: 담당 E 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
