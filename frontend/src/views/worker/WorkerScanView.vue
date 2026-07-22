<script setup>
/**
 * [F] 알바생 QR 스캔(탭)  ·  /worker/scan  ·  WORKER  (탭 화면)
 * 카메라 스캔 → GPS 검증 → 출근/퇴근 자동 판별·기록(단일 스캔 API).
 * 카메라·위치 권한 필요.
 * 연계 API: POST /worker/scan  →  @/services/worker (scan)
 *   요청: { qrToken, latitude, longitude } / 응답: scanType(CHECK_IN/CHECK_OUT), isLate, ...
 * TODO(담당 F): 카메라·QR 디코딩 라이브러리 + navigator.geolocation.
 */
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <EmptyState message="알바생 QR 스캔 화면 (TODO: 담당 F 구현)" />
</template>
