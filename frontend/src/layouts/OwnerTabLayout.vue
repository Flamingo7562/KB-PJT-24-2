<script setup>
/**
 * 사장 탭 화면 레이아웃 — 상단바 + 본문(RouterView) + 하단 탭.
 * 라우터에서 사장 탭 화면(홈·근태관리·문서함·QR)의 부모로 쓴다.
 * 각 view 는 본문(screen-body 안)만 작성한다 — chrome 은 여기서 담당.
 */
import { RouterView } from 'vue-router'

import AppTopBar from '@/components/common/AppTopBar.vue'
import OwnerBottomNav from '@/components/owner/OwnerBottomNav.vue'
</script>

<template>
  <div class="tab-screen">
    <AppTopBar role="OWNER" />
    <main class="screen-body with-tabbar">
      <RouterView />
    </main>
    <OwnerBottomNav />
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
