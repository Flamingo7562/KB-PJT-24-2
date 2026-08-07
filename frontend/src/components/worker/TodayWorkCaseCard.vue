<script setup>
import { CalendarX } from 'lucide-vue-next'
import { computed } from 'vue'

import StatusChip from '@/components/common/StatusChip.vue'
import { formatSeoulTimeRange } from '@/utils/format'

const props = defineProps({
  workCase: { type: Object, default: null }
})

const isEmpty = computed(() => !props.workCase || props.workCase.status === 'NONE')

const statusKind = computed(() =>
  ['BEFORE_WORK', 'LATE', 'NONE'].includes(props.workCase?.status) ? 'today' : 'workCase'
)

const timeRange = computed(() =>
  props.workCase?.startsAt
    ? formatSeoulTimeRange(props.workCase.startsAt, props.workCase.endsAt)
    : `${props.workCase?.startTime ?? ''}–${props.workCase?.endTime ?? ''}`
)
</script>

<template>
  <section class="today-card">
    <h2 class="title">오늘의 알바</h2>

    <div v-if="isEmpty" class="empty">
      <CalendarX :size="20" />
      <span>오늘은 예정된 알바가 없어요.</span>
    </div>

    <div v-else class="work-case">
      <StatusChip :status="workCase.status" :kind="statusKind" />
      <p class="work-case-title">{{ workCase.title }}</p>
      <p class="work-case-info">{{ workCase.workplaceName }} · {{ timeRange }}</p>
    </div>
  </section>
</template>

<style scoped>
.today-card {
  margin-top: var(--space-md);
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.title {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}

.empty {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: var(--space-md);
  color: var(--color-text-sub);
  font-size: var(--text-md);
}

.work-case-title {
  margin-top: var(--space-sm);
  font-size: var(--text-lg);
  font-weight: var(--weight-medium);
  color: var(--color-text);
}

.work-case-info {
  margin-top: var(--space-xs);
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
</style>
