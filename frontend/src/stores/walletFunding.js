import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 충전 입력 화면과 확인 화면 사이에서만 쓰는 메모리 전용 초안.
 * 계좌번호가 URL·브라우저 저장소에 남지 않도록 persistence를 사용하지 않는다.
 */
export const useWalletFundingStore = defineStore('walletFunding', () => {
  const draft = ref(null)

  function setDraft({ bankCode, accountNo, amount }) {
    draft.value = {
      bankCode: String(bankCode),
      accountNo: String(accountNo),
      amount: Number(amount)
    }
  }

  function clearDraft() {
    draft.value = null
  }

  return { draft, setDraft, clearDraft }
})
