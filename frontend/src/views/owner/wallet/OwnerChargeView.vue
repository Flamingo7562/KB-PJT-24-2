<script setup>
/**
 * [B] 사장 충전  ·  /owner/wallet/charge  ·  OWNER
 * 은행·금액 선택 후 충전(Mock 승인). 사장 전용.
 * 연계 API: POST /wallet/charge  →  @/services/wallet (chargeWallet)
 * 공통: BankSelect(BANKS) · AppField(계좌) · WalletAmountField · isPositiveAmount
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
import { isPositiveAmount } from '@/utils/validators'

const router = useRouter()
const ui = useUiStore()
const walletStore = useWalletStore()

const bankCode = ref('')
const accountNo = ref('')
const amount = ref('')
const accountError = ref('')
const amountError = ref('')
const submitting = ref(false)

/** 계좌번호: 숫자(하이픈 허용) 8자리 이상 — 출금과 동일 규칙 */
const accountCheck = computed(() => {
  const digits = accountNo.value.replace(/-/g, '')
  if (!digits) return { valid: false, message: '계좌번호를 입력해주세요.' }
  if (!/^\d{8,}$/.test(digits)) return { valid: false, message: '계좌번호를 정확히 입력해주세요.' }
  return { valid: true, message: '' }
})

// 충전은 금액 상한이 없다(양의 정수만) — 음수·비숫자는 입력 단계에서 차단되고 여기서 재검증.
const amountCheck = computed(() => isPositiveAmount(amount.value))
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
    // 서버가 금액을 최종 재검증한다(프론트 값 불신 — 추후 PortOne 교체 지점).
    await chargeWallet({
      bankCode: bankCode.value,
      accountNo: accountNo.value.replace(/-/g, ''),
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
      <BankSelect v-model="bankCode" label="출금 은행" />

      <AppField
        v-model="accountNo"
        label="계좌번호"
        placeholder="'-' 없이 숫자만 입력"
        :error="accountError"
      />

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
.submit {
  margin-top: var(--space-sm);
}
</style>
