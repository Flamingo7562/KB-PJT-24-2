<script setup>
import { Bell, CircleUser } from 'lucide-vue-next'
import { storeToRefs } from 'pinia'
import { computed, onMounted } from 'vue'

import LogoSymbol from '@/assets/images/logo/logo-symbol.svg'
import SecuredEarningCard from '@/components/worker/SecuredEarningCard.vue'
import TodayShiftCard from '@/components/worker/TodayShiftCard.vue'
import WorkerBottomNav from '@/components/worker/WorkerBottomNav.vue'
import WorkerWalletCard from '@/components/worker/WorkerWalletCard.vue'
import { useWorkerHomeStore } from '@/stores/workerHome'

const workerHomeStore = useWorkerHomeStore()
const { balance, todayShift, earning } = storeToRefs(workerHomeStore)

// 오늘 근무가 있을 때만 확보 안심금액 표시
const showEarning = computed(() => earning.value && todayShift.value?.status !== 'NONE')

onMounted(() => {
  workerHomeStore.loadHome()
})

// TODO(후속 이슈): 출금(/worker/wallet/withdraw) 화면 연결
function onWithdraw() {}
</script>

<template>
  <div class="worker-home with-tabbar">
    <header class="topbar">
      <span class="brand">
        <LogoSymbol class="brand-logo" aria-label="Gig Hub" />
        <span class="brand-name">Gig Hub</span>
      </span>
      <div class="icons">
        <button type="button" class="icon-btn" aria-label="알림">
          <Bell :size="22" />
        </button>
        <button type="button" class="icon-btn" aria-label="마이페이지">
          <CircleUser :size="22" />
        </button>
      </div>
    </header>

    <main class="content">
      <WorkerWalletCard :balance="balance" @withdraw="onWithdraw" />

      <TodayShiftCard :shift="todayShift" />

      <SecuredEarningCard v-if="showEarning" :earning="earning" />
    </main>

    <WorkerBottomNav />
  </div>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
}

.brand-logo {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  color: var(--color-worker);
}

.brand-name {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}

.icons {
  display: flex;
  gap: var(--space-sm);
}

.icon-btn {
  color: var(--color-text);
}

.content {
  padding: var(--space-lg);
}
</style>
