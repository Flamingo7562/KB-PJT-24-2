<script setup>
/**
 * [F] 알바생 홈(안심지갑)  ·  /worker/home  ·  WORKER  (탭 화면)
 * 안심지갑 잔액·출금(입금 없음) + 오늘의 알바 일정 카드(출근전/지각/노쇼/없음)
 * + 확보 안심금액(프로그레스바·지각 주황 구간·! 툴팁·예상 실수령액).
 * 확보 금액·차감 계산은 서버 응답(earning) 그대로 사용 — 프론트 재계산 금지.
 * 연계 API: GET /worker/home · POST /wallet/withdraw
 *   →  @/services/worker (getWorkerHome), @/services/wallet (withdrawWallet)
 * 공통: StatusChip(kind='today') · WalletBalanceCard(출금 전용으로 응용 가능)
 * 툴팁 문구: '휴게시간, 지각 등 특이사항이 있을 경우 실제 지급되는 금액은 상이할 수 있습니다.'
 */
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <EmptyState message="알바생 안심지갑(홈) 화면 (TODO: 담당 F 구현)" />
</template>
