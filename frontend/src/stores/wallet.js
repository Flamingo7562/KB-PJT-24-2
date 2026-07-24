import { defineStore } from 'pinia'
import { ref } from 'vue'

import { fetchTransactions, fetchWallet } from '@/services/wallet'

/**
 * 사장 지갑 화면 상태.
 * 서버 데이터의 최종 원본은 백엔드이며, 여기서는 화면 표시용 상태만 보관한다.
 */
export const useWalletStore = defineStore('wallet', () => {
  const availableBalance = ref(0) // 대표 잔액·출금 가능액 (예치금 미포함)
  const lockedBalance = ref(0) // 예치중 금액
  const transactions = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function loadWallet() {
    const data = await fetchWallet()
    availableBalance.value = data.availableBalance
    lockedBalance.value = data.lockedBalance
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
    availableBalance,
    lockedBalance,
    transactions,
    loading,
    error,
    loadWallet,
    loadTransactions,
    loadHome
  }
})
