<script setup>
/**
 * [E] 사장 회원정보 변경  ·  /owner/mypage/profile  ·  OWNER
 * 전화번호만 수정(아이디·이메일·이름 변경 불가).
 * 연계 API: GET /users/me · PATCH /users/me  →  @/services/users (getMe, updateMe)
 * 공통: AppField · BaseButton · formatPhoneInput/blockNonDigitKeydown(전화번호 양식)
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { getMe, updateMe } from '@/services/users'
import { useUiStore } from '@/stores/ui'
import { blockNonDigitKeydown, formatPhoneInput } from '@/utils/format'
import { isPhone } from '@/utils/validators'

const router = useRouter()
const ui = useUiStore()

const loginId = ref('')
const email = ref('')
const name = ref('')
const phone = ref('')

const phoneError = ref('')
const submitting = ref(false)

onMounted(async () => {
  const me = await getMe()
  loginId.value = me.loginId
  email.value = me.email
  name.value = me.name
  phone.value = me.phone ? formatPhoneInput(me.phone) : ''
})

function onPhoneInput(v) {
  phone.value = formatPhoneInput(v)
}

function validate() {
  const phoneCheck = isPhone(phone.value, { required: true })
  phoneError.value = phoneCheck.valid ? '' : phoneCheck.message
  return phoneCheck.valid
}

async function handleSubmit() {
  if (!validate()) return

  submitting.value = true
  try {
    // 이름은 변경 불가지만 서버가 name 을 null 로 덮지 않도록 로드한 값을 그대로 보낸다.
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
        <AppField label="이름" :model-value="name" disabled />
        <AppField
          :model-value="phone"
          label="전화번호"
          type="tel"
          placeholder="010-0000-0000"
          maxlength="13"
          required
          :error="phoneError"
          @keydown="blockNonDigitKeydown"
          @update:model-value="onPhoneInput"
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
