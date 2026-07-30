<script setup>
/**
 * 로그인·회원가입·온보딩의 역할 토글(사장님/알바생).
 * 선택 시 해당 역할 화면으로 이동은 부모가 처리한다(이 컴포넌트는 선택만 알림).
 *
 * <AuthRoleToggle :model-value="role" @update:modelValue="onChangeRole" />
 *
 * 선택 표시는 버튼 배경이 아니라 밑에 깔린 thumb 한 장이 좌우로 미끄러지는 방식이다.
 * 색도 역할별(사장=블루, 알바생=앰버)로 함께 바뀐다.
 */
defineProps({
  modelValue: { type: String, required: true } // 'OWNER' | 'WORKER'
})

defineEmits(['update:modelValue'])
</script>

<template>
  <div class="role-toggle" role="tablist" aria-label="역할 선택">
    <span
      class="thumb"
      :class="modelValue === 'OWNER' ? 'is-owner' : 'is-worker'"
      aria-hidden="true"
    />
    <button
      type="button"
      role="tab"
      class="role"
      :class="{ active: modelValue === 'OWNER' }"
      :aria-selected="modelValue === 'OWNER'"
      @click="$emit('update:modelValue', 'OWNER')"
    >
      사장님
    </button>
    <button
      type="button"
      role="tab"
      class="role"
      :class="{ active: modelValue === 'WORKER' }"
      :aria-selected="modelValue === 'WORKER'"
      @click="$emit('update:modelValue', 'WORKER')"
    >
      알바생
    </button>
  </div>
</template>

<style scoped>
.role-toggle {
  position: relative;
  display: flex;
  padding: 4px;
  background: var(--color-bg);
  border-radius: var(--radius-pill);
}

/* 선택 표시 — 두 버튼 밑에 깔려 좌우로 미끄러진다. 폭은 트랙 안쪽의 절반. */
.thumb {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  border-radius: var(--radius-pill);
  background: var(--color-owner);
  transition:
    transform 0.25s ease,
    background-color 0.25s ease;
}
.thumb.is-worker {
  transform: translateX(100%);
  background: var(--color-worker);
}

.role {
  position: relative; /* thumb 위로 올려 글자가 가리지 않게 한다 */
  z-index: 1;
  flex: 1;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-pill);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
  transition: color 0.25s ease;
}
.role.active {
  color: var(--color-on-primary);
}

/* 모션 최소화 설정을 켠 사용자에게는 미끄러지는 연출을 끈다. */
@media (prefers-reduced-motion: reduce) {
  .thumb,
  .role {
    transition: none;
  }
}
</style>
