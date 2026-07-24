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
 */
import { useId } from 'vue'

defineProps({
  label: { type: String, default: '' },
  modelValue: { type: [String, Number], default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  error: { type: String, default: '' },
  hint: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  maxlength: { type: [String, Number], default: null }
})

defineEmits(['update:modelValue'])

const fieldId = useId()
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
        class="input"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :maxlength="maxlength"
        @input="$emit('update:modelValue', $event.target.value)"
      />
      <div v-if="$slots.suffix" class="suffix"><slot name="suffix" /></div>
    </div>

    <p v-if="error" class="msg error">{{ error }}</p>
    <p v-else-if="hint" class="msg hint">{{ hint }}</p>
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
.msg.hint {
  color: var(--color-text-sub);
}
</style>
