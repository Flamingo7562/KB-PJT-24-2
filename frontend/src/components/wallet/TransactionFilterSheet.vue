<script>
/**
 * 송금상세(거래내역) 필터 바텀시트 — /owner/home 에서 사용.
 *
 * 필터는 프론트에서 목록을 재계산하지 않고, 서버 파라미터로만 전달한다
 * (서버가 최종 권위 — docs/rules/domain.md). buildTransactionFilterParams 는
 * 빈 값/기본값을 제외한 GET /wallet/transactions 파라미터를 만든다.
 */
export const DEFAULT_FILTER = {
  keyword: '',
  txType: 'ALL',
  sort: 'LATEST',
  startDate: '',
  endDate: '',
  minAmount: '',
  maxAmount: ''
}

/** 필터 초안 → 서버 파라미터(빈 값·기본값 제외). 순수 함수라 단위 테스트 대상. */
export function buildTransactionFilterParams(draft = {}) {
  const f = { ...DEFAULT_FILTER, ...draft }
  const params = {}
  const keyword = String(f.keyword).trim()
  if (keyword) params.keyword = keyword
  if (f.txType && f.txType !== 'ALL') params.txType = f.txType
  if (f.sort) params.sort = f.sort
  if (f.startDate) params.startDate = f.startDate
  if (f.endDate) params.endDate = f.endDate
  if (f.minAmount !== '' && Number(f.minAmount) >= 0) params.minAmount = Number(f.minAmount)
  if (f.maxAmount !== '' && Number(f.maxAmount) >= 0) params.maxAmount = Number(f.maxAmount)
  return params
}
</script>

<script setup>
import { reactive, watch } from 'vue'

import AppField from '@/components/common/AppField.vue'
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { TX_SORT, TX_TYPE_FILTER } from '@/utils/constants'

const props = defineProps({
  open: { type: Boolean, default: false },
  // 현재 적용 중인 필터(시트를 다시 열 때 초안 복원용)
  modelValue: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['close', 'apply'])

const draft = reactive({ ...DEFAULT_FILTER })

// 시트가 열릴 때마다 현재 적용값으로 초안을 맞춘다.
watch(
  () => props.open,
  (open) => {
    if (open) Object.assign(draft, DEFAULT_FILTER, props.modelValue)
  }
)

function onReset() {
  Object.assign(draft, DEFAULT_FILTER)
}

function onApply() {
  emit('apply', buildTransactionFilterParams(draft))
  emit('close')
}
</script>

<template>
  <BaseBottomSheet :open="open" title="검색·필터" @close="emit('close')">
    <div class="filter-body">
      <AppField v-model="draft.keyword" label="검색어" placeholder="내용·설명 검색" />

      <div class="group">
        <p class="group-label">유형</p>
        <div class="chips">
          <button
            v-for="opt in TX_TYPE_FILTER"
            :key="opt.value"
            type="button"
            class="chip"
            :class="{ 'is-active': draft.txType === opt.value }"
            @click="draft.txType = opt.value"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>

      <div class="group">
        <p class="group-label">정렬</p>
        <div class="chips">
          <button
            v-for="opt in TX_SORT"
            :key="opt.value"
            type="button"
            class="chip"
            :class="{ 'is-active': draft.sort === opt.value }"
            @click="draft.sort = opt.value"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>

      <div class="group">
        <p class="group-label">기간</p>
        <div class="row">
          <AppField v-model="draft.startDate" type="date" />
          <span class="tilde">~</span>
          <AppField v-model="draft.endDate" type="date" />
        </div>
      </div>

      <div class="group">
        <p class="group-label">금액 범위</p>
        <div class="row">
          <AppField v-model="draft.minAmount" type="number" placeholder="최소" />
          <span class="tilde">~</span>
          <AppField v-model="draft.maxAmount" type="number" placeholder="최대" />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="actions">
        <BaseButton variant="secondary" @click="onReset">초기화</BaseButton>
        <BaseButton variant="owner" block @click="onApply">적용</BaseButton>
      </div>
    </template>
  </BaseBottomSheet>
</template>

<style scoped>
.filter-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.group-label {
  margin-bottom: var(--space-sm);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}
.chip {
  padding: var(--space-xs) var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.chip.is-active {
  border-color: var(--color-owner);
  background: var(--color-owner-weak);
  color: var(--color-owner);
  font-weight: var(--weight-medium);
}
.row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.row > .field {
  flex: 1;
}
.tilde {
  color: var(--color-text-sub);
}
.actions {
  display: flex;
  gap: var(--space-sm);
}
</style>
