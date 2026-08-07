<script setup>
/**
 * [B] 사장 충전  ·  /owner/wallet/charge  ·  OWNER
 * 은행·계좌번호·금액을 검증하고 메모리 전용 초안으로 확인 화면에 전달한다. 사장 전용.
 * 실제 POST /wallet/funding-orders는 PIN 확인 화면에서 한 번만 호출한다.
 * 공통: BankSelect(은행) · AppField(계좌) · WalletAmountField · 승인 계좌/금액 검증
 */
import { computed, onBeforeMount, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BankSelect from '@/components/wallet/BankSelect.vue'
import WalletAmountField from '@/components/wallet/WalletAmountField.vue'
import { useUiStore } from '@/stores/ui'
import { useWalletFundingStore } from '@/stores/walletFunding'
import { bankAccountRule, isWalletAmount, normalizeBankAccountNo } from '@/utils/validators'

const router = useRouter()
const ui = useUiStore()
const fundingStore = useWalletFundingStore()

const bankCode = ref('')
const accountNo = ref('')
const amount = ref('')
const accountError = ref('')
const amountError = ref('')

const accountCheck = computed(() => bankAccountRule(accountNo.value))

const amountCheck = computed(() => isWalletAmount(amount.value))
const accountFieldError = computed(() =>
  accountNo.value && !accountCheck.value.valid ? accountCheck.value.message : accountError.value
)
const amountFieldError = computed(() =>
  amount.value && !amountCheck.value.valid ? amountCheck.value.message : amountError.value
)
const canSubmit = computed(
  () => !!bankCode.value && accountCheck.value.valid && amountCheck.value.valid
)

onBeforeMount(() => {
  // 새 충전 입력은 이전 화면 이탈 때 남았을 수 있는 민감 초안을 이어받지 않는다.
  fundingStore.clearDraft()
})

function onSubmit() {
  if (!bankCode.value) {
    ui.toast('은행을 선택해주세요.', { type: 'warning' })
    return
  }
  accountError.value = accountCheck.value.valid ? '' : accountCheck.value.message
  amountError.value = amountCheck.value.valid ? '' : amountCheck.value.message
  if (accountError.value || amountError.value) return

  fundingStore.setDraft({
    bankCode: bankCode.value,
    accountNo: normalizeBankAccountNo(accountNo.value),
    amount: Number(amount.value)
  })
  router.push({ name: 'owner-charge-confirm' })
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
        충전하기
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
