import { defineStore } from 'pinia'
import { ref } from 'vue'

import { getWorkerHome } from '@/services/worker'

/**
 * 알바생 홈 화면 상태.
 * 서버 데이터의 최종 원본은 백엔드이며, 여기서는 화면 표시용 상태만 보관한다.
 */
export const useWorkerHomeStore = defineStore('workerHome', () => {
  const availableBalance = ref(0) // 안심지갑 대표 잔액·출금 가능액
  const todayWorkCase = ref(null) // 오늘의 알바 일정
  const earning = ref(null) // 확보 안심금액(서버 계산)
  const loading = ref(false)
  const error = ref(null)

  async function loadHome() {
    loading.value = true
    error.value = null
    try {
      const data = await getWorkerHome()
      availableBalance.value = data.wallet.availableBalance
      todayWorkCase.value = data.todayWorkCase
      earning.value = data.earning
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
  }

  return { availableBalance, todayWorkCase, earning, loading, error, loadHome }
})
