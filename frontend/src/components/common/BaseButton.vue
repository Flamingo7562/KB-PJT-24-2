<script setup>
/**
 * 공통 버튼.
 *
 * variant: primary(다크) | owner(블루) | worker(앰버) | danger | secondary(외곽선) | ghost
 * block: 가로 100%  ·  size: md | lg
 * 아이콘은 기본 슬롯 앞에 넣으면 된다: <BaseButton><Plus :size="18"/> 충전</BaseButton>
 */
defineProps({
  variant: { type: String, default: 'primary' },
  type: { type: String, default: 'button' },
  block: { type: Boolean, default: false },
  size: { type: String, default: 'md' },
  disabled: { type: Boolean, default: false }
})
</script>

<template>
  <button
    :type="type"
    class="btn"
    :class="[`btn--${variant}`, `btn--${size}`, { 'btn--block': block }]"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  border-radius: var(--radius-sm);
  font-weight: var(--weight-medium);
  transition: opacity 0.15s;
}
.btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.btn:not(:disabled):active {
  opacity: 0.85;
}
.btn--block {
  width: 100%;
}

/* 크기 */
.btn--md {
  padding: var(--space-sm) var(--space-lg);
  font-size: var(--text-md);
}
.btn--lg {
  padding: var(--space-md) var(--space-lg);
  font-size: var(--text-lg);
}

/* 색 */
.btn--primary {
  background: var(--color-primary);
  color: var(--color-on-primary);
}
.btn--owner {
  background: var(--color-owner);
  color: var(--color-on-primary);
}
.btn--worker {
  background: var(--color-worker);
  color: var(--color-on-primary);
}
.btn--danger {
  background: var(--color-danger);
  color: var(--color-on-primary);
}
.btn--secondary {
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}
.btn--ghost {
  background: transparent;
  color: var(--color-text-sub);
}
</style>
