<script setup>
/**
 * 로그인·회원가입 화면 상단 역할 토글(사장님/알바생).
 * 선택 시 해당 역할 화면으로 이동은 부모가 처리한다(이 컴포넌트는 선택만 알림).
 *
 * <AuthRoleToggle :model-value="role" @update:modelValue="onChangeRole" />
 */
defineProps({
  modelValue: { type: String, required: true } // 'OWNER' | 'WORKER'
})

defineEmits(['update:modelValue'])
</script>

<template>
  <div class="role-toggle" role="tablist" aria-label="역할 선택">
    <button
      type="button"
      class="role"
      :class="{ active: modelValue === 'OWNER' }"
      @click="$emit('update:modelValue', 'OWNER')"
    >
      사장님
    </button>
    <button
      type="button"
      class="role"
      :class="{ active: modelValue === 'WORKER' }"
      @click="$emit('update:modelValue', 'WORKER')"
    >
      알바생
    </button>
  </div>
</template>

<style scoped>
.role-toggle {
  display: flex;
  gap: var(--space-sm);
}
.role {
  flex: 1;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
  background: var(--color-surface);
}
.role.active {
  border-color: var(--color-primary);
  color: var(--color-text);
}
</style>
