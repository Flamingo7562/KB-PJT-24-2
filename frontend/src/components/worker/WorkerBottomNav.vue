<script setup>
import { ClipboardList, FileText, QrCode, Wallet } from 'lucide-vue-next'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()

// 알바생 하단 탭: 안심지갑(홈)·근로관리·QR·문서함 (문서·QR 아이콘은 사장과 동일 재사용)
const tabs = [
  { key: 'home', label: '안심지갑', icon: Wallet, to: '/worker/home' },
  { key: 'work', label: '근로관리', icon: ClipboardList, to: '/worker/work' },
  { key: 'scan', label: 'QR', icon: QrCode, to: '/worker/scan' },
  { key: 'documents', label: '문서함', icon: FileText, to: '/worker/documents' }
]

const isActive = (to) => route.path === to || route.path.startsWith(`${to}/`)
</script>

<template>
  <nav class="bottom-nav" aria-label="알바생 메뉴">
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
