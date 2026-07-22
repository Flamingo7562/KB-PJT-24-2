<script setup>
import { CalendarX, Clock, TriangleAlert, UserX } from 'lucide-vue-next'
import { computed } from 'vue'

const props = defineProps({
  shift: { type: Object, default: null }
})

const isEmpty = computed(() => !props.shift || props.shift.status === 'NONE')

// 상태 → 뱃지 라벨·색·아이콘
const meta = computed(() => {
  switch (props.shift?.status) {
    case 'BEFORE_WORK':
      return { label: '출근 전', color: 'var(--color-text-sub)', bg: 'var(--color-bg)', icon: Clock }
    case 'LATE':
      return {
        label: '지각',
        color: 'var(--color-warning)',
        bg: 'var(--color-warning-bg)',
        icon: TriangleAlert
      }
    case 'NO_SHOW':
      return {
        label: '노쇼',
        color: 'var(--color-danger)',
        bg: 'var(--color-danger-bg)',
        icon: UserX
      }
    default:
      return null
  }
})
</script>

<template>
  <section class="today-card">
    <h2 class="title">오늘의 알바</h2>

    <div v-if="isEmpty" class="empty">
      <CalendarX :size="20" />
      <span>오늘은 예정된 알바가 없어요.</span>
    </div>

    <div v-else class="shift">
      <span class="badge" :style="{ color: meta.color, background: meta.bg }">
        <component :is="meta.icon" :size="14" />
        {{ meta.label }}
      </span>
      <p class="shift-title">{{ shift.title }}</p>
      <p class="shift-info">{{ shift.workplaceName }} · {{ shift.startTime }}–{{ shift.endTime }}</p>
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

.badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-pill);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
}

.shift-title {
  margin-top: var(--space-sm);
  font-size: var(--text-lg);
  font-weight: var(--weight-medium);
  color: var(--color-text);
}

.shift-info {
  margin-top: var(--space-xs);
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
</style>
