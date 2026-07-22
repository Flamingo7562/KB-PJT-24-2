<script setup>
import { Search } from 'lucide-vue-next'

import TransactionItem from '@/components/wallet/TransactionItem.vue'

defineProps({
  transactions: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

// TODO(후속 이슈): 필터 바텀시트 열기. 지금은 자리만 잡아 둔다.
defineEmits(['open-filter'])
</script>

<template>
  <section class="tx-list">
    <header class="head">
      <h2 class="title">송금상세</h2>
      <button type="button" class="filter" @click="$emit('open-filter')">
        <Search :size="16" />
        검색·필터
      </button>
    </header>

    <p v-if="loading" class="empty">불러오는 중…</p>
    <p v-else-if="transactions.length === 0" class="empty">거래 내역이 없습니다.</p>
    <ul v-else class="list">
      <TransactionItem v-for="tx in transactions" :key="tx.txId" :tx="tx" />
    </ul>
  </section>
</template>

<style scoped>
.tx-list {
  margin-top: var(--space-xl);
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}

.filter {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-xs) var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.list {
  margin-top: var(--space-sm);
  /* Bootstrap Reboot 의 ul padding-left(2rem) 무효화 */
  padding: 0;
}

.empty {
  padding: var(--space-xl) 0;
  text-align: center;
  color: var(--color-text-sub);
  font-size: var(--text-md);
}
</style>
