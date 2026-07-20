<script setup>
import { storeToRefs } from 'pinia'
import { RouterLink } from 'vue-router'

import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const { projectName } = storeToRefs(appStore)
</script>

<template>
  <header class="border-bottom bg-white">
    <nav class="container navbar navbar-expand py-3" aria-label="주요 메뉴">
      <RouterLink class="navbar-brand d-flex align-items-center gap-2 fw-bold" to="/">
        <span class="brand-mark" aria-hidden="true">G</span>
        {{ projectName }}
      </RouterLink>

      <ul class="navbar-nav ms-auto">
        <li class="nav-item">
          <RouterLink class="nav-link" to="/">홈</RouterLink>
        </li>
      </ul>
    </nav>
  </header>
</template>
