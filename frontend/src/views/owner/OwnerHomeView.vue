<script setup>
/**
 * [B] 사장 홈(지갑)  ·  /owner/home  ·  OWNER  (탭 화면 — chrome 은 OwnerTabLayout)
 * 지갑 잔액·예치중(전 지점 합산, 지점 select 무관) + 충전·출금 + 송금상세 리스트.
 * 연계 API: GET /wallet · GET /wallet/transactions  →  @/services/wallet
 * TODO(담당 B): 송금상세 필터 바텀시트(BaseBottomSheet) 연결.
 */
import { Lock } from 'lucide-vue-next'
import { storeToRefs } from 'pinia'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

import TransactionList from '@/components/wallet/TransactionList.vue'
import WalletBalanceCard from '@/components/wallet/WalletBalanceCard.vue'
import { useWalletStore } from '@/stores/wallet'
import { formatKRW } from '@/utils/format'

const router = useRouter()
const walletStore = useWalletStore()
const { balance, heldAmount, transactions, loading } = storeToRefs(walletStore)

onMounted(() => {
  walletStore.loadHome()
})

const onCharge = () => router.push('/owner/wallet/charge')
const onWithdraw = () => router.push('/owner/wallet/withdraw')
// TODO(담당 B): 송금상세 필터 시트 열기
const onOpenFilter = () => {}
</script>

<template>
  <div class="owner-home">
    <WalletBalanceCard :balance="balance" @charge="onCharge" @withdraw="onWithdraw" />

    <div class="held-summary">
      <span class="held-label">
        <Lock :size="16" />
        예치중
      </span>
      <strong class="held-amount">{{ formatKRW(heldAmount) }}</strong>
    </div>

    <TransactionList :transactions="transactions" :loading="loading" @open-filter="onOpenFilter" />
  </div>
</template>

<style scoped>
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
