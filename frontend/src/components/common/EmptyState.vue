<script setup>
/**
 * 빈 상태 / 자리표시 안내.
 * 리스트가 비었을 때, 혹은 미구현 화면 stub 의 안내 문구로 사용한다.
 * 아이콘은 icon 슬롯으로 넣을 수 있다(선택).
 */
defineProps({
  message: { type: String, default: '표시할 내용이 없습니다.' }
})
</script>

<template>
  <div class="empty">
    <div v-if="$slots.icon" class="empty-icon"><slot name="icon" /></div>
    <p class="empty-msg">{{ message }}</p>
    <p v-if="$slots.default" class="empty-sub"><slot /></p>
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xl) var(--space-lg);
  text-align: center;
}
.empty-icon {
  color: var(--color-text-sub);
}
.empty-msg {
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
.empty-sub {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
</style>
