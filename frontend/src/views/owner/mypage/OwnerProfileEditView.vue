<script setup>
/**
 * [E] 사장 회원정보 변경  ·  /owner/mypage/profile  ·  OWNER
 * 이름·전화번호 수정(아이디·이메일 변경 불가).
 * 연계 API: GET /users/me · PATCH /users/me  →  @/services/users (getMe, updateMe)
 * 공통: AppField · BaseButton
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { getMe, updateMe } from '@/services/users'
import { useUiStore } from '@/stores/ui'
import { isPhone, isRequired } from '@/utils/validators'

const router = useRouter()
const ui = useUiStore()

const loginId = ref('')
const email = ref('')
const name = ref('')
const phone = ref('')

const nameError = ref('')
const phoneError = ref('')
const submitting = ref(false)

onMounted(async () => {
  const me = await getMe()
  loginId.value = me.loginId
  email.value = me.email
  name.value = me.name
  phone.value = me.phone
})

function validate() {
  const nameCheck = isRequired(name.value, '이름')
  const phoneCheck = isPhone(phone.value, { required: true })
  nameError.value = nameCheck.valid ? '' : nameCheck.message
  phoneError.value = phoneCheck.valid ? '' : phoneCheck.message
  return nameCheck.valid && phoneCheck.valid
}

async function handleSubmit() {
  if (!validate()) return

  submitting.value = true
  try {
    await updateMe({ name: name.value, phone: phone.value })
    ui.toast('회원정보가 변경됐어요.', { type: 'success' })
    router.back()
  } catch (err) {
    ui.toast(err?.response?.data?.message || '회원정보 변경에 실패했어요.', { type: 'danger' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="회원정보 변경" />
    <main class="screen-body">
      <form class="edit-form" @submit.prevent="handleSubmit">
        <AppField label="아이디" :model-value="loginId" disabled />
        <AppField label="이메일" :model-value="email" disabled />
        <AppField
          v-model="name"
          label="이름"
          placeholder="이름을 입력하세요"
          required
          :error="nameError"
        />
        <AppField
          v-model="phone"
          label="전화번호"
          type="tel"
          placeholder="010-0000-0000"
          required
          :error="phoneError"
        />

        <BaseButton type="submit" variant="owner" block :disabled="submitting">저장</BaseButton>
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
