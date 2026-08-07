<script setup>
import { Search } from 'lucide-vue-next'
import { computed } from 'vue'

import TransactionItem from '@/components/wallet/TransactionItem.vue'

const props = defineProps({
  transactions: { type: Array, default: () => [] },
  page: { type: Object, default: () => ({ number: 0, totalPages: 0 }) },
  loading: { type: Boolean, default: false }
})

defineEmits(['open-filter', 'load-more'])

const hasNextPage = computed(() => props.page.number + 1 < props.page.totalPages)
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

    <p v-if="loading && transactions.length === 0" class="empty">불러오는 중…</p>
    <p v-else-if="transactions.length === 0" class="empty">거래 내역이 없습니다.</p>
    <template v-else>
      <ul class="list">
        <TransactionItem v-for="tx in transactions" :key="tx.transactionId" :tx="tx" />
      </ul>
      <button
        v-if="hasNextPage"
        type="button"
        class="load-more"
        :disabled="loading"
        @click="$emit('load-more')"
      >
        {{ loading ? '불러오는 중…' : '거래 더 보기' }}
      </button>
    </template>
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
}

.empty {
  padding: var(--space-xl) 0;
  text-align: center;
  color: var(--color-text-sub);
  font-size: var(--text-md);
}
.load-more {
  width: 100%;
  margin-top: var(--space-md);
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text-sub);
}
.load-more:disabled {
  opacity: 0.6;
}
</style>
