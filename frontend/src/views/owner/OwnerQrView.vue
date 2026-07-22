<script setup>
/**
 * [C] 사장 QR  ·  /owner/qr  ·  OWNER  (탭 화면)
 * 선택 지점 동적 QR 표시(30~60초 주기 갱신).
 * 지점 컨텍스트: useWorkplaceStore().selectedId 기준.
 * 연계 API: GET /workplaces/{id}/qr → { qrToken, expiresAt }
 *   →  (서비스 함수 추가 위치: @/services/shifts 또는 workplaces — 담당이 확정)
 * TODO(담당 C): QR 렌더링 라이브러리 선정 후 주기 재발급 타이머.
 */
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <EmptyState message="사장 QR 화면 (TODO: 담당 C 구현)" />
</template>
