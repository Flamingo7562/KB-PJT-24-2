<script setup>
/**
 * [A] 온보딩 · 역할 선택  ·  /  ·  PUBLIC
 * 서비스 소개 + 사장/알바생 토글 → 선택 역할의 로그인·회원가입 진입.
 * 로그인 상태로 접근하면 가드(G3)가 역할 홈으로 보낸다.
 */
import { ref } from 'vue'
import { RouterLink } from 'vue-router'

import LogoGighub from '@/assets/images/logo/logo-gighub.svg'

const role = ref('owner') // 'owner' | 'worker'

const features = [
  { title: '안심 에스크로', desc: '근무 확정 시 임금을 미리 예치, 정산까지 안전하게.' },
  { title: 'QR 출퇴근', desc: '짧은 수명 QR + 위치로 출퇴근을 간편하게 인증.' },
  { title: '문서·정산 한곳에', desc: '계약서·보건증·정산 이력을 한 화면에서.' }
]
</script>

<template>
  <div class="onboarding">
    <div class="hero">
      <LogoGighub
        class="logo"
        :class="role === 'owner' ? 'is-owner' : 'is-worker'"
        aria-label="Gig Hub"
      />
      <p class="tagline">전자지갑·에스크로 근로정산 서비스</p>
    </div>

    <ul class="features">
      <li v-for="f in features" :key="f.title" class="feature">
        <strong>{{ f.title }}</strong>
        <span>{{ f.desc }}</span>
      </li>
    </ul>

    <div class="role-toggle" role="tablist" aria-label="역할 선택">
      <button
        type="button"
        class="role"
        :class="{ active: role === 'owner' }"
        @click="role = 'owner'"
      >
        사장님
      </button>
      <button
        type="button"
        class="role"
        :class="{ active: role === 'worker' }"
        @click="role = 'worker'"
      >
        알바생
      </button>
    </div>

    <div class="cta">
      <RouterLink :to="`/${role}/login`" class="btn btn-primary">로그인</RouterLink>
      <RouterLink :to="`/${role}/signup`" class="btn btn-secondary">회원가입</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.onboarding {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding: var(--space-xl) var(--space-lg);
}
.hero {
  margin-top: var(--space-xl);
  text-align: center;
}
.logo {
  width: 180px;
  height: auto;
  margin: 0 auto;
}
.logo.is-owner {
  color: var(--color-owner);
}
.logo.is-worker {
  color: var(--color-worker);
}
.tagline {
  margin-top: var(--space-md);
  color: var(--color-text-sub);
  font-size: var(--text-md);
}
.features {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin: var(--space-xl) 0;
}
.feature {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.feature strong {
  font-size: var(--text-md);
  color: var(--color-text);
}
.feature span {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.role-toggle {
  display: flex;
  gap: var(--space-sm);
  margin-top: auto;
}
.role {
  flex: 1;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
  background: var(--color-surface);
}
.role.active {
  border-color: var(--color-primary);
  color: var(--color-text);
}
.cta {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin-top: var(--space-md);
}
.btn {
  display: block;
  text-align: center;
  padding: var(--space-md);
  border-radius: var(--radius-sm);
  font-weight: var(--weight-medium);
}
.btn-primary {
  background: var(--color-primary);
  color: var(--color-on-primary);
}
.btn-secondary {
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}
</style>
