<script setup>
/**
 * 신뢰 뱃지 — 사장 '안심일터' / 알바생 '성실근로자'.
 *
 * 마이페이지 뱃지 카드, 근무 확정(사장 뱃지), 근무 상세(알바생 뱃지)에서 공용.
 * level 0 = 이력 5건 미만(미부여) → 회색 아이콘 + '이력 쌓는 중'.
 * showRemaining=true 면 'criterion N건 남음' 을 함께 표시한다.
 *
 * 뱃지 정의 문구(*성실근로란?…)는 화면(마이페이지)에서 별도로 붙인다.
 */
import { CircleUser } from 'lucide-vue-next'
import { computed } from 'vue'

import ownerLv1 from '@/assets/images/badges/badge-owner-lv1.svg?url'
import ownerLv2 from '@/assets/images/badges/badge-owner-lv2.svg?url'
import ownerLv3 from '@/assets/images/badges/badge-owner-lv3.svg?url'
import workerLv1 from '@/assets/images/badges/badge-worker-lv1.svg?url'
import workerLv2 from '@/assets/images/badges/badge-worker-lv2.svg?url'
import workerLv3 from '@/assets/images/badges/badge-worker-lv3.svg?url'

const props = defineProps({
  role: { type: String, required: true }, // 'owner' | 'worker'
  level: { type: Number, default: 0 }, // 0~3 (0 = 미부여)
  size: { type: Number, default: 48 },
  showRemaining: { type: Boolean, default: false },
  remaining: { type: Number, default: 0 },
  criterion: { type: String, default: '' } // '성실근로' | '안심거래'
})

const BADGES = {
  owner: [null, ownerLv1, ownerLv2, ownerLv3],
  worker: [null, workerLv1, workerLv2, workerLv3]
}

const src = computed(() => BADGES[props.role]?.[props.level] ?? null)
const remainingText = computed(() =>
  props.criterion ? `${props.criterion} ${props.remaining}건 남음` : ''
)
</script>

<template>
  <div class="trust-badge">
    <img v-if="src" :src="src" :alt="`${role} 뱃지 ${level}단계`" :width="size" :height="size" />
    <span v-else class="no-badge" :style="{ width: `${size}px`, height: `${size}px` }">
      <CircleUser :size="size * 0.6" />
    </span>

    <p v-if="showRemaining" class="remaining">
      <template v-if="level === 0">이력 쌓는 중</template>
      <template v-else>{{ remainingText }}</template>
    </p>
  </div>
</template>

<style scoped>
.trust-badge {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-xs);
}
.no-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  background: var(--color-bg);
  color: var(--color-text-sub);
}
.remaining {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
</style>
