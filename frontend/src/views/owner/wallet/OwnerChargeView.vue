<script setup>
/**
 * [B] 사장 충전  ·  /owner/wallet/charge  ·  OWNER
 * PortOne 모의 결제창 — 은행·계좌번호·금액 입력 후 충전(Mock 승인). 사장 전용.
 * 연계 API: POST /wallet/funding-orders  →  @/services/wallet
 * 공통: BankSelect(은행) · AppField(계좌) · WalletAmountField · 승인 계좌/금액 검증
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BankSelect from '@/components/wallet/BankSelect.vue'
import WalletAmountField from '@/components/wallet/WalletAmountField.vue'
import { chargeWallet } from '@/services/wallet'
import { useUiStore } from '@/stores/ui'
import { useWalletStore } from '@/stores/wallet'
import { formatKRW } from '@/utils/format'
import { bankAccountRule, isWalletAmount, normalizeBankAccountNo } from '@/utils/validators'

const router = useRouter()
const ui = useUiStore()
const walletStore = useWalletStore()

const bankCode = ref('')
const accountNo = ref('')
const amount = ref('')
const accountError = ref('')
const amountError = ref('')
const submitting = ref(false)

const accountCheck = computed(() => bankAccountRule(accountNo.value))

const amountCheck = computed(() => isWalletAmount(amount.value))
const accountFieldError = computed(() =>
  accountNo.value && !accountCheck.value.valid ? accountCheck.value.message : accountError.value
)
const amountFieldError = computed(() =>
  amount.value && !amountCheck.value.valid ? amountCheck.value.message : amountError.value
)
const canSubmit = computed(
  () => !!bankCode.value && accountCheck.value.valid && amountCheck.value.valid && !submitting.value
)

async function onSubmit() {
  if (!bankCode.value) {
    ui.toast('은행을 선택해주세요.', { type: 'warning' })
    return
  }
  accountError.value = accountCheck.value.valid ? '' : accountCheck.value.message
  amountError.value = amountCheck.value.valid ? '' : amountCheck.value.message
  if (accountError.value || amountError.value) return

  submitting.value = true
  try {
    // 서버가 금액·계좌를 최종 재검증한다(프론트 값 불신 — 추후 PortOne 교체 지점).
    await chargeWallet({
      bankCode: bankCode.value,
      accountNo: normalizeBankAccountNo(accountNo.value),
      amount: Number(amount.value)
    })
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
      <BankSelect v-model="bankCode" label="충전 계좌 은행" />

      <AppField
        :model-value="accountNo"
        label="계좌번호"
        type="tel"
        placeholder="계좌번호 10~14자리"
        maxlength="20"
        :error="accountFieldError"
        @update:model-value="(v) => (accountNo = v)"
      />

      <WalletAmountField v-model="amount" label="충전 금액" :error="amountFieldError" />

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
.submit {
  margin-top: var(--space-sm);
}
</style>
