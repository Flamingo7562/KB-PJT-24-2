<script setup>
/**
 * [E] 페이지 없음(404)  ·  catch-all(/:pathMatch(.*)*)  ·  PUBLIC
 * 정의되지 않은 경로. 예전처럼 /로 리다이렉트하지 않고 404 의미를 그대로 보여준다
 * (401·403·404·410 의미 분리 — docs/rules/routing.md).
 * 홈 버튼은 로그인 상태면 역할 홈으로, 아니면 온보딩(/)으로 보낸다.
 */
import { Compass } from 'lucide-vue-next'
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
    <span class="error-icon error-icon--muted">
      <Compass :size="48" />
    </span>
    <p class="error-code">404</p>
    <h1 class="error-title">페이지를 찾을 수 없어요</h1>
    <p class="error-desc">
      요청하신 주소가 없거나 이동되었어요.<br />
      주소를 확인하거나 홈으로 돌아가주세요.
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
.error-icon--muted {
  background: var(--color-bg);
  color: var(--color-text-sub);
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
