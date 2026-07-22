<script setup>
/**
 * [B] 사장 출금  ·  /owner/wallet/withdraw  ·  OWNER
 * 출금 대상(은행·계좌)·금액 지정. 가용 잔액 내에서만(초과 400).
 * 연계 API: POST /wallet/withdraw  →  @/services/wallet (withdrawWallet)
 * 공통: BANKS · AppField · @/utils/validators (isPositiveAmount)
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="출금" />
    <main class="screen-body">
      <EmptyState message="출금 화면 (TODO: 담당 B 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
