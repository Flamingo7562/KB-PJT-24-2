<script setup>
/**
 * [B] 사장 출금  ·  /owner/wallet/withdraw  ·  OWNER
 * 출금 대상(은행·계좌)·금액 지정. 가용 잔액 내에서만(초과 시 서버 400).
 * 연계 API: POST /wallet/withdraw  →  @/services/wallet (withdrawWallet)
 * 공통: BankSelect(BANKS) · AppField(계좌) · WalletAmountField · isPositiveAmount
 */
import { storeToRefs } from 'pinia'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BankSelect from '@/components/wallet/BankSelect.vue'
import WalletAmountField from '@/components/wallet/WalletAmountField.vue'
import { withdrawWallet } from '@/services/wallet'
import { useUiStore } from '@/stores/ui'
import { useWalletStore } from '@/stores/wallet'
import { formatKRW } from '@/utils/format'
import { isPositiveAmount } from '@/utils/validators'

const router = useRouter()
const ui = useUiStore()
const walletStore = useWalletStore()
const { balance } = storeToRefs(walletStore)

const bankCode = ref('')
const accountNo = ref('')
const amount = ref('')
const accountError = ref('')
const amountError = ref('')
const submitting = ref(false)

onMounted(() => {
  // 전액 버튼·잔액 초과 가드에 필요하므로 항상 최신 잔액을 로드한다.
  walletStore.loadWallet()
})

/** 계좌번호: 숫자(하이픈 허용) 8자리 이상 */
const accountCheck = computed(() => {
  const digits = accountNo.value.replace(/-/g, '')
  if (!digits) return { valid: false, message: '계좌번호를 입력해주세요.' }
  if (!/^\d{8,}$/.test(digits)) return { valid: false, message: '계좌번호를 정확히 입력해주세요.' }
  return { valid: true, message: '' }
})

/** 금액: 양의 정수 + 가용 잔액 이내(서버가 최종 검증, 여기선 UX 가드) */
const amountCheck = computed(() => {
  const base = isPositiveAmount(amount.value)
  if (!base.valid) return base
  if (Number(amount.value) > balance.value) {
    return { valid: false, message: '가용 잔액을 초과했습니다.' }
  }
  return { valid: true, message: '' }
})

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
    await withdrawWallet({
      bankCode: bankCode.value,
      accountNo: accountNo.value.replace(/-/g, ''),
      amount: Number(amount.value)
    })
    await walletStore.loadWallet()
    ui.toast(`${formatKRW(Number(amount.value))} 출금 신청이 완료되었습니다.`, { type: 'success' })
    router.back()
  } catch {
    ui.toast('출금에 실패했습니다. 잔액을 확인해주세요.', { type: 'danger' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="출금" />
    <main class="screen-body">
      <p class="balance-line">
        가용 잔액 <strong>{{ formatKRW(balance) }}</strong>
      </p>

      <BankSelect v-model="bankCode" label="입금 은행" />

      <AppField
        v-model="accountNo"
        label="계좌번호"
        placeholder="'-' 없이 숫자만 입력"
        :error="accountError"
      />

      <WalletAmountField
        v-model="amount"
        label="출금 금액"
        :error="amountError"
        :fill-amount="balance"
        :max="balance"
      />

      <BaseButton
        class="submit"
        variant="owner"
        size="lg"
        block
        :disabled="!canSubmit"
        @click="onSubmit"
      >
        {{ submitting ? '처리 중…' : '출금하기' }}
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
