<script setup>
/**
 * 중앙 모달 — 확인·경고 등 짧은 다이얼로그.
 * 리스트성 시트는 BaseBottomSheet 를, 확인/경고는 이 컴포넌트를 쓴다.
 *
 * 사용:
 *   <BaseModal :open="open" title="삭제할까요?" @close="open=false">
 *     본문...
 *     <template #footer>...버튼...</template>
 *   </BaseModal>
 */
import { X } from 'lucide-vue-next'

defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  closable: { type: Boolean, default: true }
})

const emit = defineEmits(['close'])
</script>

<template>
  <Teleport to="body">
    <Transition name="base-modal">
      <div v-if="open" class="modal-overlay" @click.self="closable && emit('close')">
        <div class="base-modal" role="dialog" aria-modal="true">
          <header v-if="title || closable" class="modal-head">
            <h2 class="base-modal-title">{{ title }}</h2>
            <button
              v-if="closable"
              type="button"
              class="close"
              aria-label="닫기"
              @click="emit('close')"
            >
              <X :size="20" />
            </button>
          </header>

          <div class="base-modal-body"><slot /></div>

          <footer v-if="$slots.footer" class="base-modal-footer">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: var(--z-overlay);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-xl);
  background: var(--color-overlay);
}
.base-modal {
  width: 100%;
  max-width: 360px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sheet);
  overflow: hidden;
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-lg) var(--space-lg) 0;
}
.base-modal-title {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.close {
  color: var(--color-text-sub);
}
.base-modal-body {
  padding: var(--space-lg);
  color: var(--color-text);
}
.base-modal-footer {
  display: flex;
  gap: var(--space-sm);
  padding: 0 var(--space-lg) var(--space-lg);
}

.base-modal-enter-active,
.base-modal-leave-active {
  transition: opacity 0.18s ease;
}
.base-modal-enter-from,
.base-modal-leave-to {
  opacity: 0;
}
</style>
