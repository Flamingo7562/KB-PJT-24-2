<script setup>
/**
 * 상태 칩 — 근무/거래/정산/오늘일정 상태를 라벨+색+아이콘으로 통일 표기.
 *
 * 라벨·색은 utils/constants 의 단일 소스에서, 아이콘은 여기서 매핑한다
 * (assets/README.md 리스트 상태 표시 규약 준수).
 *
 * 사용: <StatusChip :status="shift.status" kind="shift" />
 *   kind: 'shift' | 'tx' | 'settle' | 'today'
 */
import {
  CircleCheck,
  Clock,
  Loader,
  Lock,
  Minus,
  RotateCcw,
  TriangleAlert,
  UserX
} from 'lucide-vue-next'
import { computed } from 'vue'

import { SHIFT_STATUS, SETTLE_STATUS, TODAY_SHIFT_STATUS, TX_STATUS } from '@/utils/constants'

const props = defineProps({
  status: { type: String, required: true },
  kind: { type: String, default: 'shift' }
})

const LABEL_MAPS = {
  shift: SHIFT_STATUS,
  tx: TX_STATUS,
  settle: SETTLE_STATUS,
  today: TODAY_SHIFT_STATUS
}

// 상태값(enum) → lucide 아이콘. 서로 다른 kind 가 같은 상태값을 공유한다.
const ICONS = {
  OPEN: Clock,
  MATCHED: Clock,
  IN_PROGRESS: Loader,
  COMPLETED: CircleCheck,
  NO_SHOW: UserX,
  HOLD: Lock,
  SETTLED: CircleCheck,
  REFUNDED: RotateCcw,
  DONE: CircleCheck,
  BEFORE_WORK: Clock,
  LATE: TriangleAlert,
  NONE: Minus
}

const meta = computed(() => {
  const map = LABEL_MAPS[props.kind] ?? SHIFT_STATUS
  return map[props.status] ?? { label: props.status, color: 'var(--color-text-sub)' }
})
const icon = computed(() => ICONS[props.status] ?? Clock)
</script>

<template>
  <span class="chip" :style="{ color: meta.color }">
    <component :is="icon" :size="13" />
    {{ meta.label }}
  </span>
</template>

<style scoped>
.chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  white-space: nowrap;
}
</style>
