import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { fetchTransactions, fetchWallet } from '@/services/wallet'

const DEFAULT_QUERY = Object.freeze({ sort: 'LATEST', page: 0, size: 20 })
const emptyPage = () => ({ number: 0, size: 20, totalElements: 0, totalPages: 0 })

/** 지갑 요약과 승인 거래 Page를 함께 보존하는 공용 Store. */
export const useWalletStore = defineStore('wallet', () => {
  const currency = ref('KRW')
  const availableBalance = ref(0)
  const lockedBalance = ref(0)
  const transactions = ref([])
  const transactionPage = ref(emptyPage())
  const transactionQuery = ref({ ...DEFAULT_QUERY })
  const loading = ref(false)
  const transactionsLoading = ref(false)
  const error = ref(null)

  const hasNextTransactionPage = computed(
    () => transactionPage.value.number + 1 < transactionPage.value.totalPages
  )

  async function loadWallet() {
    const data = await fetchWallet()
    currency.value = data.currency
    availableBalance.value = data.availableBalance
    lockedBalance.value = data.lockedBalance
  }

  async function loadTransactions(params = {}, { append = false } = {}) {
    if (transactionsLoading.value) return

    const nextQuery = append
      ? { ...transactionQuery.value, ...params }
      : { ...DEFAULT_QUERY, ...params, page: params.page ?? 0 }

    transactionsLoading.value = true
    try {
      const data = await fetchTransactions(nextQuery)
      // 다음 Page만 이어 붙이고, 필터나 정렬이 바뀐 첫 Page는 기존 목록을 교체한다.
      transactions.value = append ? [...transactions.value, ...data.content] : [...data.content]
      transactionPage.value = { ...data.page }
      transactionQuery.value = { ...nextQuery }
    } finally {
      transactionsLoading.value = false
    }
  }

  async function loadNextTransactions() {
    if (!hasNextTransactionPage.value || transactionsLoading.value) return
    await loadTransactions(
      { ...transactionQuery.value, page: transactionPage.value.number + 1 },
      { append: true }
    )
  }

  /** 홈 진입 때 전체 지갑 요약과 거래 첫 Page를 함께 로드한다. */
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
    currency,
    availableBalance,
    lockedBalance,
    transactions,
    transactionPage,
    transactionQuery,
    hasNextTransactionPage,
    loading,
    transactionsLoading,
    error,
    loadWallet,
    loadTransactions,
    loadNextTransactions,
    loadHome
  }
})
