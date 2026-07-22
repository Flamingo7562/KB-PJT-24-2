<script setup>
/**
 * [A] 알바생 회원가입  ·  /worker/signup  ·  PUBLIC(게스트 전용)
 * 사장과 동일 필드 구성(아이디·비밀번호·확인·이름·이메일·전화번호 선택).
 * 연계 API: GET /auth/check-login-id · GET /auth/check-email · POST /auth/signup
 *   →  @/services/auth — role: 'WORKER'
 * 성공 후: 로그인 화면으로 이동.
 * 폼 자체는 사장/알바생 공용 AuthSignupForm(@/components/auth) 에 있다.
 */
import AuthSignupForm from '@/components/auth/AuthSignupForm.vue'
import AppBackHeader from '@/components/common/AppBackHeader.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="알바생 회원가입" to="/worker/login" />
    <main class="screen-body">
      <AuthSignupForm role="WORKER" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
