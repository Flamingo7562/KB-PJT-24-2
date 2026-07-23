<script setup>
/**
 * [A] 알바생 로그인  ·  /worker/login  ·  PUBLIC(게스트 전용)
 * 아이디·비밀번호 로그인. redirect 쿼리 지원(초대 링크 복귀).
 * 연계 API: POST /auth/login  →  useAuthStore().login({ loginId, password, role: 'WORKER' })
 * 성공 후: route.query.redirect ?? '/worker/home'.
 * 공통: AppField · BaseButton · useUiStore().toast
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AuthRoleToggle from '@/components/auth/AuthRoleToggle.vue'
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import LogoGighub from '@/assets/images/logo/logo-gighub.svg'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { resolveWorkerLoginRedirect } from '@/utils/authRedirect'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const ui = useUiStore()

const loginId = ref('')
const password = ref('')
const submitting = ref(false)

const canSubmit = computed(() => loginId.value.trim() !== '' && password.value !== '')

function onChangeRole(next) {
  if (next === 'OWNER') router.push('/owner/login')
}

async function onSubmit() {
  if (!canSubmit.value) {
    ui.toast('아이디와 비밀번호를 입력해주세요.', { type: 'warning' })
    return
  }

  submitting.value = true
  try {
    await auth.login({
      loginId: loginId.value,
      password: password.value,
      role: 'WORKER'
    })
    router.push(resolveWorkerLoginRedirect(route.query.redirect))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="알바생 로그인" to="/?step=auth&role=worker" />
    <main class="screen-body">
      <LogoGighub class="logo" aria-label="Gig Hub" />

      <AuthRoleToggle model-value="WORKER" @update:model-value="onChangeRole" />

      <form class="form" @submit.prevent="onSubmit">
        <AppField v-model="loginId" label="아이디" placeholder="아이디" required />
        <AppField
          v-model="password"
          type="password"
          label="비밀번호"
          placeholder="비밀번호"
          required
        />
        <BaseButton type="submit" variant="worker" size="lg" block :disabled="submitting">
          로그인
        </BaseButton>
      </form>

      <p class="signup-link">
        아직 계정이 없나요?
        <RouterLink to="/worker/signup">회원가입</RouterLink>
      </p>
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}
.logo {
  width: 140px;
  height: auto;
  margin: var(--space-md) auto 0;
  color: var(--color-worker);
}
.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.signup-link {
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.signup-link a {
  color: var(--color-worker);
  font-weight: var(--weight-medium);
}
</style>
