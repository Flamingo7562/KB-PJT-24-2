<script setup>
/**
 * [G] 알바생 문서 뷰어  ·  /worker/documents/:documentId  ·  WORKER(소유자)
 * 이미지·PDF 인앱 열람 + 다운로드. 보건증 만료 예정일(발급일+1년) 표시.
 * 연계 API: GET /documents/{id}/file  →  @/services/documents (documentFileUrl)
 * route.params.documentId 사용. 공통: @/utils/format (formatDate).
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="문서 보기" />
    <main class="screen-body">
      <EmptyState message="알바생 문서 뷰어 화면 (TODO: 담당 G 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
