<script setup>
/**
 * [D] 사업장 관리  ·  /owner/mypage/workplaces  ·  OWNER
 * 사업장 목록·추가(+)·수정·삭제. 진행 중 근무 있는 지점 삭제 제한(409).
 * 연계 API: GET/POST /workplaces · PATCH/DELETE /workplaces/{id}
 *   →  @/services/workplaces (list, create, update, remove)
 * 변경 후 useWorkplaceStore().load({force:true}) 로 네비 select 갱신.
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="사업장 관리" />
    <main class="screen-body">
      <EmptyState message="사업장 관리 화면 (TODO: 담당 D 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
