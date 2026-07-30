<script setup>
/**
 * 은행 선택 그리드 — 충전·출금 공용.
 * assets/images/banks 로고 + 이름으로 표시한다(로고 로드 실패 시 색칩으로 폴백).
 * 주요 은행은 그리드로 바로 노출하고, 그 밖의 은행은 "기타 은행" 시트에서 고른다.
 *
 * v-model 은 은행 코드(BANKS_ALL[].code) 문자열이다.
 */
import { computed, ref } from 'vue'

import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'
import { BANKS, BANKS_ALL, findBank } from '@/utils/constants'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '은행' }
})

const emit = defineEmits(['update:modelValue'])

const moreOpen = ref(false)

// 선택된 은행이 주요 그리드에 없으면(기타 은행) "기타 은행" 버튼에 이름을 노출한다.
const isPrimary = (code) => BANKS.some((b) => b.code === code)
const selectedExtra = computed(() =>
  props.modelValue && !isPrimary(props.modelValue) ? findBank(props.modelValue) : null
)

// 로고 로드 실패한 은행 코드 — 해당 은행만 색칩으로 폴백한다.
const failed = ref(new Set())
function onLogoError(code) {
  failed.value = new Set(failed.value).add(code)
}

function select(code) {
  emit('update:modelValue', code)
}
function selectFromSheet(code) {
  select(code)
  moreOpen.value = false
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
        @click="select(bank.code)"
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

    <!-- 기타 은행: 시트에서 전체 은행을 고른다. 기타 은행이 선택되면 이름을 노출한다. -->
    <button
      type="button"
      class="more"
      :class="{ 'is-active': !!selectedExtra }"
      @click="moreOpen = true"
    >
      <img
        v-if="selectedExtra && !failed.has(selectedExtra.code)"
        :src="selectedExtra.logo"
        :alt="`${selectedExtra.name} 로고`"
        class="logo"
        @error="onLogoError(selectedExtra.code)"
      />
      <span>{{ selectedExtra ? selectedExtra.name : '기타 은행' }}</span>
    </button>

    <BaseBottomSheet :open="moreOpen" title="은행 선택" @close="moreOpen = false">
      <div class="grid">
        <button
          v-for="bank in BANKS_ALL"
          :key="bank.code"
          type="button"
          class="bank"
          :class="{ 'is-active': modelValue === bank.code }"
          :aria-pressed="modelValue === bank.code"
          @click="selectFromSheet(bank.code)"
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
    </BaseBottomSheet>
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
.more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  width: 100%;
  margin-top: var(--space-sm);
  padding: var(--space-md);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
.more.is-active {
  border-style: solid;
  border-color: var(--color-owner);
  background: var(--color-owner-weak);
  color: var(--color-text);
  font-weight: var(--weight-medium);
}
</style>
