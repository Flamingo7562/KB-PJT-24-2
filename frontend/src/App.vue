<script setup>
import { RouterView } from 'vue-router'

import AppHeader from '@/components/AppHeader.vue'
</script>

<template>
  <div class="app-shell">
    <AppHeader />

    <main class="container py-5">
      <RouterView />
    </main>

    <footer class="container mt-auto border-top py-4 text-secondary">
      <small>Gig Hub · 근로생활 디지털 월렛 프로토타입</small>
    </footer>
  </div>
</template>
