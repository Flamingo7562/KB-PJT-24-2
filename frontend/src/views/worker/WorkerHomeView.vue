<script setup>
/**
 * [F] 알바생 홈(안심지갑)  ·  /worker/home  ·  WORKER  (탭 화면)
 * 안심지갑 잔액·출금(입금 없음) + 오늘의 알바 일정 카드(출근전/지각/노쇼/없음)
 * + 확보 안심금액(프로그레스바·지각 주황 구간·! 툴팁·예상 실수령액).
 * 확보 금액·차감 계산은 서버 응답(earning) 그대로 사용 — 프론트 재계산 금지.
 * 연계 API: GET /worker/home  →  @/stores/workerHome (loadHome)
 * 출금 버튼 → /worker/wallet/withdraw (사장 출금 화면과 동일한 별도 화면 흐름).
 */
import { storeToRefs } from 'pinia'
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import SecuredEarningCard from '@/components/worker/SecuredEarningCard.vue'
import TodayShiftCard from '@/components/worker/TodayShiftCard.vue'
import WorkerWalletCard from '@/components/worker/WorkerWalletCard.vue'
import { useWorkerHomeStore } from '@/stores/workerHome'

const router = useRouter()
const homeStore = useWorkerHomeStore()
const { balance, todayShift, earning, loading, error } = storeToRefs(homeStore)

// 확보 안심금액은 오늘 진행 중인 근무가 있을 때만 노출(없음/미배정이면 숨김).
const showEarning = computed(
  () => !!earning.value && !!todayShift.value && todayShift.value.status !== 'NONE'
)

onMounted(() => {
  homeStore.loadHome()
})

const goWithdraw = () => router.push('/worker/wallet/withdraw')
</script>

<template>
  <div class="worker-home">
    <EmptyState v-if="error" message="홈 정보를 불러오지 못했습니다." />

    <template v-else>
      <WorkerWalletCard :balance="balance" @withdraw="goWithdraw" />

      <TodayShiftCard :shift="todayShift" />

      <SecuredEarningCard v-if="showEarning" :earning="earning" />

      <p v-if="loading" class="loading">불러오는 중…</p>
    </template>
  </div>
</template>

<style scoped>
.loading {
  margin-top: var(--space-md);
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
</style>
