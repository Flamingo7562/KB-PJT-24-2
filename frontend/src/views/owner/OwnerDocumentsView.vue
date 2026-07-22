<script setup>
/**
 * [D] 사장 문서함  ·  /owner/documents  ·  OWNER  (탭 화면)
 * 지점 문서: 자동 생성 계약서 + 직접 업로드(계약서 스캔) + 공유받은 보건증(읽기 전용).
 * 지점 컨텍스트: useWorkplaceStore().selectedId 로 workplaceId 전달.
 * 연계 API: GET /documents?workplaceId · POST /documents · DELETE /documents/{id}
 *   →  @/services/documents (listDocuments, uploadDocument, deleteDocument)
 * 규칙: 계약서 삭제는 근무 종료 후만. 보건증 직접 업로드 없음.
 * 공통: 카드 클릭 → /owner/documents/:documentId
 */
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <EmptyState message="사장 문서함 화면 (TODO: 담당 D 구현)" />
</template>
