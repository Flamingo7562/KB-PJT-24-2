<script setup>
/**
 * [H] 알바생 회원정보 변경  ·  /worker/mypage/profile  ·  WORKER
 * 전화번호만 수정(아이디·이메일·이름 변경 불가).
 * 연계 API: GET /users/me · PATCH /users/me  →  @/services/users (getMe, updateMe)
 * 공통: AppField · BaseButton · formatPhoneInput/blockNonDigitKeydown(전화번호 양식)
 *
 * 실시간 검증(#238): 필드가 전화번호 하나뿐이라 useFieldValidation 의 errors 슬롯 하나로 충분하다.
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { useFieldValidation } from '@/composables/useFieldValidation'
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

// 형식 오류 전용. 규칙은 기존 validate() 가 쓰던 것을 그대로 옮겼다 — 규칙 자체는 불변.
const { errors, handleBlur, validateAll } = useFieldValidation(() => ({ phone: phone.value }), {
  phone: (v) => isPhone(v.phone, { required: true })
})

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

async function handleSubmit() {
  if (!validateAll()) return

  submitting.value = true
  try {
    // 승인 계약상 PATCH Body 는 phone 만 허용한다. name 을 함께 보내면 400 으로 거부된다.
    await updateMe({ phone: phone.value })
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
          :error="errors.phone"
          @keydown="blockNonDigitKeydown"
          @update:model-value="onPhoneInput"
          @blur="handleBlur('phone')"
        />

        <BaseButton type="submit" variant="worker" block :disabled="submitting">저장</BaseButton>
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
