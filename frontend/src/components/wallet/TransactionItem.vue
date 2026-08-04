<script setup>
import { ArrowUpRight, CircleCheck, Lock, Plus, RotateCcw } from 'lucide-vue-next'
import { computed } from 'vue'

import { formatDateTime, formatSignedKRW } from '@/utils/format'

const props = defineProps({
  tx: { type: Object, required: true }
})

const CREDIT_TYPES = new Set(['FUNDING', 'ESCROW_REFUND', 'WITHDRAWAL_REFUND'])

// 거래 유형 → 좌측 아이콘
const typeIcon = computed(() => {
  switch (props.tx.type) {
    case 'FUNDING':
      return Plus
    case 'WITHDRAWAL':
      return ArrowUpRight
    case 'ESCROW_REFUND':
    case 'WITHDRAWAL_REFUND':
      return RotateCcw
    case 'ESCROW_RELEASE':
      return CircleCheck
    case 'ESCROW_HOLD':
    default:
      return Lock
  }
})

// 상태 → 칩 라벨·색 토큰·아이콘
const statusMeta = computed(() => {
  switch (props.tx.type) {
    case 'ESCROW_HOLD':
      return { color: 'var(--color-owner)', icon: Lock }
    case 'ESCROW_RELEASE':
      return { color: 'var(--color-success)', icon: CircleCheck }
    case 'ESCROW_REFUND':
    case 'WITHDRAWAL_REFUND':
      return { color: 'var(--color-text-sub)', icon: RotateCcw }
    default:
      return { color: 'var(--color-text-sub)', icon: CircleCheck }
  }
})

const isCredit = computed(() => CREDIT_TYPES.has(props.tx.type))
const amountText = computed(() =>
  formatSignedKRW(props.tx.amount, isCredit.value ? 'CREDIT' : 'DEBIT')
)
const description = computed(() => {
  if (props.tx.workTitle && props.tx.workplaceName) {
    return `${props.tx.workTitle} · ${props.tx.workplaceName}`
  }
  return props.tx.workTitle || props.tx.workplaceName || props.tx.displayStatus
})
</script>

<template>
  <li class="tx">
    <span class="icon" :class="{ 'is-credit': isCredit }">
      <component :is="typeIcon" :size="20" />
    </span>

    <div class="body">
      <p class="desc">{{ description }}</p>
      <p class="date">{{ formatDateTime(tx.createdAt) }}</p>
    </div>

    <div class="right">
      <p class="amount" :class="{ 'is-credit': isCredit }">{{ amountText }}</p>
      <span class="status" :style="{ color: statusMeta.color }">
        <component :is="statusMeta.icon" :size="12" />
        {{ tx.displayStatus }}
      </span>
    </div>
  </li>
</template>

<style scoped>
.tx {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg) 0;
  border-bottom: 1px solid var(--color-border);
}

.icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: var(--radius-pill);
  background: var(--color-bg);
  color: var(--color-text-sub);
}

.icon.is-credit {
  color: var(--color-owner);
}

.body {
  flex: 1;
  min-width: 0;
}

.desc {
  font-size: var(--text-lg);
  font-weight: var(--weight-medium);
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.date {
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.right {
  text-align: right;
  flex-shrink: 0;
}

.amount {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}

.amount.is-credit {
  color: var(--color-owner);
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
}
</style>
