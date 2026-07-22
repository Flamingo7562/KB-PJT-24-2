<script setup>
/**
 * [B] 사장 충전  ·  /owner/wallet/charge  ·  OWNER
 * 은행·금액 선택 후 충전(Mock 승인). 사장 전용.
 * 연계 API: POST /wallet/charge  →  @/services/wallet (chargeWallet)
 * 공통: @/utils/constants BANKS(은행 목록·색칩) · AppField(금액) · BaseButton
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="충전" />
    <main class="screen-body">
      <EmptyState message="충전 화면 (TODO: 담당 B 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
