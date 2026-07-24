<script setup>
/**
 * 금액 입력 필드 — 충전·출금 공용.
 * 숫자만 입력받고, 아래에 빠른 금액 칩(+1만 등)과 선택적 '전액' 버튼을 제공한다.
 * 입력값은 원 단위 정수 문자열로 v-model 된다(검증은 상위에서 isPositiveAmount 로).
 */
import { computed } from 'vue'

import AppField from '@/components/common/AppField.vue'
import { formatKRW } from '@/utils/format'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '금액' },
  error: { type: String, default: '' },
  placeholder: { type: String, default: '금액을 입력하세요' },
  quickAmounts: { type: Array, default: () => [10000, 50000, 100000, 1000000] },
  // '전액' 버튼에 채울 금액(출금 화면용). null 이면 버튼을 숨긴다.
  fillAmount: { type: Number, default: null },
  // 빠른금액 칩의 상한(출금 화면의 가용 잔액). null 이면 상한 없음(충전).
  max: { type: Number, default: null }
})

const emit = defineEmits(['update:modelValue'])

const numeric = computed(() => Number(props.modelValue) || 0)
// 입력한 금액을 읽기 쉽게 다시 보여주는 힌트(에러가 있으면 AppField 가 에러를 우선 표시).
const hint = computed(() => (numeric.value > 0 ? formatKRW(numeric.value) : ''))

/** 만 단위면 "+N만", 아니면 "+N원" */
function quickLabel(v) {
  return v % 10000 === 0 ? `+${v / 10000}만` : `+${v.toLocaleString('ko-KR')}`
}

function setAmount(v) {
  let next = Math.max(0, Math.floor(v))
  // 빠른금액 칩/전액은 상한(가용 잔액)을 넘지 않게 자른다. 직접 타이핑(onInput)은 자르지 않고
  // 상위에서 초과 에러로 안내한다(타이핑 도중 값이 튀는 것을 막기 위함).
  if (props.max != null) next = Math.min(next, props.max)
  emit('update:modelValue', String(next))
}

function addAmount(v) {
  setAmount(numeric.value + v)
}

/** 숫자 외 문자는 제거하고 반영 */
function onInput(v) {
  emit('update:modelValue', String(v).replace(/[^\d]/g, ''))
}
</script>

<template>
  <div class="amount-field">
    <AppField
      :label="label"
      :model-value="modelValue"
      type="text"
      :placeholder="placeholder"
      :error="error"
      :hint="hint"
      @update:model-value="onInput"
    />

    <div class="quick">
      <button v-for="q in quickAmounts" :key="q" type="button" class="chip" @click="addAmount(q)">
        {{ quickLabel(q) }}
      </button>
      <button
        v-if="fillAmount != null"
        type="button"
        class="chip chip--fill"
        @click="setAmount(fillAmount)"
      >
        전액
      </button>
    </div>
  </div>
</template>

<style scoped>
.quick {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
}
.chip {
  padding: var(--space-xs) var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.chip:active {
  opacity: 0.85;
}
.chip--fill {
  border-color: var(--color-owner);
  color: var(--color-owner);
  font-weight: var(--weight-medium);
}
</style>
