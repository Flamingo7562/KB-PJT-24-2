<script setup>
import { CalendarCheck, FileText, QrCode, Wallet } from 'lucide-vue-next'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()

// 사장 하단 탭: 홈(지갑)·근태관리·문서함·QR
const tabs = [
  { key: 'home', label: '홈', icon: Wallet, to: '/owner/home' },
  { key: 'attendance', label: '근태관리', icon: CalendarCheck, to: '/owner/attendance' },
  { key: 'documents', label: '문서함', icon: FileText, to: '/owner/documents' },
  { key: 'qr', label: 'QR', icon: QrCode, to: '/owner/qr' }
]

const isActive = (to) => route.path === to || route.path.startsWith(`${to}/`)
</script>

<template>
  <nav class="bottom-nav" aria-label="사장 메뉴">
    <RouterLink
      v-for="tab in tabs"
      :key="tab.key"
      :to="tab.to"
      class="tab"
      :class="{ 'is-active': isActive(tab.to) }"
    >
      <component :is="tab.icon" :size="22" />
      <span>{{ tab.label }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  z-index: var(--z-tabbar);
  width: 100%;
  max-width: 430px;
  display: flex;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
}
.tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: var(--space-sm) 0 calc(var(--space-sm) + env(safe-area-inset-bottom, 0px));
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.tab.is-active {
  color: var(--color-text);
  font-weight: var(--weight-medium);
}
</style>
