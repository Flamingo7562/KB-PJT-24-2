<script setup>
/**
 * 알바생 탭 화면 레이아웃 — 상단바 + 본문(RouterView) + 하단 탭.
 * 라우터에서 알바생 탭 화면(안심지갑·근로관리·QR·문서함)의 부모로 쓴다.
 * 각 view 는 본문(screen-body 안)만 작성한다 — chrome 은 여기서 담당.
 */
import { RouterView } from 'vue-router'

import AppTopBar from '@/components/common/AppTopBar.vue'
import WorkerBottomNav from '@/components/worker/WorkerBottomNav.vue'
</script>

<template>
  <div class="tab-screen">
    <AppTopBar role="WORKER" />
    <main class="screen-body with-tabbar">
      <RouterView />
    </main>
    <WorkerBottomNav />
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
