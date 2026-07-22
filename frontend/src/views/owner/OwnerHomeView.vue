<script setup>
import { Bell, CircleUser, Lock } from 'lucide-vue-next'
import { storeToRefs } from 'pinia'
import { onMounted } from 'vue'

import LogoSymbol from '@/assets/images/logo/logo-symbol.svg'
import OwnerBottomNav from '@/components/owner/OwnerBottomNav.vue'
import TransactionList from '@/components/wallet/TransactionList.vue'
import WalletBalanceCard from '@/components/wallet/WalletBalanceCard.vue'
import { useWalletStore } from '@/stores/wallet'
import { formatKRW } from '@/utils/format'

const walletStore = useWalletStore()
const { balance, heldAmount, transactions, loading } = storeToRefs(walletStore)

onMounted(() => {
  walletStore.loadHome()
})

// TODO(후속 이슈): 충전(/owner/wallet/charge)·출금(/owner/wallet/withdraw) 화면 연결
function onCharge() {}
function onWithdraw() {}
function onOpenFilter() {}
</script>

<template>
  <div class="owner-home with-tabbar">
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
      <WalletBalanceCard :balance="balance" @charge="onCharge" @withdraw="onWithdraw" />

      <div class="held-summary">
        <span class="held-label">
          <Lock :size="16" />
          예치중
        </span>
        <strong class="held-amount">{{ formatKRW(heldAmount) }}</strong>
      </div>

      <TransactionList
        :transactions="transactions"
        :loading="loading"
        @open-filter="onOpenFilter"
      />
    </main>

    <OwnerBottomNav />
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
  color: var(--color-owner);
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

/* 예치중 요약 — 지갑 카드 밖 별도 라인 */
.held-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.held-label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--text-md);
  color: var(--color-text-sub);
}

.held-amount {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-owner);
}
</style>
