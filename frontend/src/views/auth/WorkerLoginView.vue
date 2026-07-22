<script setup>
/**
 * [A] 알바생 로그인  ·  /worker/login  ·  PUBLIC(게스트 전용)
 * 아이디·비밀번호 로그인. redirect 쿼리 지원(초대 링크 복귀).
 * 연계 API: POST /auth/login  →  useAuthStore().login({ loginId, password, role: 'WORKER' })
 * 성공 후: route.query.redirect ?? '/worker/home'.
 * 공통: AppField · BaseButton · useUiStore().toast
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="알바생 로그인" to="/" />
    <main class="screen-body">
      <EmptyState message="알바생 로그인 화면 (TODO: 담당 A 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
