<script setup>
/**
 * [A] 사장 회원가입  ·  /owner/signup  ·  PUBLIC(게스트 전용)
 * 필드: 아이디(중복확인)·비밀번호·비밀번호 확인·이름·이메일(중복확인)·전화번호(선택).
 * 연계 API: GET /auth/check-login-id · GET /auth/check-email · POST /auth/signup
 *   →  @/services/auth (checkLoginId, checkEmail, signup) — role: 'OWNER'
 * 성공 후: 로그인 화면으로 이동(자동 로그인 없음).
 * 공통: AppField(+ suffix 슬롯에 중복확인 BaseButton) · @/utils/validators
 * 폼 자체는 사장/알바생 공용 AuthSignupForm(@/components/auth) 에 있다.
 */
import AuthSignupForm from '@/components/auth/AuthSignupForm.vue'
import AppBackHeader from '@/components/common/AppBackHeader.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="사장 회원가입" to="/owner/login" />
    <main class="screen-body">
      <AuthSignupForm role="OWNER" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
