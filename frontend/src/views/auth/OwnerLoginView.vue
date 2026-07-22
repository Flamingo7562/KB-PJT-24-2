<script setup>
/**
 * [A] 사장 로그인  ·  /owner/login  ·  PUBLIC(게스트 전용)
 * 아이디·비밀번호 로그인.
 * 연계 API: POST /auth/login  →  useAuthStore().login({ loginId, password, role: 'OWNER' })
 * 성공 후: 응답 needsWorkplaceSetup=true → /owner/workplaces/new,
 *          아니면 route.query.redirect ?? '/owner/home'.
 * 공통: AppField(입력) · BaseButton(제출) · useUiStore().toast(실패 안내)
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="사장 로그인" to="/" />
    <main class="screen-body">
      <EmptyState message="사장 로그인 화면 (TODO: 담당 A 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
