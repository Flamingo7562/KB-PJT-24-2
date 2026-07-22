<script setup>
/**
 * [G] 알바생 문서함  ·  /worker/documents  ·  WORKER  (탭 화면)
 * 근로계약서(자동 저장) + 보건증(업로드·발급일 수정·삭제·공유·공유 취소).
 * 업로드=보건증만. 계약서 삭제=근무 종료 후. 카드에 공유중 지점 표시.
 * 연계 API: GET/POST/PATCH/DELETE /documents · GET/POST/DELETE /documents/{id}/shares
 *   →  @/services/documents (전부)
 * 공통: BaseBottomSheet(보건증 등록/공유 모달) · 카드 클릭 → /worker/documents/:documentId
 */
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <EmptyState message="알바생 문서함 화면 (TODO: 담당 G 구현)" />
</template>
