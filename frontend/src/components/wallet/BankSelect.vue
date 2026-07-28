<script setup>
/**
 * 은행 선택 그리드 — 충전·출금 공용.
 * assets/images/banks 로고 + 이름으로 표시한다(로고 로드 실패 시 색칩으로 폴백).
 *
 * v-model 은 은행 코드(BANKS[].code) 문자열이다.
 */
import { ref } from 'vue'

import { BANKS } from '@/utils/constants'

defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '은행' }
})

defineEmits(['update:modelValue'])

// 로고 로드 실패한 은행 코드 — 해당 은행만 색칩으로 폴백한다.
const failed = ref(new Set())
function onLogoError(code) {
  failed.value = new Set(failed.value).add(code)
}
</script>

<template>
  <div class="bank-select">
    <p v-if="label" class="label">{{ label }}</p>
    <div class="grid">
      <button
        v-for="bank in BANKS"
        :key="bank.code"
        type="button"
        class="bank"
        :class="{ 'is-active': modelValue === bank.code }"
        :aria-pressed="modelValue === bank.code"
        @click="$emit('update:modelValue', bank.code)"
      >
        <img
          v-if="bank.logo && !failed.has(bank.code)"
          :src="bank.logo"
          :alt="`${bank.name} 로고`"
          class="logo"
          @error="onLogoError(bank.code)"
        />
        <span v-else class="dot" :style="{ background: bank.chip }" />
        <span class="name">{{ bank.name }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.label {
  margin-bottom: var(--space-sm);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}
.bank {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  font-size: var(--text-md);
  color: var(--color-text);
}
.bank.is-active {
  border-color: var(--color-owner);
  background: var(--color-owner-weak);
  font-weight: var(--weight-medium);
}
.logo {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  object-fit: contain;
  border-radius: var(--radius-xs, 4px);
}
.dot {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  border-radius: var(--radius-pill);
}
.name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
