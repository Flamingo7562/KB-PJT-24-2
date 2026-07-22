<script setup>
/**
 * [D] 사장 문서 뷰어  ·  /owner/documents/:documentId  ·  OWNER(소유/공유 수신)
 * 이미지·PDF 인앱 열람 + 다운로드. 공유받은 보건증은 발급일·만료 예정일 표시.
 * 연계 API: GET /documents/{id}/file  →  @/services/documents (documentFileUrl)
 * route.params.documentId 사용.
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="문서 보기" />
    <main class="screen-body">
      <EmptyState message="사장 문서 뷰어 화면 (TODO: 담당 D 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
