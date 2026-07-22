import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 전역 UI 상태 — 토스트 큐.
 *
 * 라우터 가드(G2 역할 안내 등)와 화면 어디서든 `ui.toast('메시지')` 로 호출한다.
 * 렌더링은 App.vue 의 <ToastHost/> 가 담당한다.
 */
export const useUiStore = defineStore('ui', () => {
  const toasts = ref([]) // [{ id, message, type }]
  let seq = 0

  /**
   * 토스트 표시.
   * @param {string} message
   * @param {object} options type('info'|'success'|'warning'|'danger'), duration(ms)
   */
  function toast(message, { type = 'info', duration = 2500 } = {}) {
    const id = ++seq
    toasts.value.push({ id, message, type })
    if (duration > 0) setTimeout(() => remove(id), duration)
    return id
  }

  function remove(id) {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }

  return { toasts, toast, remove }
})
