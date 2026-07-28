import { defineStore } from 'pinia'
import { ref } from 'vue'

import { listNotifications, markNotificationRead } from '@/services/notifications'

/**
 * Schema Gap — notifications 테이블 부재로 알림 기능은 보류(docs/rules/routing.md, frontend.md).
 * enabled=false 인 동안 진입점은 '준비 중'으로만 노출하고 API(listNotifications 등)는 호출하지 않는다.
 * 스키마·API 가 준비되면 이 플래그만 true 로 바꾸면 아래 로직이 그대로 살아난다.
 */
const NOTIFICATIONS_ENABLED = false

/**
 * 알림 상태 — 공통. 헤더 종 아이콘(안읽음 배지 + 모달 열기)과 NotificationModal 이 공유한다.
 */
export const useNotificationsStore = defineStore('notifications', () => {
  const items = ref([]) // [{ notificationId, notiType, title, content, isRead, createdAt }]
  const unreadCount = ref(0)
  const isOpen = ref(false)
  const loading = ref(false)
  const enabled = ref(NOTIFICATIONS_ENABLED)

  async function load() {
    if (!enabled.value) return // Schema Gap: 호출 금지
    loading.value = true
    try {
      const data = await listNotifications()
      items.value = data.content
      unreadCount.value = data.unreadCount ?? items.value.filter((n) => !n.isRead).length
    } finally {
      loading.value = false
    }
  }

  async function markRead(notificationId) {
    if (!enabled.value) return // Schema Gap: 호출 금지
    await markNotificationRead(notificationId)
    const n = items.value.find((x) => x.notificationId === notificationId)
    if (n && !n.isRead) {
      n.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  function open() {
    isOpen.value = true
    load()
  }
  function close() {
    isOpen.value = false
  }

  return { items, unreadCount, isOpen, loading, enabled, load, markRead, open, close }
})
