import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const projectName = ref('Gig Hub')
  const backendConnected = ref(false)

  function setBackendConnected(connected) {
    backendConnected.value = connected
  }

  return {
    projectName,
    backendConnected,
    setBackendConnected
  }
})
