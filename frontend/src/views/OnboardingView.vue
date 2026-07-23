<script setup>
/**
 * [A] 온보딩  ·  /  ·  PUBLIC
 * 3단계: ① 사장님 소개 ② 알바생 소개 ③ 역할 선택(로그인·회원가입 진입).
 * 로그인·회원가입 화면의 뒤로가기는 `/?step=auth&role=owner|worker`로 이동해 ③을 바로 연다.
 * 로그인 상태로 접근하면 가드(G3)가 역할 홈으로 보낸다.
 */
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Backpack, ChevronLeft, FileText, QrCode, ShieldCheck, Store } from 'lucide-vue-next'

import LogoGighub from '@/assets/images/logo/logo-gighub.svg'

const route = useRoute()

const STEP_COUNT = 3
// 로그인·회원가입 화면에서 뒤로가기 시(?step=auth) 역할 선택 단계로 바로 진입한다.
const step = ref(route.query.step === 'auth' ? 2 : 0) // 0: 사장님 소개, 1: 알바생 소개, 2: 역할 선택

const intros = [
  {
    role: 'owner',
    icon: Store,
    title: '사장님, 급여 관리 걱정 끝',
    subtitle: '전자지갑 에스크로로 인건비를 미리 예치하고, 정산까지 투명하게 관리하세요.',
    features: [
      { icon: ShieldCheck, title: '안심 에스크로', desc: '근무 확정 시 임금을 미리 예치해 체불 걱정을 덜어요.' },
      { icon: QrCode, title: 'QR 출퇴근 관리', desc: '매장별 QR로 출퇴근 현황을 한눈에 확인해요.' },
      { icon: FileText, title: '문서 자동 보관', desc: '근로계약서·보건증을 한곳에서 관리해요.' }
    ]
  },
  {
    role: 'worker',
    icon: Backpack,
    title: '알바생, 내 임금은 안전하게',
    subtitle: '일한 만큼 안심하고 받을 수 있는 전자지갑.',
    features: [
      { icon: ShieldCheck, title: '확보된 임금 확인', desc: '사장님이 예치한 안심 금액을 바로 확인해요.' },
      { icon: QrCode, title: 'QR 출퇴근', desc: '스캔 한 번으로 출퇴근을 간편하게 인증해요.' },
      { icon: FileText, title: '정산 내역 한눈에', desc: '지급 이력과 문서를 언제든 열람해요.' }
    ]
  }
]

const isIntroStep = computed(() => step.value < intros.length)
const intro = computed(() => intros[step.value])

const role = ref(route.query.role === 'worker' ? 'worker' : 'owner') // 'owner' | 'worker' (역할 선택 단계에서 사용)

const features = [
  { title: '안심 에스크로', desc: '근무 확정 시 임금을 미리 예치, 정산까지 안전하게.' },
  { title: 'QR 출퇴근', desc: '짧은 수명 QR + 위치로 출퇴근을 간편하게 인증.' },
  { title: '문서·정산 한곳에', desc: '계약서·보건증·정산 이력을 한 화면에서.' }
]
</script>

<template>
  <div class="onboarding">
    <div class="nav-row">
      <button v-if="step > 0" type="button" class="icon-btn" aria-label="이전" @click="step--">
        <ChevronLeft :size="22" />
      </button>
      <span v-else class="icon-btn-spacer" />

      <button
        v-if="isIntroStep"
        type="button"
        class="skip"
        @click="step = intros.length"
      >
        건너뛰기
      </button>
    </div>

    <template v-if="isIntroStep">
      <div class="hero">
        <component :is="intro.icon" class="hero-icon" :class="`is-${intro.role}`" :size="56" />
        <h1 class="title">{{ intro.title }}</h1>
        <p class="tagline">{{ intro.subtitle }}</p>
      </div>

      <ul class="intro-features">
        <li v-for="f in intro.features" :key="f.title" class="intro-feature">
          <component :is="f.icon" class="intro-feature-icon" :class="`is-${intro.role}`" :size="20" />
          <div class="intro-feature-text">
            <strong>{{ f.title }}</strong>
            <span>{{ f.desc }}</span>
          </div>
        </li>
      </ul>

      <div class="dots" role="tablist" aria-label="온보딩 단계">
        <span
          v-for="n in STEP_COUNT"
          :key="n"
          class="dot"
          :class="{ active: n - 1 === step }"
        />
      </div>

      <button type="button" class="btn btn-primary" @click="step++">다음</button>
    </template>

    <template v-else>
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
    </template>
  </div>
</template>

<style scoped>
.onboarding {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding: var(--space-xl) var(--space-lg);
}
.nav-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
}
.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--color-text-sub);
}
.icon-btn-spacer {
  width: 32px;
  height: 32px;
}
.skip {
  margin-left: auto;
  padding: var(--space-xs) var(--space-sm);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.hero {
  margin-top: var(--space-lg);
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
.hero-icon {
  margin: 0 auto;
}
.hero-icon.is-owner {
  color: var(--color-owner);
}
.hero-icon.is-worker {
  color: var(--color-worker);
}
.title {
  margin-top: var(--space-md);
  font-size: var(--text-xl);
  font-weight: var(--weight-medium);
  color: var(--color-text);
}
.tagline {
  margin-top: var(--space-sm);
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
.intro-features {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin: var(--space-xl) 0;
}
.intro-feature {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.intro-feature-icon {
  flex-shrink: 0;
  margin-top: 2px;
}
.intro-feature-icon.is-owner {
  color: var(--color-owner);
}
.intro-feature-icon.is-worker {
  color: var(--color-worker);
}
.intro-feature-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.intro-feature-text strong {
  font-size: var(--text-md);
  color: var(--color-text);
}
.intro-feature-text span {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.dots {
  display: flex;
  justify-content: center;
  gap: var(--space-xs);
  margin-top: auto;
  padding: var(--space-md) 0;
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-pill);
  background: var(--color-border);
}
.dot.active {
  width: 18px;
  background: var(--color-brand);
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
