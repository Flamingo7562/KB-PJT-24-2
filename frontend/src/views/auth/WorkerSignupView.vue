<script setup>
/**
 * [A] 알바생 회원가입  ·  /worker/signup  ·  PUBLIC(게스트 전용)
 * 사장과 동일 필드 구성(아이디·비밀번호·확인·이름·이메일·전화번호 선택).
 * 연계 API: GET /auth/check-login-id · GET /auth/check-email · POST /auth/signup
 *   →  @/services/auth — role: 'WORKER'
 * 성공 후: 로그인 화면으로 이동.
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="알바생 회원가입" to="/worker/login" />
    <main class="screen-body">
      <EmptyState message="알바생 회원가입 화면 (TODO: 담당 A 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
