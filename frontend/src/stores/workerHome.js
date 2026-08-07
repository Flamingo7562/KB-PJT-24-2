import { defineStore } from 'pinia'
import { ref } from 'vue'

import { getWorkerHome } from '@/services/worker'

/**
 * 알바생 홈 화면 상태.
 * 서버 데이터의 최종 원본은 백엔드이며, 여기서는 화면 표시용 상태만 보관한다.
 *
 * 안심지갑 잔액은 이 Store가 아니라 OWNER와 공유하는 @/stores/wallet이 단일 원천이다
 * (#151). GET /worker/home 응답의 wallet 필드는 화면에서 읽지 않는다.
 */
export const useWorkerHomeStore = defineStore('workerHome', () => {
  const todayWorkCase = ref(null) // 오늘의 알바 일정
  const earning = ref(null) // 확보 안심금액(서버 계산)
  const loading = ref(false)
  const error = ref(null)

  async function loadHome() {
    loading.value = true
    error.value = null
    try {
      const data = await getWorkerHome()
      todayWorkCase.value = data.todayWorkCase
      earning.value = data.earning
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
  }

  return { todayWorkCase, earning, loading, error, loadHome }
})
