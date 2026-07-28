<script setup>
import { Info } from 'lucide-vue-next'
import { computed } from 'vue'

import { formatKRW } from '@/utils/format'

const props = defineProps({
  earning: { type: Object, required: true }
})

const TOOLTIP = '휴게시간, 지각 등 특이사항이 있을 경우 실제 지급되는 금액은 상이할 수 있습니다.'

// 진행바는 경과(적립) 표시만. 지각 차감 구간 없음 — 지급액은 합의 일급(agreedWage) 전액.
const progress = computed(() => Math.min(1, Math.max(0, props.earning.progressRatio || 0)))
</script>

<template>
  <section class="earning-card">
    <header class="head">
      <h2 class="title">확보 안심금액</h2>
      <button type="button" class="info" :title="TOOLTIP" aria-label="확보 안심금액 안내">
        <Info :size="16" />
      </button>
    </header>

    <p class="amount">{{ formatKRW(earning.accruedAmount) }}</p>
    <p class="sub">일급 {{ formatKRW(earning.dailyWage) }} 기준 실시간 적립</p>

    <div
      class="bar"
      role="progressbar"
      :aria-valuenow="Math.round(progress * 100)"
      aria-valuemin="0"
      aria-valuemax="100"
    >
      <div class="seg" :style="{ width: progress * 100 + '%' }"></div>
    </div>

    <!-- 지각은 표시(뱃지)로만 — 임금 차감 없음 -->
    <p v-if="earning.lateMinutes > 0" class="late-note">지각 {{ earning.lateMinutes }}분</p>
  </section>
</template>

<style scoped>
.earning-card {
  margin-top: var(--space-md);
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.head {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.title {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}

.info {
  display: inline-flex;
  color: var(--color-text-sub);
}

.amount {
  margin-top: var(--space-sm);
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--color-worker);
}

.sub {
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.bar {
  display: flex;
  height: 10px;
  margin-top: var(--space-md);
  border-radius: var(--radius-pill);
  background: var(--color-bg);
  overflow: hidden;
}

.seg {
  background: var(--color-worker);
}

.late-note {
  margin-top: var(--space-sm);
  font-size: var(--text-sm);
  color: var(--color-warning);
}
</style>
