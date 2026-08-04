<script setup>
/**
 * 사장/알바생 공용 회원가입 폼.
 * 필드: 아이디(중복확인)·비밀번호·비밀번호 확인·이름·이메일(중복확인)·전화번호(선택).
 *
 * 연계 API: GET /auth/check-login-id · GET /auth/check-email · POST /auth/signup
 *   → @/services/auth (checkLoginId, checkEmail, signup)
 * 성공 후: role 에 맞는 로그인 화면으로 이동.
 */
import { Check } from 'lucide-vue-next'
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { checkEmail, checkLoginId, signup } from '@/services/auth'
import { useUiStore } from '@/stores/ui'
import { formatPhoneInput } from '@/utils/format'
import {
  isEmail,
  isPhone,
  loginIdRule,
  NAME_MAX_LENGTH,
  nameRule,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  passwordRule,
  passwordsMatch
} from '@/utils/validators'

const props = defineProps({
  role: { type: String, required: true } // 'OWNER' | 'WORKER'
})

const router = useRouter()
const ui = useUiStore()

const form = reactive({
  loginId: '',
  password: '',
  passwordConfirm: '',
  name: '',
  email: '',
  phone: ''
})

const errors = reactive({
  loginId: '',
  password: '',
  passwordConfirm: '',
  name: '',
  email: '',
  phone: ''
})

const loginIdCheck = reactive({ done: false, available: false })
const emailCheck = reactive({ done: false, available: false })
const submitting = ref(false)

// 중복확인을 통과한 상태 — 버튼 자리를 '확인완료' 표시로 바꾼다.
// 값이 바뀌면 아래 watch 가 done 을 내려 자동으로 버튼이 돌아온다.
const loginIdVerified = computed(() => loginIdCheck.done && loginIdCheck.available)
const emailVerified = computed(() => emailCheck.done && emailCheck.available)

// 값이 바뀌면 이전 중복확인 결과는 무효화한다.
watch(
  () => form.loginId,
  () => {
    loginIdCheck.done = false
    loginIdCheck.available = false
  }
)
watch(
  () => form.email,
  () => {
    emailCheck.done = false
    emailCheck.available = false
  }
)

// 입력 중 하이픈을 자동으로 채운다(010-1234-5678). 숫자 외 입력은 AppField 의 digits-only 가 막는다.
// v-model 은 입력값을 그대로 대입해 가공할 틈이 없다. 그래서 템플릿에서 축약을 풀어
// :model-value + @update:model-value 로 쓰고, 대입 자리에 이 함수를 끼워 포맷을 거치게 한다.
function onPhoneInput(v) {
  form.phone = formatPhoneInput(v)
}

async function onCheckLoginId() {
  const rule = loginIdRule(form.loginId)
  errors.loginId = rule.message
  if (!rule.valid) return

  const { available } = await checkLoginId(form.loginId)
  loginIdCheck.done = true
  loginIdCheck.available = available
  errors.loginId = available ? '' : '이미 사용 중인 아이디입니다.'
  if (available) ui.toast('사용 가능한 아이디입니다.', { type: 'success' })
}

async function onCheckEmail() {
  const rule = isEmail(form.email)
  errors.email = rule.message
  if (!rule.valid) return

  const { available } = await checkEmail(form.email)
  emailCheck.done = true
  emailCheck.available = available
  errors.email = available ? '' : '이미 사용 중인 이메일입니다.'
  if (available) ui.toast('사용 가능한 이메일입니다.', { type: 'success' })
}

/** 전 필드 검증. 하나라도 실패하면 false. */
function validate() {
  const nameCheck = nameRule(form.name)
  const pwRule = passwordRule(form.password)
  const pwConfirmRule = passwordsMatch(form.password, form.passwordConfirm)
  const phoneRule = isPhone(form.phone)

  errors.name = nameCheck.message
  errors.password = pwRule.message
  errors.passwordConfirm = pwConfirmRule.message
  errors.phone = phoneRule.message

  const idRule = loginIdRule(form.loginId)
  errors.loginId = !idRule.valid
    ? idRule.message
    : !loginIdCheck.done
      ? '아이디 중복확인을 해주세요.'
      : !loginIdCheck.available
        ? '이미 사용 중인 아이디입니다.'
        : ''

  const emailRule = isEmail(form.email)
  errors.email = !emailRule.valid
    ? emailRule.message
    : !emailCheck.done
      ? '이메일 중복확인을 해주세요.'
      : !emailCheck.available
        ? '이미 사용 중인 이메일입니다.'
        : ''

  return Object.values(errors).every((message) => !message)
}

async function onSubmit() {
  if (!validate()) {
    ui.toast('입력 내용을 확인해주세요.', { type: 'warning' })
    return
  }

  submitting.value = true
  try {
    // 화면은 하이픈이 들어간 표시 형식을 유지하고, 전송 값 정규화는 서비스 계층이 담당한다.
    await signup({
      loginId: form.loginId,
      password: form.password,
      passwordConfirm: form.passwordConfirm,
      name: form.name,
      email: form.email,
      phone: form.phone,
      role: props.role
    })
    ui.toast('회원가입이 완료되었습니다. 로그인해주세요.', { type: 'success' })
    router.push(props.role === 'OWNER' ? '/owner/login' : '/worker/login')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="signup-form" @submit.prevent="onSubmit">
    <AppField
      v-model="form.loginId"
      label="아이디"
      placeholder="4~20자 영문·숫자"
      required
      :error="errors.loginId"
    >
      <template #suffix>
        <span v-if="loginIdVerified" class="verified"><Check :size="16" /> 확인완료</span>
        <BaseButton v-else type="button" variant="secondary" @click="onCheckLoginId">
          중복확인
        </BaseButton>
      </template>
    </AppField>

    <AppField
      v-model="form.password"
      type="password"
      label="비밀번호"
      :placeholder="`${PASSWORD_MIN_LENGTH}~${PASSWORD_MAX_LENGTH}자`"
      :maxlength="PASSWORD_MAX_LENGTH"
      required
      :error="errors.password"
    />

    <AppField
      v-model="form.passwordConfirm"
      type="password"
      label="비밀번호 확인"
      placeholder="비밀번호를 한 번 더 입력"
      required
      :error="errors.passwordConfirm"
    />

    <AppField
      v-model="form.name"
      label="이름"
      placeholder="이름"
      :maxlength="NAME_MAX_LENGTH"
      required
      :error="errors.name"
    />

    <AppField
      v-model="form.email"
      type="email"
      label="이메일"
      placeholder="example@gighub.com"
      required
      :error="errors.email"
    >
      <template #suffix>
        <span v-if="emailVerified" class="verified"><Check :size="16" /> 확인완료</span>
        <BaseButton v-else type="button" variant="secondary" @click="onCheckEmail">
          중복확인
        </BaseButton>
      </template>
    </AppField>

    <AppField
      :model-value="form.phone"
      type="tel"
      label="전화번호"
      placeholder="010-0000-0000 (선택)"
      digits-only
      maxlength="13"
      :error="errors.phone"
      @update:model-value="onPhoneInput"
    />

    <BaseButton
      type="submit"
      :variant="role === 'OWNER' ? 'owner' : 'worker'"
      size="lg"
      block
      :disabled="submitting"
    >
      회원가입
    </BaseButton>
  </form>
</template>

<style scoped>
.signup-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
/* 중복확인 완료 표시 — 버튼과 같은 높이를 유지해 입력란 우측 정렬이 흔들리지 않게 한다. */
.verified {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 var(--space-md);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-success);
  background: var(--color-success-bg);
  white-space: nowrap;
}
</style>
