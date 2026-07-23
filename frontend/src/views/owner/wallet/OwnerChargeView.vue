<script setup>
/**
 * [B] 사장 충전  ·  /owner/wallet/charge  ·  OWNER
 * 은행·금액 선택 후 충전(Mock 승인). 사장 전용.
 * 연계 API: POST /wallet/charge  →  @/services/wallet (chargeWallet)
 * 공통: @/utils/constants BANKS(BankSelect) · WalletAmountField · BaseButton
 */
import { storeToRefs } from 'pinia'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BankSelect from '@/components/wallet/BankSelect.vue'
import WalletAmountField from '@/components/wallet/WalletAmountField.vue'
import { chargeWallet } from '@/services/wallet'
import { useUiStore } from '@/stores/ui'
import { useWalletStore } from '@/stores/wallet'
import { formatKRW } from '@/utils/format'
import { isPositiveAmount } from '@/utils/validators'

const router = useRouter()
const ui = useUiStore()
const walletStore = useWalletStore()
const { balance } = storeToRefs(walletStore)

const bankCode = ref('')
const amount = ref('')
const amountError = ref('')
const submitting = ref(false)

onMounted(() => {
  // 잔액은 안내용. 홈을 거치지 않고 진입했을 수 있어 없으면 로드한다.
  if (!balance.value) walletStore.loadWallet()
})

const amountCheck = computed(() => isPositiveAmount(amount.value))
const canSubmit = computed(() => !!bankCode.value && amountCheck.value.valid && !submitting.value)

async function onSubmit() {
  if (!bankCode.value) {
    ui.toast('은행을 선택해주세요.', { type: 'warning' })
    return
  }
  if (!amountCheck.value.valid) {
    amountError.value = amountCheck.value.message
    return
  }
  amountError.value = ''
  submitting.value = true
  try {
    // 서버가 금액을 최종 재검증한다(프론트 값 불신 — 추후 PortOne 교체 지점).
    await chargeWallet({ bankCode: bankCode.value, amount: Number(amount.value) })
    await walletStore.loadWallet()
    ui.toast(`${formatKRW(Number(amount.value))} 충전이 완료되었습니다.`, { type: 'success' })
    router.back()
  } catch {
    ui.toast('충전에 실패했습니다. 잠시 후 다시 시도해주세요.', { type: 'danger' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="충전" />
    <main class="screen-body">
      <p class="balance-line">
        가용 잔액 <strong>{{ formatKRW(balance) }}</strong>
      </p>

      <BankSelect v-model="bankCode" />

      <WalletAmountField v-model="amount" label="충전 금액" :error="amountError" />

      <BaseButton
        class="submit"
        variant="owner"
        size="lg"
        block
        :disabled="!canSubmit"
        @click="onSubmit"
      >
        {{ submitting ? '처리 중…' : '충전하기' }}
      </BaseButton>
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
  padding: var(--space-lg);
}
.balance-line {
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
.balance-line strong {
  margin-left: var(--space-xs);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.submit {
  margin-top: var(--space-sm);
}
</style>
