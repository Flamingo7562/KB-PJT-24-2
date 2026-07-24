<script setup>
/**
 * [E] 접근 권한 없음(403)  ·  /forbidden  ·  PUBLIC
 * 권한·상태 위반으로 접근이 막힌 화면. 서버 403(RESOURCE_FORBIDDEN 등)의 의미를
 * 401(로그인)·404(없음)·410(만료)과 분리해 명시적으로 안내한다(catch-all → / 금지).
 * 홈 버튼은 로그인 상태면 역할 홈으로, 아니면 온보딩(/)으로 보낸다.
 */
import { ShieldAlert } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

import BaseButton from '@/components/common/BaseButton.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

function goHome() {
  router.push(auth.isAuthenticated ? auth.homeRoute() : '/')
}
</script>

<template>
  <div class="error-page">
    <span class="error-icon error-icon--danger">
      <ShieldAlert :size="48" />
    </span>
    <p class="error-code">403</p>
    <h1 class="error-title">접근 권한이 없어요</h1>
    <p class="error-desc">
      이 페이지를 볼 수 있는 권한이 없어요.<br />
      로그인 계정을 확인하거나 홈으로 돌아가주세요.
    </p>
    <BaseButton variant="primary" size="lg" block @click="goHome">홈으로</BaseButton>
  </div>
</template>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  min-height: 100vh;
  padding: var(--space-xl) var(--space-lg);
  text-align: center;
}
.error-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  border-radius: var(--radius-pill);
}
.error-icon--danger {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}
.error-code {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text-sub);
  letter-spacing: 0.1em;
}
.error-title {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.error-desc {
  font-size: var(--text-md);
  color: var(--color-text-sub);
  line-height: 1.6;
}
.error-page .btn {
  margin-top: var(--space-lg);
}
</style>
