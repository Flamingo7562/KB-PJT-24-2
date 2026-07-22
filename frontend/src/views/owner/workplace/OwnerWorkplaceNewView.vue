<script setup>
/**
 * [D] 사업장 등록  ·  /owner/workplaces/new  ·  OWNER
 * 폼: 사업자등록번호·상호명·대표자명·사업장 주소·사업장 전화번호(주소→좌표 자동, 반경 기본 100).
 * 첫 로그인 시 사업장 0개면 가드 G7이 이 화면으로 강제 진입시킨다.
 * 연계 API: POST /workplaces  →  @/services/workplaces (createWorkplace)
 * 등록 성공 후: useWorkplaceStore().load({force:true}) 갱신 → /owner/home.
 * 공통: AppField · BaseButton · @/utils/validators (isBusinessNumber, isPhone)
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="사업장 등록" to="/owner/home" />
    <main class="screen-body">
      <EmptyState message="사업장 등록 화면 (TODO: 담당 D 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
