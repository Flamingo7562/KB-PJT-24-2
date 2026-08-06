<script setup>
/**
 * 폼 입력 필드 — 라벨 + 입력 + 에러/힌트.
 * 회원가입·사업장 등록·비밀번호 변경 등 모든 폼에서 공용.
 *
 * v-model 지원: <AppField v-model="email" label="이메일" type="email" :error="emailError" />
 * suffix 슬롯: 입력 우측에 버튼 등을 붙인다(아이디·이메일 중복확인).
 *   <AppField v-model="loginId" label="아이디">
 *     <template #suffix><BaseButton @click="check">중복확인</BaseButton></template>
 *   </AppField>
 *
 * IME(한글): 조합 중에는 값을 올리지 않고 조합이 끝날 때 한 번만 올린다.
 * digits-only 필드는 조합 시작 즉시 조합을 취소해 한글이 화면에 찍히지 않게 한다.
 */
import { nextTick, ref, useId } from 'vue'

const props = defineProps({
  label: { type: String, default: '' },
  modelValue: { type: [String, Number], default: '' },
  type: { type: String, default: 'text' },
  inputmode: { type: String, default: null },
  // 숫자 전용 필드(사업자등록번호·전화번호) — IME 차단 + inputmode="numeric"
  digitsOnly: { type: Boolean, default: false },
  placeholder: { type: String, default: '' },
  error: { type: String, default: '' },
  hint: { type: String, default: '' },
  // 중복확인처럼 서버에 물어야만 알 수 있는 결과만 성공으로 표시한다.
  success: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  maxlength: { type: [String, Number], default: null }
})

const emit = defineEmits(['update:modelValue', 'blur'])

const fieldId = useId()
// 에러·성공·힌트 메시지(셋 중 하나만 보인다)의 id. aria-describedby 로 input 과 이어서
// 스크린리더가 값과 함께 이 메시지를 읽게 한다.
const messageId = `${fieldId}-msg`
const inputEl = ref(null)
const composing = ref(false)
// 조합 취소용 내부 blur 인지 표시한다. 이 구간의 blur 는 사용자가 필드를 떠난 것이
// 아니므로 부모에게 올리지 않는다.
const cancelingComposition = ref(false)

// 부모가 정제한 값이 직전 값과 같으면 Vue 가 DOM 을 패치하지 않아 입력 문자가 남는다.
// 그래서 DOM 값을 모델값에 직접 맞춘다.
async function syncDomValue() {
  await nextTick()
  const el = inputEl.value
  if (!el) return
  const next = String(props.modelValue ?? '')
  if (el.value !== next) {
    el.value = next
    el.setSelectionRange(next.length, next.length)
  }
}

function onInput(e) {
  if (composing.value) return
  emit('update:modelValue', e.target.value)
  if (props.digitsOnly) syncDomValue()
}

function onBlur(e) {
  if (cancelingComposition.value) return
  emit('blur', e)
}

function onCompositionStart(e) {
  if (props.digitsOnly) {
    // keydown preventDefault 로는 IME 가 막히지 않는다. 포커스를 끊었다 되돌려 조합을 취소한다.
    const el = e.target
    cancelingComposition.value = true
    el.blur()
    el.focus()
    cancelingComposition.value = false
    syncDomValue()
    return
  }
  composing.value = true
}

function onCompositionEnd(e) {
  composing.value = false
  emit('update:modelValue', e.target.value)
  syncDomValue()
}
</script>

<template>
  <div class="field" :class="{ 'has-error': error }">
    <label v-if="label" :for="fieldId" class="label">
      {{ label }}
      <span v-if="required" class="req" aria-hidden="true">*</span>
    </label>

    <div class="input-row">
      <input
        :id="fieldId"
        ref="inputEl"
        class="input"
        :type="type"
        :inputmode="inputmode ?? (digitsOnly ? 'numeric' : null)"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :maxlength="maxlength"
        :aria-describedby="error || success || hint ? messageId : undefined"
        :aria-invalid="error ? 'true' : undefined"
        @input="onInput"
        @compositionstart="onCompositionStart"
        @compositionend="onCompositionEnd"
        @blur="onBlur"
      />
      <div v-if="$slots.suffix" class="suffix"><slot name="suffix" /></div>
    </div>

    <p v-if="error" :id="messageId" class="msg error" role="alert">{{ error }}</p>
    <p v-else-if="success" :id="messageId" class="msg success">{{ success }}</p>
    <p v-else-if="hint" :id="messageId" class="msg hint">{{ hint }}</p>
  </div>
</template>

<style scoped>
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
.req {
  color: var(--color-danger);
}
.input-row {
  display: flex;
  gap: var(--space-sm);
}
.input {
  flex: 1;
  min-width: 0;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
}
.input:focus {
  outline: none;
  border-color: var(--color-primary);
}
.input:disabled {
  color: var(--color-text-sub);
  background: var(--color-bg);
  cursor: not-allowed;
}
.field.has-error .input {
  border-color: var(--color-danger);
}
.suffix {
  flex-shrink: 0;
  display: flex;
  align-items: stretch;
}
.msg {
  font-size: var(--text-sm);
}
.msg.error {
  color: var(--color-danger);
}
.msg.success {
  color: var(--color-success);
}
.msg.hint {
  color: var(--color-text-sub);
}
</style>
