/**
 * 알림 API 서비스 — 공통. 헤더 종 아이콘 → 알림 모달.
 *
 * 알림 발생: 근무 확정·예치·정산·환불·보건증 공유·임금분쟁 신고 접수.
 *
 * 관련 API(명세 45~46):
 *   GET /api/notifications   PATCH /api/notifications/{notificationId}/read
 */
import http from '@/services/http'

const USE_MOCK = true

const mockNotifications = [
  {
    notificationId: 5,
    notiType: 'SETTLED',
    title: '정산 완료',
    content: '강남점 주말 홀 서빙 임금이 정산되었습니다.',
    isRead: false,
    createdAt: '2026-07-22T18:05:00'
  },
  {
    notificationId: 4,
    notiType: 'SHIFT_CONFIRMED',
    title: '근무 확정',
    content: '이알바님이 근로계약서에 날인했습니다.',
    isRead: false,
    createdAt: '2026-07-21T11:30:00'
  },
  {
    notificationId: 3,
    notiType: 'DOC_SHARED',
    title: '보건증 공유',
    content: '이알바님이 보건증을 공유했습니다.',
    isRead: true,
    createdAt: '2026-07-20T10:01:00'
  }
]

/** 알림 목록 조회 → { content[], unreadCount } (명세 45) */
export async function listNotifications(params = {}) {
  if (USE_MOCK) {
    const content = mockNotifications.map((n) => ({ ...n }))
    return { content, unreadCount: content.filter((n) => !n.isRead).length }
  }
  const { data } = await http.get('/notifications', { params })
  return data
}

/** 알림 읽음 처리 (명세 46) */
export async function markNotificationRead(notificationId) {
  if (USE_MOCK) return
  await http.patch(`/notifications/${notificationId}/read`)
}
