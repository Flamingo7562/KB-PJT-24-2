<script setup>
/**
 * [F] 임금분쟁 신고  ·  /worker/work/shifts/:shiftId/report  ·  WORKER(본인 근무)
 * 경위서 작성·제출. 기록·알림용 — 정산 영향 없음. 제출 시 사장 알림(WAGE_REPORTED).
 * 연계 API: POST /worker/shifts/{id}/reports  →  @/services/shifts (createReport)
 * route.params.shiftId 사용. 공통: BaseButton · 제출 후 useUiStore().toast + 뒤로가기.
 */
import { Info } from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { createReport } from '@/services/shifts'
import { useUiStore } from '@/stores/ui'

const MIN_LENGTH = 10

const route = useRoute()
const router = useRouter()
const ui = useUiStore()

const shiftId = route.params.shiftId
const content = ref('')
const submitting = ref(false)

const trimmedLength = computed(() => content.value.trim().length)
const canSubmit = computed(() => trimmedLength.value >= MIN_LENGTH && !submitting.value)

async function onSubmit() {
  if (!canSubmit.value) {
    ui.toast(`경위서를 ${MIN_LENGTH}자 이상 작성해주세요.`, { type: 'warning' })
    return
  }

  submitting.value = true
  try {
    await createReport(shiftId, { content: content.value.trim() })
    ui.toast('신고가 접수되었습니다.', { type: 'success' })
    router.back()
  } catch {
    ui.toast('신고 접수에 실패했습니다. 잠시 후 다시 시도해주세요.', { type: 'danger' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="임금분쟁 신고" />
    <main class="screen-body">
      <p class="notice">
        <Info :size="16" />
        신고는 <strong>기록·알림용</strong>이며 정산 진행에는 영향을 주지 않습니다. 상황을 최대한
        구체적으로 작성해주세요.
      </p>

      <label class="field">
        <span class="label">경위서</span>
        <textarea
          v-model="content"
          class="textarea"
          rows="10"
          placeholder="언제, 어떤 임금 문제가 있었는지 구체적으로 작성해주세요."
        ></textarea>
        <span class="counter">{{ trimmedLength }}자 (최소 {{ MIN_LENGTH }}자)</span>
      </label>

      <BaseButton
        class="submit"
        variant="danger"
        size="lg"
        block
        :disabled="!canSubmit"
        @click="onSubmit"
      >
        {{ submitting ? '접수 중…' : '신고 제출' }}
      </BaseButton>
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
  padding: var(--space-lg);
}
.notice {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-md);
  background: var(--color-warning-bg);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
  line-height: 1.5;
}
.notice strong {
  color: var(--color-text);
  font-weight: var(--weight-medium);
}
.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.label {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
}
.textarea {
  width: 100%;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  resize: vertical;
  line-height: 1.6;
}
.textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}
.counter {
  align-self: flex-end;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.submit {
  margin-top: var(--space-sm);
}
</style>
