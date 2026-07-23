<script setup>
/**
 * [E] 사장 비밀번호 변경  ·  /owner/mypage/password  ·  OWNER
 * 현재 비밀번호 확인 후 새 비밀번호 설정(불일치 시 400).
 * 연계 API: PATCH /users/me/password  →  @/services/users (changePassword)
 * 공통: AppField · BaseButton · @/utils/validators (passwordRule, passwordsMatch)
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { changePassword } from '@/services/users'
import { useUiStore } from '@/stores/ui'
import { isRequired, passwordRule, passwordsMatch } from '@/utils/validators'

const router = useRouter()
const ui = useUiStore()

const currentPassword = ref('')
const newPassword = ref('')
const newPasswordConfirm = ref('')

const currentPasswordError = ref('')
const newPasswordError = ref('')
const confirmError = ref('')
const submitting = ref(false)

function validate() {
  const currentCheck = isRequired(currentPassword.value, '현재 비밀번호')
  const newCheck = passwordRule(newPassword.value)
  const confirmCheck = passwordsMatch(newPassword.value, newPasswordConfirm.value)

  currentPasswordError.value = currentCheck.valid ? '' : currentCheck.message
  newPasswordError.value = newCheck.valid ? '' : newCheck.message
  confirmError.value = confirmCheck.valid ? '' : confirmCheck.message

  return currentCheck.valid && newCheck.valid && confirmCheck.valid
}

async function handleSubmit() {
  if (!validate()) return

  submitting.value = true
  currentPasswordError.value = ''
  try {
    await changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value
    })
    ui.toast('비밀번호가 변경됐어요.', { type: 'success' })
    router.back()
  } catch (err) {
    currentPasswordError.value = err?.response?.data?.message || '현재 비밀번호가 일치하지 않아요.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="비밀번호 변경" />
    <main class="screen-body">
      <form class="edit-form" @submit.prevent="handleSubmit">
        <AppField
          v-model="currentPassword"
          label="현재 비밀번호"
          type="password"
          placeholder="현재 비밀번호를 입력하세요"
          required
          :error="currentPasswordError"
        />
        <AppField
          v-model="newPassword"
          label="새 비밀번호"
          type="password"
          placeholder="영문+숫자 포함 8자 이상"
          required
          :error="newPasswordError"
        />
        <AppField
          v-model="newPasswordConfirm"
          label="새 비밀번호 확인"
          type="password"
          placeholder="새 비밀번호를 다시 입력하세요"
          required
          :error="confirmError"
        />

        <BaseButton type="submit" variant="owner" block :disabled="submitting">변경</BaseButton>
      </form>
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
.edit-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
</style>
