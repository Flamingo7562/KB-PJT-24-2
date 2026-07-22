<script setup>
import { CalendarCheck, FileText, QrCode, Wallet } from 'lucide-vue-next'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()

// 사장 하단 탭: 홈(지갑)·근태관리·문서함·QR
// 아직 라우트가 없는 탭은 to=null 로 두고 비활성 표시(후속 이슈에서 연결).
const tabs = [
  { key: 'home', label: '홈', icon: Wallet, to: '/owner/home' },
  { key: 'attendance', label: '근태관리', icon: CalendarCheck, to: null },
  { key: 'documents', label: '문서함', icon: FileText, to: null },
  { key: 'qr', label: 'QR', icon: QrCode, to: null }
]
</script>

<template>
  <nav class="bottom-nav" aria-label="사장 메뉴">
    <template v-for="tab in tabs" :key="tab.key">
      <RouterLink
        v-if="tab.to"
        :to="tab.to"
        class="tab"
        :class="{ 'is-active': route.path === tab.to }"
      >
        <component :is="tab.icon" :size="22" />
        <span>{{ tab.label }}</span>
      </RouterLink>
      <button v-else type="button" class="tab is-disabled" disabled>
        <component :is="tab.icon" :size="22" />
        <span>{{ tab.label }}</span>
      </button>
    </template>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
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

.tab.is-disabled {
  opacity: 0.4;
  cursor: default;
}
</style>
