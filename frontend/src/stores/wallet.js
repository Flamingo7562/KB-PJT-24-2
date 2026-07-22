import { defineStore } from 'pinia'
import { ref } from 'vue'

import { fetchTransactions, fetchWallet } from '@/services/wallet'

/**
 * 사장 지갑 화면 상태.
 * 서버 데이터의 최종 원본은 백엔드이며, 여기서는 화면 표시용 상태만 보관한다.
 */
export const useWalletStore = defineStore('wallet', () => {
  const balance = ref(0) // 가용 잔액
  const heldAmount = ref(0) // 예치중 합계
  const transactions = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function loadWallet() {
    const data = await fetchWallet()
    balance.value = data.balance
    heldAmount.value = data.heldAmount
  }

  async function loadTransactions(params = {}) {
    const data = await fetchTransactions(params)
    transactions.value = data.content
  }

  /** 홈 진입 시 지갑·거래내역을 함께 로드 */
  async function loadHome(params = {}) {
    loading.value = true
    error.value = null
    try {
      await Promise.all([loadWallet(), loadTransactions(params)])
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
  }

  return {
    balance,
    heldAmount,
    transactions,
    loading,
    error,
    loadWallet,
    loadTransactions,
    loadHome
  }
})
