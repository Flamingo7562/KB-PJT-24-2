import { defineStore } from 'pinia'
import { ref } from 'vue'

import { listNotifications, markNotificationRead } from '@/services/notifications'

/**
 * 알림 상태 — 공통. 헤더 종 아이콘(안읽음 배지 + 모달 열기)과 NotificationModal 이 공유한다.
 */
export const useNotificationsStore = defineStore('notifications', () => {
  const items = ref([]) // [{ notificationId, notiType, title, content, isRead, createdAt }]
  const unreadCount = ref(0)
  const isOpen = ref(false)
  const loading = ref(false)

  async function load() {
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

  return { items, unreadCount, isOpen, loading, load, markRead, open, close }
})
